package com.bidify.server.service;

import com.bidify.server.dao.AuctionDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.DeleteAuctionRequest;
import com.bidify.common.model.Event;
import com.bidify.common.model.GetAuctionDetailRequest;
import com.bidify.common.model.JoinAuctionRequest;
import com.bidify.common.model.LeaveAuctionRequest;
import com.bidify.common.model.PlaceBidRequest;
import com.bidify.common.model.UpdateAuctionRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;

public class AuctionService {
    private final AuctionDao auctionDao = new AuctionDao();

    // load live auctions trong sql lên ram, chỉ gọi 1 lần khi server khởi chạy
    public void loadToRuntime(){
        List<Auction> liveAuctions = auctionDao.findByStatus(AuctionStatus.ACTIVE);
        for (Auction auction : liveAuctions) RealtimeDatabase.addLiveAuction(auction);
    }

    public Response create(ClientHandler client, Request request){ // tạo auction
        CreateAuctionRequest data = JsonUtil.fromMap(request.getData(), CreateAuctionRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");
        if (!client.isValidClient()) return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");

        String sellerUsername = data.getSeller();
        String auctionName = data.getAuctionName();
        String description = data.getDescription();
        String category = data.getCategory();
        String productType = data.getProductType();
        double startingPrice = data.getStartingPrice();
        double minIncrement = data.getMinIncrement();
        LocalDateTime startTime, endTime;
        try{
            startTime = LocalDateTime.parse(data.getStartTime());
            endTime = LocalDateTime.parse(data.getEndTime());
        }
        catch (DateTimeParseException e) {
            return new Response(RequestStatus.FAILED, "Invalid date time format");
        }

        try{
            ValidationUtil.requiresNonBlank(sellerUsername, "Seller");
            ValidationUtil.requiresNonBlank(auctionName, "Auction's name");
            ValidationUtil.requiresNonBlank(description, "Description");
            ValidationUtil.requiresNonBlank(category, "Category");
            ValidationUtil.requiresNonBlank(productType, "Product type");
            ValidationUtil.validateMaxLength("Description", description, 200);
            ValidationUtil.validatePositiveAmount(startingPrice, "Starting price");
            if (minIncrement < 0) throw new ValidationException("Min increment must be non-negative");
        }
        catch (ValidationException e){
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        if (!client.getCurrentUsername().equals(sellerUsername))
            return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");

        if (!endTime.isAfter(startTime)) return new Response(RequestStatus.FAILED, "End time must be after start time");

        try{
            Auction auction = new Auction(sellerUsername, auctionName, description, startingPrice, startTime, endTime);
            auction.setCategory(category);
            auction.setProductType(productType);
            auction.setMinIncrement(minIncrement);
            if (!auctionDao.create(auction)) throw new DatabaseException("Failed to create auction");
            RealtimeDatabase.addLiveAuction(auction);
        }
        catch (DatabaseException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        return new Response(RequestStatus.SUCCESS, "Create new auction successfully!");
    }

    public Response update(ClientHandler client, Request request){ // cập nhật thông tin của auction
        UpdateAuctionRequest data = JsonUtil.fromMap(request.getData(), UpdateAuctionRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");
        if (!client.isValidClient()) return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");

        String auctionId = data.getAuctionId();
        try {
            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");
        } catch (ValidationException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) return new Response(RequestStatus.NOT_FOUND, "Auction not found");

        if (!auction.getSellerUsername().equals(client.getCurrentUsername()))
            return new Response(RequestStatus.UNAUTHORIZED, "You don't have permission to update this auction");

        // chỉ cho phép update trước khi bắt đầu đấu giá
        if (auction.getStatus() != AuctionStatus.UPCOMING)
            return new Response(RequestStatus.FAILED, "Can only update auction before it starts");

        // validate và cập nhật các field
        String auctionName = data.getAuctionName();
        String description = data.getDescription();
        double startingPrice = data.getStartingPrice();
        LocalDateTime startTime, endTime;

        try {
            startTime = LocalDateTime.parse(data.getStartTime());
            endTime = LocalDateTime.parse(data.getEndTime());
        } catch (DateTimeParseException e) {
            return new Response(RequestStatus.FAILED, "Invalid date time format");
        }

        try {
            ValidationUtil.requiresNonBlank(auctionName, "Auction's name");
            ValidationUtil.requiresNonBlank(description, "Description");
            ValidationUtil.validateMaxLength("Description", description, 200);
            ValidationUtil.validatePositiveAmount(startingPrice, "Starting price");
        } catch (ValidationException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        if (!endTime.isAfter(startTime))
            return new Response(RequestStatus.FAILED, "End time must be after start time");

        auction.setAuctionName(auctionName);
        auction.setDescription(description);
        auction.setStartingPrice(startingPrice);
        auction.setStartTime(startTime);
        auction.setEndTime(endTime);

        try {
            if (!auctionDao.save(auction)) throw new DatabaseException("Failed to update auction");
        } catch (DatabaseException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        // gửi tin nhắn cho người tham gia nếu có message
        List<ClientHandler> watchers = RealtimeDatabase.getAuctionWatchers(auctionId);
        if (watchers != null){
            for (ClientHandler watcher : watchers)
                watcher.sendEvent(new Event(EventType.AUCTION_UPDATED, "Auction updated")); // TODO: gửi auctionDto
        }
        return new Response(RequestStatus.SUCCESS, "Auction updated successfully!"); // TODO: return auctionDto
    }
    
    public void saveAllLiveAuctions(){ // lưu tất cả auction data
        for (Auction auction : RealtimeDatabase.getAllLiveAuctions())
            auctionDao.save(auction);
    }

    public Response delete(ClientHandler client, Request request){ // xóa auction
        DeleteAuctionRequest data = JsonUtil.fromMap(request.getData(), DeleteAuctionRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid data request");
        if (!client.isValidClient()) return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");

        String auctionId = data.getId();
        try{
            ValidationUtil.requiresNonBlank(auctionId, "Auction id");
        } 
        catch( ValidationException e){
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) 
            return new Response(RequestStatus.NOT_FOUND, "Auction not found");

        if (auction.getStatus() != AuctionStatus.UPCOMING) return new Response(RequestStatus.FAILED, "Can not delete auction after it started");

        if (!auction.getSellerUsername().equals(client.getCurrentUsername())) 
            return new Response(RequestStatus.FAILED, "Only seller can delete their auction");

        if (!auctionDao.deleteById(auctionId)) 
            return new Response(RequestStatus.FAILED, "Failed to delete auction");

        RealtimeDatabase.removeLiveAuction(auctionId);
        return new Response(RequestStatus.SUCCESS, "Auction deleted successfully");
    }

    public Response getDetail(ClientHandler client, Request request){ // lấy chi tiết của auction
        GetAuctionDetailRequest data = JsonUtil.fromMap(request.getData(), GetAuctionDetailRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");

        String auctionId = data.getAuctionId();
        try {
            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");
        } catch (ValidationException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        Auction auction = auctionDao.findById(auctionId);

        if (auction == null) {
            return new Response(RequestStatus.NOT_FOUND, "Auction not found");
        }
        // TODO: return auctionDto
        return new Response(RequestStatus.SUCCESS, "Get auction detail successfully", auction);
    }

    public Response getAllLiveAuctions(ClientHandler client, Request request){ // lấy danh sách các auction đang diễn ra
        List<Auction> auctions = RealtimeDatabase.getAllLiveAuctions();
        if (auctions == null || auctions.size() == 0)
            return new Response(RequestStatus.SUCCESS, "No live auctions", auctions);

        List<AuctionDto> summaries = new ArrayList<AuctionDto>();
        for (Auction auction : auctions) {
            double displayBid = auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingPrice();
            summaries.add(new AuctionDto(
                auction.getId(),
                auction.getAuctionName(),
                auction.getDescription(),
                auction.getSellerUsername(),
                auction.getEndTime() == null ? "" : auction.getEndTime().toString(),
                auction.getStartingPrice(),
                displayBid,
                auction.getBidCount()
            ));
        }
        return new Response(RequestStatus.SUCCESS, "Get live auctions successfully", summaries);
    }

    public Response join(ClientHandler client, Request request){ // tham gia vào auction
        JoinAuctionRequest data = JsonUtil.fromMap(request.getData(), JoinAuctionRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");
        if (!client.isValidClient()) return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");

        String auctionId = data.getAuctionId();
        String username = client.getCurrentUsername();

        Auction auction = RealtimeDatabase.getLiveAuction(auctionId);
        if (auction == null) return new Response(RequestStatus.NOT_FOUND, "Auction not found");

        if (RealtimeDatabase.isWatchingAuction(username, auctionId))
            return new Response(RequestStatus.SUCCESS, "You are already watching this auction");

        RealtimeDatabase.addAuctionWatcher(auctionId, username);
        return new Response(RequestStatus.SUCCESS, "Join auction successfully"); // TODO: return auctionDto
    }

    public Response leave(ClientHandler client, Request request){ // thoát khỏi auction
        LeaveAuctionRequest data = JsonUtil.fromMap(request.getData(), LeaveAuctionRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");
        if (!client.isValidClient()) return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");

        String auctionId = data.getAuctionId();
        String username = client.getCurrentUsername();

        if (!RealtimeDatabase.isWatchingAuction(username, auctionId))
            return new Response(RequestStatus.FAILED, "You are not watching this auction");

        RealtimeDatabase.removeAuctionWatcher(auctionId, username);
        return new Response(RequestStatus.SUCCESS, "Leave auction successfully");
    }

    public Response placeBid(ClientHandler client, Request request){ // đặt bid mới
        PlaceBidRequest data = JsonUtil.fromMap(request.getData(), PlaceBidRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");
        if (!client.isValidClient()) return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");

        String auctionId = data.getAuctionId();
        double bidAmount = data.getBidAmount();
        String username = client.getCurrentUsername();

        try {
            ValidationUtil.requiresNonBlank(auctionId, "Invalid auction ID");
            ValidationUtil.validatePositiveAmount(bidAmount, "Bid amount must be positive");
        }
        catch (ValidationException e){
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        Auction auction = RealtimeDatabase.getLiveAuction(auctionId);
        User user = RealtimeDatabase.getActiveUser(username);

        if (auction == null)
            return new Response(RequestStatus.NOT_FOUND, "Auction not found or not active");
        if (user == null) return new Response(RequestStatus.FAILED, "User not found");
        if (auction.getSellerUsername().equals(username))
            return new Response(RequestStatus.FAILED, "You cannot bid on your own auction");
        if (user.getWallet() < bidAmount) return new Response(RequestStatus.FAILED, "Insufficient balance");

        Bid bid = new Bid(auction, username, bidAmount);
        if (!auction.placeBid(bid))
            return new Response(RequestStatus.FAILED, "Failed to place bid");

        if (!auctionDao.save(auction)) return new Response(RequestStatus.FAILED, "Failed to update bid in database");

        // thông báo cho những người đang theo dõi
        AuctionDto updateDto = new AuctionDto(
            auction.getId(),
            auction.getAuctionName(),
            auction.getDescription(),
            auction.getSellerUsername(),
            auction.getEndTime().toString(),
            auction.getStartingPrice(),
            auction.getCurrentBid(),
            auction.getBidCount()
        );

        List<ClientHandler> watchers = RealtimeDatabase.getAuctionWatchers(auctionId);
        if (watchers != null){
            for (ClientHandler watcher : watchers){
                if (watcher != null)
                    watcher.sendEvent(new Event(EventType.BID_PLACED, "New bid placed", updateDto));
            }
        }

        return new Response(RequestStatus.SUCCESS, "Place bid successfully");
    }
}
// CREATE_AUCTION, // tạo đấu giá
// UPDATE_AUCTION, // sửa lại cuộc đấu giá trước khi bắt đầu
// GET_AUCTION_DETAIL, // xem chi tiết cuộc đấu giá
// DELETE_AUCTION, // xóa cuộc đấu giá
// JOIN_AUCTION // tham gia vào cuộc đấu giá
// LEAVE_AUCTION // rời khỏi cuộc đấu giá
// PLACE_BID // đặt bid mới
/*
important not null:
id
auction name
description
starting price
seller
startTime
endTime

*/
