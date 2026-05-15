package com.bidify.server.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.AuthException;
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
import com.bidify.server.dao.ImageDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.exception.InsufficientBalanceException;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.ItemImageLink;
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
    private final ItemDao itemDao = ItemDao.getInstance();
    private final ImageDao imageDao = ImageDao.getInstance();
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
        router.register(RequestType.GET_UPCOMING_AUCTIONS, (client, req) -> getAllUpcomingAuctions());
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
                Item item = getLinkedAuctionItem(auction);
                String auctionName = item != null ? item.getName() : auction.getAuctionName();
                String description = item != null ? item.getDescription() : auction.getDescription();
                String category = item != null ? item.getCategory() : auction.getCategory();
                String productType = item != null ? item.getProductType() : auction.getProductType();

                boolean matchesName = auctionName != null && auctionName.toLowerCase().contains(finalQuery);
                boolean matchesDesc = description != null && description.toLowerCase().contains(finalQuery);
                boolean matchesSeller = auction.getSellerUsername() != null && auction.getSellerUsername().toLowerCase().contains(finalQuery);
                boolean matchesCategory = category != null && category.toLowerCase().contains(finalQuery);
                boolean matchesProductType = productType != null && productType.toLowerCase().contains(finalQuery);
                if (matchesName || matchesDesc || matchesSeller || matchesCategory || matchesProductType)
                    results.add(toAuctionDto(auction, item, false));
            }

            return new Response(RequestStatus.SUCCESS, "Search completed", results);
        });
    }

    public Response create(ClientHandler client, Request request){
        return ServiceUtil.handleRequest(() -> {
            CreateAuctionRequest data = JsonUtil.fromMap(request.getData(), CreateAuctionRequest.class);
            ServiceUtil.validateRequestData(data);

            ServiceUtil.requireSession(client);

            String sellerUsername = client.getCurrentUsername();
            String itemId = data.getItemId();
            double startingPrice = data.getStartingPrice();
            double minIncrement = data.getMinIncrement();
            LocalDateTime startTime = parseDateTime(data.getStartTime());
            LocalDateTime endTime = parseDateTime(data.getEndTime());

            ValidationUtil.requiresNonBlank(itemId, "Item ID");
            ValidationUtil.validatePositiveAmount(startingPrice, "Starting price");
            ValidationUtil.validatePositiveAmount(minIncrement, "Min increment");
            validateAuctionTime(startTime, endTime);

            Item item = requireAvailableOwnedItem(itemId, sellerUsername);

            Auction auction = new Auction(sellerUsername, itemId, startingPrice, startTime, endTime);
            auction.setAuctionName(item.getName());
            auction.setDescription(item.getDescription());
            auction.setMinIncrement(minIncrement);
            auctionDao.create(auction);
            itemDao.updateAvailabilityStatus(itemId, ItemStatus.LOCKED_IN_AUCTION);
            item.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);

            RealtimeDatabase.addRuntimeAuction(auction);

            AuctionDto auctionDto = toAuctionDto(auction, item, false);
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

            double startingPrice = data.getStartingPrice();
            double minIncrement = data.getMinIncrement();
            LocalDateTime startTime = parseDateTime(data.getStartTime());
            LocalDateTime endTime = parseDateTime(data.getEndTime());

            validateAuctionUpdateFields(auction.getSellerUsername(), startingPrice, minIncrement);
            validateAuctionTime(startTime, endTime);

            auction.setStartingPrice(startingPrice);
            auction.setMinIncrement(minIncrement);
            auction.setStartTime(startTime);
            auction.setEndTime(endTime);

            auctionDao.save(auction);

            AuctionDto auctionDto = toAuctionDto(auction, false);
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

            if (auction.getItemId() != null && !auction.getItemId().isBlank())
                itemDao.updateAvailabilityStatus(auction.getItemId(), ItemStatus.AVAILABLE);

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

            AuctionDto auctionDto = toAuctionDto(auction, true);
            return new Response(RequestStatus.SUCCESS, "Get auction detail successfully", auctionDto);
        });
    }

    public Response getAllLiveAuctions(){
        return ServiceUtil.handleRequest(() -> {
            List<Auction> auctions = RealtimeDatabase.getAllLiveAuctions();
            List<AuctionDto> summaries = new ArrayList<>();

            if (auctions == null || auctions.isEmpty())
                return new Response(RequestStatus.SUCCESS, "No live auctions", summaries);

            for (Auction auction : auctions)
                summaries.add(toAuctionDto(auction, false));

            return new Response(RequestStatus.SUCCESS, "Get live auctions successfully", summaries);
        });
    }

    public Response getAllUpcomingAuctions(){
        return ServiceUtil.handleRequest(() -> {
            List<Auction> auctions = RealtimeDatabase.getAllUpcomingAuctions();
            List<AuctionDto> summaries = new ArrayList<>();

            if (auctions == null || auctions.isEmpty())
                return new Response(RequestStatus.SUCCESS, "No upcoming auctions", summaries);

            for (Auction auction : auctions)
                summaries.add(toAuctionDto(auction, false));

            return new Response(RequestStatus.SUCCESS, "Get upcoming auctions successfully", summaries);
        });
    }

    // lấy ảnh chính
    private String getThumbnail(Item item) {
        if (item == null) return null;
        try {
            List<ItemImageLink> links = itemDao.getItemImageLinks(item.getId());
            for (ItemImageLink link : links) {
                if (!link.isPrimary()) continue;
                Image image = imageDao.findById(link.getImageId());
                if (image != null)
                    return imageService.getBase64Image(image.getFilePath());
            }
            for (ItemImageLink link : links) {
                Image image = imageDao.findById(link.getImageId());
                if (image != null)
                    return imageService.getBase64Image(image.getFilePath());
            }
        }
        catch (DatabaseException e) {
            logger.error("Error getting thumbnail", e);
        }
        return null;
    }

    // lấy tất cả ảnh của auction
    private List<String> getGallery(Item item) {
        List<String> gallery = new ArrayList<>();
        if (item == null) return gallery;
        try {
            List<ItemImageLink> links = itemDao.getItemImageLinks(item.getId());
            for (ItemImageLink link : links) {
                Image image = imageDao.findById(link.getImageId());
                if (image == null) continue;
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

            AuctionDto auctionDto = toAuctionDto(auction, false);
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
    
                AuctionDto auctionDto = toAuctionDto(auction, false);
                AuctionChannel auctionChannel = RealtimeDatabase.getAuctionChannel(auctionId);
                if (auctionChannel != null)
                    auctionChannel.publish(new Event(EventType.BID_PLACED, "New bid placed", auctionDto));
                RealtimeDatabase.getGlobalChannel().publish(new Event(EventType.BID_PLACED, "New bid placed", auctionDto));
                publishLockedBalanceChange(username, bidAmount);
    
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

    private Item requireAvailableOwnedItem(String itemId, String username) {
        Item item = itemDao.findById(itemId);
        if (item == null)
            throw new ValidationException("Item not found");
        if (!username.equals(item.getOwnerUsername()))
            throw new ValidationException("You do not own this item");
        if (item.getAvailabilityStatus() != ItemStatus.AVAILABLE)
            throw new ValidationException("Item is not available for auction");
        return item;
    }

    private Item getLinkedAuctionItem(Auction auction) {
        if (auction == null) return null;

        String itemId = auction.getItemId();
        if (itemId == null || itemId.isBlank()) return null;

        return itemDao.findById(itemId);
    }

    public AuctionDto toAuctionDto(Auction auction, boolean includeGallery) {
        return toAuctionDto(auction, getLinkedAuctionItem(auction), includeGallery);
    }

    public AuctionDto toAuctionDto(Auction auction, Item item, boolean includeGallery) {
        List<String> gallery = includeGallery ? getGallery(item) : null;
        return AuctionMapper.toDto(auction, item, getThumbnail(item), gallery);
    }

    private void updateAuctionItemState(Auction auction, String ownerUsername, ItemStatus status) {
        Item item = getLinkedAuctionItem(auction);
        if (item == null) return;

        item.setOwnerUsername(ownerUsername);
        item.setAvailabilityStatus(status);
        itemDao.save(item);
    }

    private void validateAuctionUpdateFields(String sellerUsername, double startingPrice, double minIncrement) {
        ValidationUtil.requiresNonBlank(sellerUsername, "Seller");
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

            updateAuctionItemState(auction, winnerUsername, ItemStatus.AVAILABLE);

            auction.setStatus(AuctionStatus.ENDED);

            userDao.save(winner, false);
            userDao.save(seller, false);

            publishBalanceChange(winnerUsername, finalBid);
            publishLockedBalanceChange(winnerUsername, -finalBid);
            publishBalanceChange(sellerUsername, finalBid);
        }
        else {
            updateAuctionItemState(auction, sellerUsername, ItemStatus.AVAILABLE);
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
        Event event = new Event(EventType.LOCKED_BALANCE_CHANGED, "Locked balance changed: " + diff);
        userClient.sendEvent(event);
    }
}
