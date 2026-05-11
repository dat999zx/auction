package com.bidify.server.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.BidException;
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
import com.bidify.common.model.SearchAuctionRequest;
import com.bidify.common.model.UpdateAuctionRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.exception.InsufficientBalanceException;
import com.bidify.server.model.Auction;
import com.bidify.server.model.AuctionImage;
import com.bidify.server.model.Bid;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.model.Wallet;
import com.bidify.server.model.runtime.AuctionChannel;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.AuctionMapper;
import com.bidify.server.utility.ServiceUtil;

// service xử lý các logic liên quan đến auction, tương tác với database thông qua AuctionDao và cập nhật realtime database để đồng bộ với client
public class AuctionService {
    private static Logger logger = LoggerFactory.getLogger(AuctionService.class);
    private static AuctionService instance = new AuctionService();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    private final BidDao bidDao = BidDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();
    private final ImageService imageService = ImageService.getInstance();

    private AuctionService() {}

    public static AuctionService getInstance() { return instance; }

    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.JOIN_AUCTION, this::join);
        router.register(RequestType.LEAVE_AUCTION, this::leave);
        router.register(RequestType.CREATE_AUCTION, this::create);
        router.register(RequestType.UPDATE_AUCTION, this::update);
        router.register(RequestType.GET_LIVE_AUCTIONS, (client, req) -> getAllLiveAuctions());
        router.register(RequestType.GET_AUCTION_DETAIL, (client, req) -> getDetail(req));
        router.register(RequestType.DELETE_AUCTION, this::delete);
        router.register(RequestType.PLACE_BID, this::placeBid);
        router.register(RequestType.SEARCH_AUCTIONS, (client, req) -> search(req));
    }

    // load runtime auctions trong sql lên ram, chỉ gọi 1 lần khi server khởi chạy
    public void loadToRuntime(){
        List<Auction> runtimeAuctions = new ArrayList<>();
        runtimeAuctions.addAll(auctionDao.findByStatus(AuctionStatus.UPCOMING));
        runtimeAuctions.addAll(auctionDao.findByStatus(AuctionStatus.ACTIVE));

        for (Auction auction : runtimeAuctions)
            RealtimeDatabase.addRuntimeAuction(auction);
    }

    public Response search(Request request) {
        return ServiceUtil.handleRequest(() -> {
            SearchAuctionRequest data = JsonUtil.fromMap(request.getData(), SearchAuctionRequest.class);
            ServiceUtil.validateRequestData(data);

            // Xử lý query để tiện hơn trong việc đối chiếu
            String query = data.getQuery();
            if (query == null) query = "";
            String finalQuery = query.toLowerCase().trim();

            List<Auction> allAuctions = RealtimeDatabase.getAllRuntimeAuctions();
            List<AuctionDto> results = new ArrayList<>(); // lưu các auctiondto thỏa mãn

            for (Auction auction : allAuctions) {
                // lấy các auction thỏa mãn 2 điều kiện: chứa tên / description
                boolean matchesName = auction.getAuctionName() != null && auction.getAuctionName().toLowerCase().contains(finalQuery);
                boolean matchesDesc = auction.getDescription() != null && auction.getDescription().toLowerCase().contains(finalQuery);
                boolean matchesSeller = auction.getSellerUsername() != null && auction.getSellerUsername().toLowerCase().contains(finalQuery);
                if (matchesName || matchesDesc || matchesSeller) {
                    AuctionDto dto = AuctionMapper.toDto(auction);
                    dto.setThumbnailBase64(getThumbnail(auction.getId()));
                    results.add(dto);
                }
            }

            return new Response(RequestStatus.SUCCESS, "Search completed", results);
        });
    }

    public Response create(ClientHandler client, Request request){
        return ServiceUtil.handleRequest(() -> {
            CreateAuctionRequest data = JsonUtil.fromMap(request.getData(), CreateAuctionRequest.class);
            ServiceUtil.validateRequestData(data);

            ServiceUtil.requireSession(client);

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
                throw new ValidationException("You are not the seller of this auction");

            validateAuctionTime(startTime, endTime);

            Auction auction = new Auction(auctionName, description, sellerUsername, startingPrice, startTime, endTime);
            auction.setCategory(category);
            auction.setProductType(productType);
            auction.setMinIncrement(minIncrement);
            auctionDao.create(auction);

            List<String> images = data.getImagesBase64();
            if (images != null && !images.isEmpty()) {
                List<String> savedPaths = imageService.saveImages(auction.getId(), images);
                auctionDao.saveAuctionImages(auction.getId(), savedPaths);
            }

            RealtimeDatabase.addRuntimeAuction(auction);

            AuctionDto auctionDto = AuctionMapper.toDto(auction);
            RealtimeDatabase.getGlobalChannel().publish(new Event(EventType.AUCTION_CREATED, "New auction created", auctionDto));

            return new Response(RequestStatus.SUCCESS, "Create new auction successfully!");
        });
    }

    public Response update(ClientHandler client, Request request){
        return ServiceUtil.handleRequest(() -> {
            UpdateAuctionRequest data = JsonUtil.fromMap(request.getData(), UpdateAuctionRequest.class);
            ServiceUtil.validateRequestData(data);

            ServiceUtil.requireSession(client);

            String auctionId = data.getAuctionId();
            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");

            Auction auction = RealtimeDatabase.getUpcomingAuction(auctionId);
            if (auction == null) auction = auctionDao.findById(auctionId);
            if (auction == null)
                throw new AuctionException("Auction not found");

            requireSeller(auction, client.getCurrentUsername(), "You don't have permission to update this auction");
            if (auction.getStatus() != AuctionStatus.UPCOMING)
                throw new AuctionException("Can only update auction before it starts");

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
        return ServiceUtil.handleRequest(() -> {
            DeleteAuctionRequest data = JsonUtil.fromMap(request.getData(), DeleteAuctionRequest.class);
            ServiceUtil.validateRequestData(data);

            ServiceUtil.requireSession(client);

            String auctionId = data.getId();
            ValidationUtil.requiresNonBlank(auctionId, "Auction id");

            Auction auction = RealtimeDatabase.getUpcomingAuction(auctionId);
            if (auction == null) auction = auctionDao.findById(auctionId);
            if (auction == null)
                throw new AuctionException("Auction not found");

            if (auction.getStatus() != AuctionStatus.UPCOMING)
                throw new AuctionException("Cannot delete auction after it has started");

            requireSeller(auction, client.getCurrentUsername(), "Only seller can delete their auction");

            auctionDao.deleteById(auctionId);
            RealtimeDatabase.removeRuntimeAuction(auctionId);

            RealtimeDatabase.getGlobalChannel().publish(new Event(EventType.AUCTION_DELETED, "Auction deleted", auctionId));

            return new Response(RequestStatus.SUCCESS, "Auction deleted successfully");
        });
    }

    public Response getDetail(Request request){
        return ServiceUtil.handleRequest(() -> {
            GetAuctionDetailRequest data = JsonUtil.fromMap(request.getData(), GetAuctionDetailRequest.class);
            ServiceUtil.validateRequestData(data);

            String auctionId = data.getAuctionId();
            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");

            Auction auction = auctionDao.findById(auctionId);
            if (auction == null)
                throw new AuctionException("Auction not found");

            AuctionDto auctionDto = AuctionMapper.toDto(auction);
            auctionDto.setThumbnailBase64(getThumbnail(auctionId));
            auctionDto.setGalleryBase64(getGallery(auctionId));
            return new Response(RequestStatus.SUCCESS, "Get auction detail successfully", auctionDto);
        });
    }

    public Response getAllLiveAuctions(){
        return ServiceUtil.handleRequest(() -> {
            List<Auction> auctions = RealtimeDatabase.getAllLiveAuctions();
            List<AuctionDto> summaries = new ArrayList<>();

            if (auctions == null || auctions.isEmpty())
                return new Response(RequestStatus.SUCCESS, "No live auctions", summaries);

            for (Auction auction : auctions) {
                AuctionDto dto = AuctionMapper.toDto(auction);
                dto.setThumbnailBase64(getThumbnail(auction.getId()));
                summaries.add(dto);
            }

            return new Response(RequestStatus.SUCCESS, "Get live auctions successfully", summaries);
        });
    }

    // lấy ảnh chính
    private String getThumbnail(String auctionId) {
        try {
            List<AuctionImage> images = auctionDao.getAuctionImages(auctionId);
            for (AuctionImage image : images) {
                if (image.isPrimary())
                    return imageService.getBase64Image(image.getFilePath());
            }
        }
        catch (DatabaseException e) {
            logger.error("Error getting thumbnail", e);
        }
        return null;
    }

    // lấy tất cả ảnh của auction
    private List<String> getGallery(String auctionId) {
        List<String> gallery = new ArrayList<>();
        try {
            List<AuctionImage> images = auctionDao.getAuctionImages(auctionId);
            for (AuctionImage image : images) {
                String base64 = imageService.getBase64Image(image.getFilePath());
                if (base64 != null) gallery.add(base64);
            }
        }
        catch (DatabaseException e) {
            logger.error("Error getting gallery", e);
        }
        return gallery;
    }

    public Response join(ClientHandler client, Request request){
        return ServiceUtil.handleRequest(() -> {
            JoinAuctionRequest data = JsonUtil.fromMap(request.getData(), JoinAuctionRequest.class);
            ServiceUtil.validateRequestData(data);

            ServiceUtil.requireSession(client);

            String auctionId = data.getAuctionId();
            String username = client.getCurrentUsername();

            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");
            Auction auction = RealtimeDatabase.getRuntimeAuction(auctionId);
            if (auction == null)
                throw new AuctionException("Auction not found");

            if (!isRuntimeAuction(auction))
                throw new AuctionException("Auction not found");
            if (RealtimeDatabase.isWatchingAuction(username, auctionId))
                return new Response(RequestStatus.SUCCESS, "You are already watching this auction");

            RealtimeDatabase.subscribeAuctionChannel(auctionId, username);

            AuctionDto auctionDto = AuctionMapper.toDto(auction);
            return new Response(RequestStatus.SUCCESS, "Join auction successfully", auctionDto);
        });
    }

    public Response leave(ClientHandler client, Request request){
        return ServiceUtil.handleRequest(() -> {
            LeaveAuctionRequest data = JsonUtil.fromMap(request.getData(), LeaveAuctionRequest.class);
            ServiceUtil.validateRequestData(data);
            ServiceUtil.requireSession(client);

            String auctionId = data.getAuctionId();
            String username = client.getCurrentUsername();

            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");

            if (!RealtimeDatabase.isWatchingAuction(username, auctionId))
                throw new AuctionException("You are not watching this auction");

            RealtimeDatabase.unsubscribeAuctionChannel(auctionId, username);
            return new Response(RequestStatus.SUCCESS, "Leave auction successfully");
        });
    }

    public Response placeBid(ClientHandler client, Request request){
        return ServiceUtil.handleRequest(() -> {
            PlaceBidRequest data = JsonUtil.fromMap(request.getData(), PlaceBidRequest.class);
            ServiceUtil.validateRequestData(data);
            ServiceUtil.requireSession(client);

            String auctionId = data.getAuctionId();
            double bidAmount = data.getBidAmount();
            String username = client.getCurrentUsername();

            ValidationUtil.requiresNonBlank(auctionId, "Invalid auction ID");
            ValidationUtil.validatePositiveAmount(bidAmount, "Bid amount must be positive");

            Auction auction = RealtimeDatabase.getLiveAuction(auctionId);
            User user = RealtimeDatabase.getActiveUser(username);

            if (auction == null)
                throw new AuctionException("Auction not found");
            if (user == null)
                throw new AuthException("User not found");
            if (auction.getSellerUsername().equals(username))
                throw new AuctionException("You cannot bid on your own auction");

            user.tryLock(5);

            try {
                Wallet wallet = user.getWallet();
                
                if (wallet.getAvailableBalance() < bidAmount)
                    throw new InsufficientBalanceException();
    
                String prevBidderUsername = auction.getCurrentBidderUsername();
                double prevBid = auction.getCurrentBid();
                if (prevBidderUsername != null && prevBidderUsername.equals(username))
                    throw new AuctionException("You are already the highest bidder");
    
                Bid bid = new Bid(auction.getId(), username, bidAmount);
                auction.placeBid(bid);
    
                wallet.lockBalance(bidAmount);
                User prevBidder = RealtimeDatabase.getActiveUser(prevBidderUsername);
                if (prevBidder != null) {
                    prevBidder.getWallet().unlockBalance(prevBid);
                    publishLockedBalanceChange(prevBidderUsername, prevBid);
                }
    
                bidDao.create(bid);
                auctionDao.save(auction);
    
                AuctionDto auctionDto = AuctionMapper.toDto(auction);
                AuctionChannel auctionChannel = RealtimeDatabase.getAuctionChannel(auctionId);
                if (auctionChannel != null)
                    auctionChannel.publish(new Event(EventType.BID_PLACED, "New bid placed", auctionDto));
    
                //save changes to database after placing bid.
                if (prevBidder != null)
                    userDao.save(prevBidder, false);
    
                logger.info("bid placed: auction {} - {}, user {}: {}$", auction.getAuctionName(), auction.getId(), username, bidAmount);
    
                return new Response(RequestStatus.SUCCESS, "Place bid successfully");
            }
            finally {
                user.unlock();
            }
        });
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
        if (LocalDateTime.now().minusMinutes(1).isAfter(startTime))
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

    // chuyển auction từ ACTIVE -> ENDED, chuyển tiền các thứ...
    public void settleAuction(Auction auction) {
        String sellerUsername = auction.getSellerUsername();
        String winnerUsername = auction.getCurrentBidderUsername();
        double finalBid = auction.getCurrentBid();

        if (winnerUsername != null) {
            User winner = ServiceUtil.getOrLoadUser(winnerUsername);
            User seller = ServiceUtil.getOrLoadUser(sellerUsername);
            
            winner.getWallet().payWinAuction(finalBid);
            transactionDao.create(new Transaction(winnerUsername, TransactionType.AUCTION_PAY, finalBid, auction.getId()));

            seller.getWallet().deposit(finalBid);
            transactionDao.create(new Transaction(sellerUsername, TransactionType.AUCTION_PROFIT, finalBid, auction.getId()));

            auction.setStatus(AuctionStatus.ENDED);

            userDao.save(winner, false);
            userDao.save(seller, false);

            publishBalanceChange(winnerUsername, finalBid);
            publishLockedBalanceChange(winnerUsername, -finalBid);
        }
        else {
            auction.setEndTime(LocalDateTime.now());
            auction.setStatus(AuctionStatus.CANCELED);
        }
        
        auctionDao.save(auction);

        logger.info("auction settled: {} - {}", auction.getAuctionName(), auction.getId());
    }

    // Event cập nhật wallet của User
    private void publishBalanceChange(String username, double diff) {
        ClientHandler userClient = RealtimeDatabase.getUserClient(username);
        if (userClient == null) return;
        Event event = new Event(EventType.WALLET_CHANGED, "Wallet changed: " + diff);
        userClient.sendEvent(event);
    }

    // Event cập nhật lockedBalance của User
    private void publishLockedBalanceChange(String username, double diff) {
        ClientHandler userClient = RealtimeDatabase.getUserClient(username);
        if (userClient == null) return;
        Event event = new Event(EventType.SERVER_NOTICE, "Locked balance changed: " + diff);
        userClient.sendEvent(event);
    }
}
