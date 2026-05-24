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
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.DeleteAuctionRequest;
import com.bidify.common.model.Event;
import com.bidify.common.model.GetAuctionDetailRequest;
import com.bidify.common.model.JoinAuctionRequest;
import com.bidify.common.model.LeaveAuctionRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.SearchAuctionRequest;
import com.bidify.common.model.UpdateAuctionRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.TimeUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Auction;
import com.bidify.server.model.AutoBid;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.model.Wallet;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.service.auction.AuctionBidProcessor;
import com.bidify.server.service.auction.AuctionDtoAssembler;
import com.bidify.server.service.auction.AuctionRealtimePublisher;
import com.bidify.server.service.auction.AuctionSettlementProcessor;
import com.bidify.server.utility.ServiceUtil;

// service xử lý các logic liên quan đến auction, tương tác với database thông qua AuctionDao và cập nhật realtime database để đồng bộ với client
public class AuctionService {
    private static Logger logger = LoggerFactory.getLogger(AuctionService.class);
    private static AuctionService instance = new AuctionService();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    private final BidDao bidDao = BidDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();
    private final AuctionRealtimePublisher realtimePublisher = new AuctionRealtimePublisher();
    private final AuctionDtoAssembler auctionDtoAssembler = new AuctionDtoAssembler();
    private final AuctionSettlementProcessor settlementProcessor =
            new AuctionSettlementProcessor(auctionDao, transactionDao, auctionDtoAssembler, realtimePublisher);
    private final AuctionBidProcessor bidProcessor =
            new AuctionBidProcessor(auctionDao, bidDao, userDao, auctionDtoAssembler, realtimePublisher);

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
        router.register(RequestType.GET_AUCTION_DETAIL, this::getDetail);
        router.register(RequestType.DELETE_AUCTION, this::delete);
        router.register(RequestType.PLACE_BID, bidProcessor::placeBid);
        router.register(RequestType.SET_AUTO_BID, bidProcessor::setAutoBid);
        router.register(RequestType.DISABLE_AUTO_BID, bidProcessor::disableAutoBid);
        router.register(RequestType.SEARCH_AUCTIONS, (client, req) -> search(req));
        router.register(RequestType.PAY_AUCTION, settlementProcessor::payAuction);
        router.register(RequestType.CONFIRM_AUCTION_DELIVERY, settlementProcessor::confirmAuctionDelivery);
        router.register(RequestType.RESOLVE_AUCTION, settlementProcessor::resolveAuction);
        router.register(RequestType.GET_USER_SETTLEMENTS, this::getUserSettlements);
        router.register(RequestType.GET_ADMIN_AUCTIONS, this::getAdminAuctions);
        router.register(RequestType.GET_MY_AUCTIONS, this::getMyAuctions);
    }

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
                Item item = auctionDtoAssembler.getLinkedAuctionItem(auction);
                String auctionName = item != null ? item.getName() : auction.getAuctionName();
                String description = item != null ? item.getDescription() : auction.getDescription();
                String category = item != null ? item.getCategory() : null;
                String productType = item != null ? item.getProductType() : null;

                boolean matchesName = auctionName != null && auctionName.toLowerCase().contains(finalQuery);
                boolean matchesDesc = description != null && description.toLowerCase().contains(finalQuery);
                boolean matchesSeller = auction.getSellerUsername() != null && auction.getSellerUsername().toLowerCase().contains(finalQuery);
                boolean matchesCategory = category != null && category.toLowerCase().contains(finalQuery);
                boolean matchesProductType = productType != null && productType.toLowerCase().contains(finalQuery);
                if (matchesName || matchesDesc || matchesSeller || matchesCategory || matchesProductType)
                    results.add(auctionDtoAssembler.toAuctionDto(auction, item, false));
            }

            return new Response(RequestStatus.SUCCESS, "Search completed", results);
        });
    }

    public Response create(ClientHandler client, Request request){
        return ServiceUtil.handleRequest(() -> {
            CreateAuctionRequest data = JsonUtil.fromMap(request.getData(), CreateAuctionRequest.class);
            ServiceUtil.validateRequestData(data);

            User sessionUser = ServiceUtil.requireSessionUser(client);
            ServiceUtil.requireUserRole(sessionUser, "Admin accounts cannot create auctions");

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
            
            // Set anti-sniping configuration
            java.time.Duration triggerTime = TimeUtil.parseHHMM(data.getTriggerTime());
            java.time.Duration extensionTime = TimeUtil.parseHHMM(data.getExtensionTime());
            LocalDateTime maxDelay = LocalDateTime.parse(data.getMaxExtensionTime());

            auction.setAntiSnipingTriggerTime(triggerTime);
            auction.setAntiSnipingExtensionTime(extensionTime);
            auction.setMaxEndTime(maxDelay);

            auctionDao.create(auction);
            itemDao.updateAvailabilityStatus(itemId, ItemStatus.LOCKED_IN_AUCTION);
            item.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);

            RealtimeDatabase.addRuntimeAuction(auction);

            AuctionDto auctionDto = auctionDtoAssembler.toAuctionDto(auction, item, false);
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
            
            // Update anti-sniping configuration
            java.time.Duration triggerTime = TimeUtil.parseHHMM(data.getTriggerTime());
            java.time.Duration extensionTime = TimeUtil.parseHHMM(data.getExtensionTime());
            LocalDateTime maxDelay = LocalDateTime.parse(data.getMaxExtensionTime());

            auction.setAntiSnipingTriggerTime(triggerTime);
            auction.setAntiSnipingExtensionTime(extensionTime);
            auction.setMaxEndTime(maxDelay);

            auctionDao.save(auction);

            AuctionDto auctionDto = auctionDtoAssembler.toAuctionDto(auction, false);
            RealtimeDatabase.getGlobalChannel().publish(new Event(EventType.AUCTION_UPDATED, "Auction updated", auctionDto));

            return new Response(RequestStatus.SUCCESS, "Auction updated successfully!", auctionDto);
        });
    }

    // Lưu trạng thái của tất cả các phiên đấu giá đang chạy nền vào SQLite.
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

            Auction auction = RealtimeDatabase.getRuntimeAuction(auctionId);
            if (auction == null) auction = auctionDao.findById(auctionId);
            if (auction == null)
                throw new AuctionException("Auction not found");

            requireSeller(auction, client.getCurrentUsername(), "Only seller can delete their auction");

            if (auction.getStatus() == AuctionStatus.UPCOMING) {
                deleteAuctionCascade(auction, true);
                return new Response(RequestStatus.SUCCESS, "Auction deleted successfully");
            }

            if (auction.getStatus() == AuctionStatus.ACTIVE) {
                synchronized (auction) {
                    cancelActiveAuction(auction);
                }
                return new Response(RequestStatus.SUCCESS, "Auction canceled successfully", auctionDtoAssembler.toAuctionDto(auction, false));
            }

            throw new AuctionException("Can only delete upcoming auctions or cancel active auctions");
        });
    }

    public Response getDetail(ClientHandler client, Request request){
        return ServiceUtil.handleRequest(() -> {
            GetAuctionDetailRequest data = JsonUtil.fromMap(request.getData(), GetAuctionDetailRequest.class);
            ServiceUtil.validateRequestData(data);

            String auctionId = data.getAuctionId();
            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");

            Auction auction = RealtimeDatabase.getRuntimeAuction(auctionId);
            if (auction == null)
                auction = auctionDao.findById(auctionId);
            if (auction == null)
                throw new AuctionException("Auction not found");

            AuctionDto auctionDto = auctionDtoAssembler.toAuctionDto(auction, true);
            if (client != null && client.isInSession()) {
                AutoBid currentUserAutoBid = auction.getAutoBid(client.getCurrentUsername());
                auctionDto.setCurrentUserAutoBidActive(currentUserAutoBid != null);
                auctionDto.setCurrentUserAutoBidMax(currentUserAutoBid == null ? null : currentUserAutoBid.getMaxBid());
            }
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
                summaries.add(auctionDtoAssembler.toAuctionDto(auction, false));

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
                summaries.add(auctionDtoAssembler.toAuctionDto(auction, false));

            return new Response(RequestStatus.SUCCESS, "Get upcoming auctions successfully", summaries);
        });
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

            AuctionDto auctionDto = auctionDtoAssembler.toAuctionDto(auction, false);
            publishAuctionUpdate(auction, "Auction watcher count updated");
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
            Auction auction = RealtimeDatabase.getRuntimeAuction(auctionId);
            if (auction != null)
                publishAuctionUpdate(auction, "Auction watcher count updated");
            return new Response(RequestStatus.SUCCESS, "Leave auction successfully");
        });
    }

    public Response placeBid(ClientHandler client, Request request) {
        return bidProcessor.placeBid(client, request);
    }

    public Response setAutoBid(ClientHandler client, Request request) {
        return bidProcessor.setAutoBid(client, request);
    }

    public Response disableAutoBid(ClientHandler client, Request request) {
        return bidProcessor.disableAutoBid(client, request);
    }

    private void requireSeller(Auction auction, String username, String message) {
        if (auction == null || username == null || !ServiceUtil.isOwnerOrAdmin(auction.getSellerUsername(), username))
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

    private void publishAuctionUpdate(Auction auction, String message) {
        realtimePublisher.publishAuctionUpdate(auction, auctionDtoAssembler.toAuctionDto(auction, false), message);
    }

    public void publishLiveAudienceUpdate(String auctionId) {
        if (auctionId == null || auctionId.isBlank())
            return;

        Auction auction = RealtimeDatabase.getLiveAuction(auctionId);
        if (auction == null)
            return;

        publishAuctionUpdate(auction, "Auction watcher count updated");
    }

    private void cancelActiveAuction(Auction auction) throws DatabaseException {
        if (auction == null)
            throw new AuctionException("Auction not found");
        if (auction.getStatus() != AuctionStatus.ACTIVE)
            throw new AuctionException("Can only cancel active auctions");

        releaseCurrentLeaderLock(auction);
        updateAuctionItemState(auction, auction.getSellerUsername(), ItemStatus.AVAILABLE);

        auction.setStatus(AuctionStatus.CANCELED);
        auctionDao.save(auction);

        publishAuctionUpdate(auction, "Auction canceled");
        RealtimeDatabase.removeRuntimeAuction(auction.getId());
    }

    public void deleteAuctionCascade(Auction auction, boolean restoreLinkedItemToSeller) {
        if (auction == null)
            return;

        releaseCurrentLeaderLock(auction);

        if (restoreLinkedItemToSeller && auction.getItemId() != null && !auction.getItemId().isBlank())
            itemDao.updateAvailabilityStatus(auction.getItemId(), ItemStatus.AVAILABLE);

        bidDao.deleteByAuctionId(auction.getId());
        transactionDao.deleteByAuctionId(auction.getId());
        RealtimeDatabase.removeRuntimeAuction(auction.getId());
        auctionDao.deleteById(auction.getId());
        realtimePublisher.publishAuctionDeleted(auction);
    }

    private void releaseCurrentLeaderLock(Auction auction) {
        String currentBidderUsername = auction.getCurrentBidderUsername();
        double currentBid = auction.getCurrentBid();

        if (currentBidderUsername == null || currentBid <= 0)
            return;

        User activeBidder = RealtimeDatabase.getActiveUser(currentBidderUsername);
        User bidder = activeBidder != null ? activeBidder : ServiceUtil.getOrLoadUser(currentBidderUsername);
        if (bidder == null)
            return;

        Wallet wallet = bidder.getWallet();
        if (wallet == null || wallet.getLockedBalance() < currentBid)
            return;

        wallet.unlockBalance(currentBid);
        userDao.save(bidder, false);

        ClientHandler clientHandler = RealtimeDatabase.getUserClient(currentBidderUsername);
        if (clientHandler != null) {
            clientHandler.sendEvent(new Event(
                EventType.LOCKED_BALANCE_CHANGED,
                "Locked balance changed: -" + currentBid
            ));
        }
    }

    private void updateAuctionItemState(Auction auction, String ownerUsername, ItemStatus status) {
        Item item = auctionDtoAssembler.getLinkedAuctionItem(auction);
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
        if (TimeUtil.nowInVietnam().minusMinutes(1).isAfter(startTime))
            throw new ValidationException("Start time must be in the future");
    }

    private LocalDateTime parseDateTime(String value) {
        return TimeUtil.parseDateTime(value);
    }

    private boolean isRuntimeAuction(Auction auction) {
        if (auction == null) return false;
        AuctionStatus status = auction.getStatus();
        return status == AuctionStatus.UPCOMING || status == AuctionStatus.ACTIVE;
    }

    // Chuyển trạng thái đấu giá từ ACTIVE -> ENDED hoặc AWAITING_PAYMENT, xử lý giải phóng tiền đặt cọc.
    public void settleAuction(Auction auction) {
        String sellerUsername = auction.getSellerUsername();
        String winnerUsername = auction.getCurrentBidderUsername();

        if (winnerUsername != null) {
            auction.setStatus(AuctionStatus.AWAITING_PAYMENT);
            auctionDao.save(auction);
            publishAuctionUpdate(auction, "Auction ended, awaiting payment from winner");
        }
        else {
            updateAuctionItemState(auction, sellerUsername, ItemStatus.AVAILABLE);
            auction.setStatus(AuctionStatus.CANCELED);
            auctionDao.save(auction);
        }

        logger.info("auction settled: {} - {} (status: {})", auction.getAuctionName(), auction.getId(), auction.getStatus());
    }

    // Gửi sự kiện cập nhật số dư ví đến client của user
    private void publishBalanceChange(String username, double diff) {
        realtimePublisher.publishBalanceChange(username, diff);
    }

    // Gửi sự kiện cập nhật số dư bị giữ đến client của user
    private void publishLockedBalanceChange(String username, double diff) {
        realtimePublisher.publishLockedBalanceChange(username, diff);
    }

    public Response getUserSettlements(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User user = ServiceUtil.requireSessionUser(client);
            List<Auction> auctions = auctionDao.findUserSettlements(user.getUsername());
            List<AuctionDto> result = new ArrayList<>();
            for (Auction auction : auctions) {
                result.add(auctionDtoAssembler.toAuctionDto(auction, false));
            }
            return new Response(RequestStatus.SUCCESS, "Get user settlements successfully", result);
        });
    }

    public Response getMyAuctions(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User user = ServiceUtil.requireSessionUser(client);
            String username = user.getUsername();
            List<Auction> dbAuctions = auctionDao.findBySellerUsername(username);
            List<AuctionDto> result = new ArrayList<>();
            for (Auction auction : dbAuctions) {
                Auction runtime = RealtimeDatabase.getRuntimeAuction(auction.getId());
                Auction effective = runtime != null ? runtime : auction;
                result.add(auctionDtoAssembler.toAuctionDto(effective, false));
            }
            return new Response(RequestStatus.SUCCESS, "Get my auctions successfully", result);
        });
    }

    public Response getAdminAuctions(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);

            List<Auction> dbAuctions = auctionDao.findAll();
            List<AuctionDto> result = new ArrayList<>();
            for (Auction auction : dbAuctions) {
                Auction runtime = RealtimeDatabase.getRuntimeAuction(auction.getId());
                Auction effective = runtime != null ? runtime : auction;
                result.add(auctionDtoAssembler.toAuctionDto(effective, false));
            }

            return new Response(RequestStatus.SUCCESS, "Get admin auctions successfully", result);
        });
    }

    public Response payAuction(ClientHandler client, Request request) {
        return settlementProcessor.payAuction(client, request);
    }

    public Response confirmAuctionDelivery(ClientHandler client, Request request) {
        return settlementProcessor.confirmAuctionDelivery(client, request);
    }

    public Response resolveAuction(ClientHandler client, Request request) {
        return settlementProcessor.resolveAuction(client, request);
    }
}
