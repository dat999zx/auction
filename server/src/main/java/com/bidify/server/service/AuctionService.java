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
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
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
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.service.auction.AuctionAudienceService;
import com.bidify.server.service.auction.AuctionBidProcessor;
import com.bidify.server.service.auction.AuctionCascadeService;
import com.bidify.server.service.auction.AuctionDtoAssembler;
import com.bidify.server.service.auction.AuctionQueryService;
import com.bidify.server.service.auction.AuctionRealtimePublisher;
import com.bidify.server.service.auction.AuctionSettlementProcessor;
import com.bidify.server.utility.ServiceUtil;

public class AuctionService {
    private static final Logger logger = LoggerFactory.getLogger(AuctionService.class);
    private static final AuctionService instance = new AuctionService();

    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    private final BidDao bidDao = BidDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();

    private final AuctionRealtimePublisher realtimePublisher = new AuctionRealtimePublisher();
    private final AuctionDtoAssembler auctionDtoAssembler = new AuctionDtoAssembler();

    private final AuctionAudienceService audienceService =
            new AuctionAudienceService(auctionDtoAssembler, realtimePublisher);
    private final AuctionQueryService queryService =
            new AuctionQueryService(auctionDao, auctionDtoAssembler);
    private final AuctionCascadeService cascadeService =
            new AuctionCascadeService(auctionDao, itemDao, bidDao, transactionDao, userDao, auctionDtoAssembler, realtimePublisher);

    private final AuctionSettlementProcessor settlementProcessor =
            new AuctionSettlementProcessor(auctionDao, transactionDao, auctionDtoAssembler, realtimePublisher);
    private final AuctionBidProcessor bidProcessor =
            new AuctionBidProcessor(auctionDao, bidDao, userDao, auctionDtoAssembler, realtimePublisher);

    private AuctionService() {}

    public static AuctionService getInstance() { return instance; }

    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.JOIN_AUCTION, audienceService::join);
        router.register(RequestType.LEAVE_AUCTION, audienceService::leave);
        router.register(RequestType.CREATE_AUCTION, this::create);
        router.register(RequestType.UPDATE_AUCTION, this::update);
        router.register(RequestType.GET_LIVE_AUCTIONS, (client, req) -> queryService.getAllLiveAuctions());
        router.register(RequestType.GET_UPCOMING_AUCTIONS, (client, req) -> queryService.getAllUpcomingAuctions());
        router.register(RequestType.GET_AUCTION_DETAIL, queryService::getDetail);
        router.register(RequestType.DELETE_AUCTION, this::delete);
        router.register(RequestType.PLACE_BID, bidProcessor::placeBid);
        router.register(RequestType.SET_AUTO_BID, bidProcessor::setAutoBid);
        router.register(RequestType.DISABLE_AUTO_BID, bidProcessor::disableAutoBid);
        router.register(RequestType.SEARCH_AUCTIONS, (client, req) -> queryService.search(req));
        router.register(RequestType.PAY_AUCTION, settlementProcessor::payAuction);
        router.register(RequestType.CONFIRM_AUCTION_DELIVERY, settlementProcessor::confirmAuctionDelivery);
        router.register(RequestType.RESOLVE_AUCTION, settlementProcessor::resolveAuction);
        router.register(RequestType.GET_USER_SETTLEMENTS, queryService::getUserSettlements);
        router.register(RequestType.GET_ADMIN_AUCTIONS, queryService::getAdminAuctions);
        router.register(RequestType.GET_MY_AUCTIONS, queryService::getMyAuctions);
    }

    public void loadToRuntime(){
        List<Auction> runtimeAuctions = new ArrayList<>();
        runtimeAuctions.addAll(auctionDao.findByStatus(AuctionStatus.UPCOMING));
        runtimeAuctions.addAll(auctionDao.findByStatus(AuctionStatus.ACTIVE));

        for (Auction auction : runtimeAuctions)
            RealtimeDatabase.addRuntimeAuction(auction);
    }

    // Public wrapper methods for backward compatibility and test stability
    public Response search(Request request) {
        return queryService.search(request);
    }

    public Response getDetail(ClientHandler client, Request request) {
        return queryService.getDetail(client, request);
    }

    public Response join(ClientHandler client, Request request) {
        return audienceService.join(client, request);
    }

    public Response leave(ClientHandler client, Request request) {
        return audienceService.leave(client, request);
    }

    public void publishLiveAudienceUpdate(String auctionId) {
        audienceService.publishLiveAudienceUpdate(auctionId);
    }

    public void deleteAuctionCascade(Auction auction, boolean restoreLinkedItemToSeller) {
        cascadeService.deleteAuctionCascade(auction, restoreLinkedItemToSeller);
    }

    public Response getAllLiveAuctions() {
        return queryService.getAllLiveAuctions();
    }

    public Response getAllUpcomingAuctions() {
        return queryService.getAllUpcomingAuctions();
    }

    public Response getUserSettlements(ClientHandler client, Request request) {
        return queryService.getUserSettlements(client, request);
    }

    public Response getMyAuctions(ClientHandler client, Request request) {
        return queryService.getMyAuctions(client, request);
    }

    public Response getAdminAuctions(ClientHandler client, Request request) {
        return queryService.getAdminAuctions(client, request);
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
                cascadeService.deleteAuctionCascade(auction, true);
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

    private void cancelActiveAuction(Auction auction) throws DatabaseException {
        if (auction == null)
            throw new AuctionException("Auction not found");
        if (auction.getStatus() != AuctionStatus.ACTIVE)
            throw new AuctionException("Can only cancel active auctions");

        cascadeService.releaseCurrentLeaderLock(auction);
        cascadeService.updateAuctionItemState(auction, auction.getSellerUsername(), ItemStatus.AVAILABLE);

        auction.setStatus(AuctionStatus.CANCELED);
        auctionDao.save(auction);

        publishAuctionUpdate(auction, "Auction canceled");
        RealtimeDatabase.removeRuntimeAuction(auction.getId());
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

    public void settleAuction(Auction auction) {
        String sellerUsername = auction.getSellerUsername();
        String winnerUsername = auction.getCurrentBidderUsername();

        if (winnerUsername != null) {
            auction.setStatus(AuctionStatus.AWAITING_PAYMENT);
            auctionDao.save(auction);
            publishAuctionUpdate(auction, "Auction ended, awaiting payment from winner");
        }
        else {
            cascadeService.updateAuctionItemState(auction, sellerUsername, ItemStatus.AVAILABLE);
            auction.setStatus(AuctionStatus.CANCELED);
            auctionDao.save(auction);
        }

        logger.info("auction settled: {} - {} (status: {})", auction.getAuctionName(), auction.getId(), auction.getStatus());
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
