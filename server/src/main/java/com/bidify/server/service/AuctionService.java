package com.bidify.server.service;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.DeleteAuctionRequest;
import com.bidify.common.model.Event;
import com.bidify.common.model.GetAuctionDetailRequest;
import com.bidify.common.model.JoinAuctionRequest;
import com.bidify.common.model.LeaveAuctionRequest;
import com.bidify.common.model.PlaceBidRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdateAuctionRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.User;
import com.bidify.server.model.runtime.AuctionChannel;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.AuctionMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// service xử lý các logic liên quan đến auction, tương tác với database thông qua AuctionDao và cập nhật realtime database để đồng bộ với client
public class AuctionService {
    private final AuctionDao auctionDao = new AuctionDao();

    // load runtime auctions trong sql lên ram, chỉ gọi 1 lần khi server khởi chạy
    public void loadToRuntime(){
        List<Auction> runtimeAuctions = new ArrayList<>();
        runtimeAuctions.addAll(auctionDao.findByStatus(AuctionStatus.UPCOMING));
        runtimeAuctions.addAll(auctionDao.findByStatus(AuctionStatus.ACTIVE));

        for (Auction auction : runtimeAuctions)
            RealtimeDatabase.addRuntimeAuction(auction);
    }

    public Response create(ClientHandler client, Request request){
        CreateAuctionRequest data = JsonUtil.fromMap(request.getData(), CreateAuctionRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");

        return handleAuctionRequest(() -> {
            requireSession(client);

            String sellerUsername = data.getSeller();
            String auctionName = data.getAuctionName();
            String description = data.getDescription();
            String category = data.getCategory();
            String productType = data.getProductType();
            double startingPrice = data.getStartingPrice();
            double minIncrement = data.getMinIncrement();
            LocalDateTime startTime = parseDateTime(data.getStartTime());
            LocalDateTime endTime = parseDateTime(data.getEndTime());

            validateAuctionFields(sellerUsername, auctionName, description, category, productType, startingPrice, minIncrement);

            if (!client.getCurrentUsername().equals(sellerUsername))
                return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");
            validateAuctionTime(startTime, endTime);

            Auction auction = new Auction(auctionName, description, sellerUsername, startingPrice, startTime, endTime);
            auction.setCategory(category);
            auction.setProductType(productType);
            auction.setMinIncrement(minIncrement);
            auctionDao.create(auction);
            RealtimeDatabase.addRuntimeAuction(auction);

            AuctionDto auctionDto = AuctionMapper.toDto(auction);
            RealtimeDatabase.getGlobalChannel().publish(new Event(EventType.AUCTION_CREATED, "New auction created", auctionDto));

            return new Response(RequestStatus.SUCCESS, "Create new auction successfully!");
        });
    }

    public Response update(ClientHandler client, Request request){
        UpdateAuctionRequest data = JsonUtil.fromMap(request.getData(), UpdateAuctionRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");

        return handleAuctionRequest(() -> {
            requireSession(client);

            String auctionId = data.getAuctionId();
            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");

            Auction auction = RealtimeDatabase.getUpcomingAuction(auctionId);
            if (auction == null) auction = auctionDao.findById(auctionId);
            if (auction == null) return new Response(RequestStatus.NOT_FOUND, "Auction not found");

            requireSeller(auction, client.getCurrentUsername(), "You don't have permission to update this auction");
            if (auction.getStatus() != AuctionStatus.UPCOMING)
                return new Response(RequestStatus.FAILED, "Can only update auction before it starts");

            String auctionName = data.getAuctionName();
            String description = data.getDescription();
            double startingPrice = data.getStartingPrice();
            LocalDateTime startTime = parseDateTime(data.getStartTime());
            LocalDateTime endTime = parseDateTime(data.getEndTime());

            validateAuctionFields(auction.getSellerUsername(), auctionName, description, auction.getCategory(), auction.getProductType(), startingPrice, auction.getMinIncrement());
            validateAuctionTime(startTime, endTime);

            auction.setAuctionName(auctionName);
            auction.setDescription(description);
            auction.setStartingPrice(startingPrice);
            auction.setStartTime(startTime);
            auction.setEndTime(endTime);

            auctionDao.save(auction);

            AuctionDto auctionDto = AuctionMapper.toDto(auction);
            RealtimeDatabase.getGlobalChannel().publish(new Event(EventType.AUCTION_UPDATED, "Auction updated", auctionDto));

            return new Response(RequestStatus.SUCCESS, "Auction updated successfully!", auctionDto);
        });
    }

    public void saveAllRuntimeAuctions(){
        for (Auction auction : RealtimeDatabase.getAllRuntimeAuctions())
            auctionDao.save(auction);
    }

    public Response delete(ClientHandler client, Request request){
        DeleteAuctionRequest data = JsonUtil.fromMap(request.getData(), DeleteAuctionRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");

        return handleAuctionRequest(() -> {
            requireSession(client);

            String auctionId = data.getId();
            ValidationUtil.requiresNonBlank(auctionId, "Auction id");

            Auction auction = RealtimeDatabase.getUpcomingAuction(auctionId);
            if (auction == null) auction = auctionDao.findById(auctionId);
            if (auction == null)
                return new Response(RequestStatus.NOT_FOUND, "Auction not found");

            if (auction.getStatus() != AuctionStatus.UPCOMING)
                return new Response(RequestStatus.FAILED, "Cannot delete auction after it has started");

            requireSeller(auction, client.getCurrentUsername(), "Only seller can delete their auction");

            auctionDao.deleteById(auctionId);
            RealtimeDatabase.removeRuntimeAuction(auctionId);

            RealtimeDatabase.getGlobalChannel().publish(new Event(EventType.AUCTION_DELETED, "Auction deleted", auctionId));

            return new Response(RequestStatus.SUCCESS, "Auction deleted successfully");
        });
    }

    public Response getDetail(Request request){
        GetAuctionDetailRequest data = JsonUtil.fromMap(request.getData(), GetAuctionDetailRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");

        return handleAuctionRequest(() -> {
            String auctionId = data.getAuctionId();
            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");

            Auction auction = auctionDao.findById(auctionId);
            if (auction == null)
                return new Response(RequestStatus.NOT_FOUND, "Auction not found");

            AuctionDto auctionDto = AuctionMapper.toDto(auction);
            return new Response(RequestStatus.SUCCESS, "Get auction detail successfully", auctionDto);
        });
    }

    public Response getAllLiveAuctions(){
        List<Auction> auctions = RealtimeDatabase.getAllLiveAuctions();
        List<AuctionDto> summaries = new ArrayList<>();

        if (auctions == null || auctions.isEmpty())
            return new Response(RequestStatus.SUCCESS, "No live auctions", summaries);

        for (Auction auction : auctions)
            summaries.add(AuctionMapper.toDto(auction));

        return new Response(RequestStatus.SUCCESS, "Get live auctions successfully", summaries);
    }

    public Response join(ClientHandler client, Request request){
        JoinAuctionRequest data = JsonUtil.fromMap(request.getData(), JoinAuctionRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");

        return handleAuctionRequest(() -> {
            requireSession(client);

            String auctionId = data.getAuctionId();
            String username = client.getCurrentUsername();

            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");
            Auction auction = RealtimeDatabase.getRuntimeAuction(auctionId);
            if (auction == null) return new Response(RequestStatus.NOT_FOUND, "Auction not found");

            if (!isRuntimeAuction(auction))
                return new Response(RequestStatus.NOT_FOUND, "Auction not found");
            if (RealtimeDatabase.isWatchingAuction(username, auctionId))
                return new Response(RequestStatus.SUCCESS, "You are already watching this auction");

            RealtimeDatabase.subscribeAuctionChannel(auctionId, username);

            AuctionDto auctionDto = AuctionMapper.toDto(auction);
            return new Response(RequestStatus.SUCCESS, "Join auction successfully", auctionDto);
        });
    }

    public Response leave(ClientHandler client, Request request){
        LeaveAuctionRequest data = JsonUtil.fromMap(request.getData(), LeaveAuctionRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");
        if (!client.isInSession()) return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");

        String auctionId = data.getAuctionId();
        String username = client.getCurrentUsername();

        if (!RealtimeDatabase.isWatchingAuction(username, auctionId))
            return new Response(RequestStatus.FAILED, "You are not watching this auction");

        RealtimeDatabase.unsubscribeAuctionChannel(auctionId, username);
        return new Response(RequestStatus.SUCCESS, "Leave auction successfully");
    }

    public Response placeBid(ClientHandler client, Request request){
        PlaceBidRequest data = JsonUtil.fromMap(request.getData(), PlaceBidRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");
        if (!client.isInSession()) return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");

        return handleAuctionRequest(() -> {
            String auctionId = data.getAuctionId();
            double bidAmount = data.getBidAmount();
            String username = client.getCurrentUsername();

            ValidationUtil.requiresNonBlank(auctionId, "Invalid auction ID");
            ValidationUtil.validatePositiveAmount(bidAmount, "Bid amount must be positive");

            Auction auction = RealtimeDatabase.getLiveAuction(auctionId);
            User user = RealtimeDatabase.getActiveUser(username);

            if (auction == null)
                return new Response(RequestStatus.NOT_FOUND, "Auction not found or not active");
            if (user == null)
                return new Response(RequestStatus.FAILED, "User not found");
            if (auction.getSellerUsername().equals(username))
                return new Response(RequestStatus.FAILED, "You cannot bid on your own auction");
            if (user.getWallet() < bidAmount)
                return new Response(RequestStatus.FAILED, "Insufficient balance");

            Bid bid = new Bid(auction.getId(), username, bidAmount);
            if (!auction.placeBid(bid))
                return new Response(RequestStatus.FAILED, "Failed to place bid");

            auctionDao.save(auction);

            AuctionDto auctionDto = AuctionMapper.toDto(auction);
            AuctionChannel auctionChannel = RealtimeDatabase.getAuctionChannel(auctionId);
            if (auctionChannel != null)
                auctionChannel.publish(new Event(EventType.BID_PLACED, "New bid placed", auctionDto));

            return new Response(RequestStatus.SUCCESS, "Place bid successfully");
        });
    }

    private Response handleAuctionRequest(Supplier<Response> action) {
        try {
            return action.get();
        }
        catch (DateTimeParseException e) {
            return new Response(RequestStatus.FAILED, "Invalid date time format");
        }
        catch (ValidationException | DatabaseException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }
    }

    private void requireSession(ClientHandler client) {
        if (client == null || !client.isInSession())
            throw new ValidationException("Invalid session");
    }

    private void requireSeller(Auction auction, String username, String message) {
        if (auction == null || username == null || !username.equals(auction.getSellerUsername()))
            throw new ValidationException(message);
    }

    private void validateAuctionFields(String sellerUsername, String auctionName, String description,
                                       String category, String productType,
                                       double startingPrice, double minIncrement) {
        ValidationUtil.requiresNonBlank(sellerUsername, "Seller");
        ValidationUtil.requiresNonBlank(auctionName, "Auction's name");
        ValidationUtil.requiresNonBlank(description, "Description");
        ValidationUtil.requiresNonBlank(category, "Category");
        ValidationUtil.requiresNonBlank(productType, "Product type");
        ValidationUtil.validateMaxLength("Description", description, 2000);
        ValidationUtil.validatePositiveAmount(startingPrice, "Starting price");
        ValidationUtil.validatePositiveAmount(minIncrement, "Min increment");
    }

    private void validateAuctionTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null) 
            throw new ValidationException("Start date cannot be empty");
        if (endTime == null) 
            throw new ValidationException("End date cannot be empty");
        if (startTime.isAfter(endTime))
            throw new ValidationException("End time must be after start time");
        if (LocalDateTime.now().isAfter(startTime))
            throw new ValidationException("Start time must be in the future");
    }

    private LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value);
    }

    private boolean isRuntimeAuction(Auction auction) {
        if (auction == null) return false;
        AuctionStatus status = auction.getStatus();
        return status == AuctionStatus.UPCOMING || status == AuctionStatus.ACTIVE;
    }
}
