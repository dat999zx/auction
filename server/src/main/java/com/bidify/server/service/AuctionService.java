package com.bidify.server.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionResolutionAction;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.ConfirmDeliveryRequest;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.DeleteAuctionRequest;
import com.bidify.common.model.DisableAutoBidRequest;
import com.bidify.common.model.Event;
import com.bidify.common.model.GetAuctionDetailRequest;
import com.bidify.common.model.JoinAuctionRequest;
import com.bidify.common.model.LeaveAuctionRequest;
import com.bidify.common.model.PayAuctionRequest;
import com.bidify.common.model.PlaceBidRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.ResolveAuctionRequest;
import com.bidify.common.model.Response;
import com.bidify.common.model.SearchAuctionRequest;
import com.bidify.common.model.SetAutoBidRequest;
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
import com.bidify.server.model.AutoBid;
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

    // dùng để tạo một đối tượng AuctionService
    private AuctionService() {}

    // dùng để lấy đối tượng Singleton
    public static AuctionService getInstance() { return instance; }

    // dùng để khởi tạo
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
        router.register(RequestType.PLACE_BID, this::placeBid);
        router.register(RequestType.SET_AUTO_BID, this::setAutoBid);
        router.register(RequestType.DISABLE_AUTO_BID, this::disableAutoBid);
        router.register(RequestType.SEARCH_AUCTIONS, (client, req) -> search(req));
        router.register(RequestType.PAY_AUCTION, this::payAuction);
        router.register(RequestType.CONFIRM_AUCTION_DELIVERY, this::confirmAuctionDelivery);
        router.register(RequestType.RESOLVE_AUCTION, this::resolveAuction);
        router.register(RequestType.GET_USER_SETTLEMENTS, this::getUserSettlements);
    }

    // load runtime auctions trong sql lên ram, chỉ gọi 1 lần khi server khởi chạy
    // dùng để tải chuyển thành runtime
    public void loadToRuntime(){
        List<Auction> runtimeAuctions = new ArrayList<>();
        runtimeAuctions.addAll(auctionDao.findByStatus(AuctionStatus.UPCOMING));
        runtimeAuctions.addAll(auctionDao.findByStatus(AuctionStatus.ACTIVE));

        for (Auction auction : runtimeAuctions)
            RealtimeDatabase.addRuntimeAuction(auction);
    }

    // dùng để tìm kiếm
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

    // dùng để tạo
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
            // dùng để kiểm tra tính hợp lệ đấu giá thời gian
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

    // dùng để cập nhật
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
            // dùng để kiểm tra tính hợp lệ đấu giá thời gian
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

    // dùng để lưu all runtime danh sách đấu giá
    public void saveAllRuntimeAuctions(){
        for (Auction auction : RealtimeDatabase.getAllRuntimeAuctions())
            auctionDao.save(auction);
    }

    // dùng để xóa
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
            // dùng để xóa đấu giá cascade
            deleteAuctionCascade(auction, true);

            return new Response(RequestStatus.SUCCESS, "Auction deleted successfully");
        });
    }

    // dùng để lấy chi tiết
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

            AuctionDto auctionDto = toAuctionDto(auction, true);
            if (client != null && client.isInSession()) {
                AutoBid currentUserAutoBid = auction.getAutoBid(client.getCurrentUsername());
                auctionDto.setCurrentUserAutoBidActive(currentUserAutoBid != null);
                auctionDto.setCurrentUserAutoBidMax(currentUserAutoBid == null ? null : currentUserAutoBid.getMaxBid());
            }
            return new Response(RequestStatus.SUCCESS, "Get auction detail successfully", auctionDto);
        });
    }

    // dùng để lấy all live danh sách đấu giá
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

    // dùng để lấy all upcoming danh sách đấu giá
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
    // dùng để lấy thumbnail
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
    // dùng để lấy gallery
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

    // dùng để tham gia
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
            // dùng để phát sự kiện đấu giá cập nhật
            publishAuctionUpdate(auction, "Auction watcher count updated");
            return new Response(RequestStatus.SUCCESS, "Join auction successfully", auctionDto);
        });
    }

    // dùng để rời khỏi
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
                // dùng để phát sự kiện đấu giá cập nhật
                publishAuctionUpdate(auction, "Auction watcher count updated");
            return new Response(RequestStatus.SUCCESS, "Leave auction successfully");
        });
    }

    // dùng để place lượt đặt giá
    public Response placeBid(ClientHandler client, Request request){
        return ServiceUtil.handleRequest(() -> {
            PlaceBidRequest data = JsonUtil.fromMap(request.getData(), PlaceBidRequest.class);
            ServiceUtil.validateRequestData(data);
            User sessionUser = ServiceUtil.requireSessionUser(client);
            ServiceUtil.requireUserRole(sessionUser, "Admin accounts cannot place bids");

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
                synchronized (auction) {
                    Wallet wallet = user.getWallet();

                    if (wallet.getAvailableBalance() < bidAmount)
                        throw new InsufficientBalanceException();

                    String prevBidderUsername = auction.getCurrentBidderUsername();
                    double prevBid = auction.getCurrentBid();
                    if (prevBidderUsername != null && prevBidderUsername.equals(username))
                        throw new AuctionException("You are already the highest bidder");

                    Bid bid = new Bid(auction.getId(), username, bidAmount, false);
                    auction.placeBid(bid);

                    wallet.lockBalance(bidAmount);
                    // dùng để phát sự kiện locked số dư change
                    publishLockedBalanceChange(username, bidAmount);

                    User prevBidder = RealtimeDatabase.getActiveUser(prevBidderUsername);
                    if (prevBidder != null) {
                        prevBidder.getWallet().unlockBalance(prevBid);
                        // dùng để phát sự kiện locked số dư change
                        publishLockedBalanceChange(prevBidderUsername, -prevBid);
                    }

                    bidDao.create(bid);

                    AutoBid existingAutoBid = auction.getAutoBid(username);
                    if (existingAutoBid != null && bidAmount > existingAutoBid.getMaxBid())
                        existingAutoBid.setMaxBid(bidAmount);

                    // dùng để áp dụng auto lượt đặt giá resolution
                    applyAutoBidResolution(auction);

                    auctionDao.save(auction);
                    // dùng để phát sự kiện đấu giá lượt đặt giá sự kiện
                    publishAuctionBidEvent(auction, "New bid placed");

                    logger.info("bid placed: auction {} - {}, user {}: {}$", auction.getAuctionName(), auction.getId(), username, bidAmount);
                }

                return new Response(RequestStatus.SUCCESS, "Place bid successfully");
            }
            finally {
                user.unlock();
            }
        });
    }

    // dùng để thiết lập auto lượt đặt giá
    public Response setAutoBid(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            SetAutoBidRequest data = JsonUtil.fromMap(request.getData(), SetAutoBidRequest.class);
            ServiceUtil.validateRequestData(data);
            User sessionUser = ServiceUtil.requireSessionUser(client);
            ServiceUtil.requireUserRole(sessionUser, "Admin accounts cannot configure auto bid");

            String username = client.getCurrentUsername();
            Auction auction = requireActiveAuction(data.getAuctionId());
            User user = requireActiveUser(username);

            if (auction.getSellerUsername().equals(username))
                throw new AuctionException("You cannot bid on your own auction");

            user.tryLock(5);
            try {
                synchronized (auction) {
                    validateAutoBidRequest(auction, user, data.getMaxBid());

                    AutoBid autoBid = auction.getAutoBid(username);
                    if (autoBid == null) {
                        auction.upsertAutoBid(new AutoBid(auction.getId(), username, data.getMaxBid()));
                    } else {
                        autoBid.setMaxBid(data.getMaxBid());
                    }

                    boolean visibleStateChanged = applyAutoBidResolution(auction);
                    auctionDao.save(auction);
                    if (visibleStateChanged)
                        // dùng để phát sự kiện đấu giá lượt đặt giá sự kiện
                        publishAuctionBidEvent(auction, "New bid placed");
                }
                return new Response(RequestStatus.SUCCESS, "AutoBid saved successfully");
            }
            finally {
                user.unlock();
            }
        });
    }

    // dùng để disable auto lượt đặt giá
    public Response disableAutoBid(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            DisableAutoBidRequest data = JsonUtil.fromMap(request.getData(), DisableAutoBidRequest.class);
            ServiceUtil.validateRequestData(data);
            User sessionUser = ServiceUtil.requireSessionUser(client);
            ServiceUtil.requireUserRole(sessionUser, "Admin accounts cannot disable auto bid");

            Auction auction = requireActiveAuction(data.getAuctionId());
            synchronized (auction) {
                auction.disableAutoBid(client.getCurrentUsername());
            }
            return new Response(RequestStatus.SUCCESS, "AutoBid disabled successfully");
        });
    }

    // dùng để auto lượt đặt giá candidate
    private record AutoBidCandidate(String username, double maxBid, LocalDateTime priorityTime) {}

    // dùng để auto lượt đặt giá resolution
    private record AutoBidResolution(String winnerUsername, double resolvedBid, boolean stateChanged) {}

    // dùng để áp dụng auto lượt đặt giá resolution
    private boolean applyAutoBidResolution(Auction auction) {
        AutoBidResolution resolution = resolveAutoBid(auction);
        if (!resolution.stateChanged())
            return false;

        String previousLeader = auction.getCurrentBidderUsername();
        double previousBid = auction.getCurrentBid();
        String winnerUsername = resolution.winnerUsername();
        double resolvedBid = resolution.resolvedBid();

        User winner = ServiceUtil.getOrLoadUser(winnerUsername);
        Wallet winnerWallet = winner.getWallet();

        if (winnerUsername.equals(previousLeader)) {
            double extraNeeded = resolvedBid - previousBid;
            if (extraNeeded > 0) {
                winnerWallet.lockBalance(extraNeeded);
                // dùng để phát sự kiện locked số dư change
                publishLockedBalanceChange(winnerUsername, extraNeeded);
            }
        } else {
            winnerWallet.lockBalance(resolvedBid);
            // dùng để phát sự kiện locked số dư change
            publishLockedBalanceChange(winnerUsername, resolvedBid);

            if (previousLeader != null) {
                User previousWinner = ServiceUtil.getOrLoadUser(previousLeader);
                previousWinner.getWallet().unlockBalance(previousBid);
                // dùng để phát sự kiện locked số dư change
                publishLockedBalanceChange(previousLeader, -previousBid);
            }
        }

        Bid autoBid = new Bid(auction.getId(), winnerUsername, resolvedBid, true);
        auction.setCurrentBidderUsername(winnerUsername);
        auction.setCurrentBid(resolvedBid);
        auction.getBids().add(autoBid);
        bidDao.create(autoBid);
        return true;
    }

    // dùng để giải quyết auto lượt đặt giá
    private AutoBidResolution resolveAutoBid(Auction auction) {
        List<AutoBidCandidate> candidates = collectAutoBidCandidates(auction);
        if (candidates.isEmpty())
            return new AutoBidResolution(auction.getCurrentBidderUsername(), auction.getCurrentBid(), false);

        candidates.sort(Comparator
                .comparingDouble(AutoBidCandidate::maxBid).reversed()
                .thenComparing(AutoBidCandidate::priorityTime));

        AutoBidCandidate winner = candidates.get(0);
        AutoBidCandidate second = candidates.size() > 1 ? candidates.get(1) : null;

        if (winner.username().equals(auction.getCurrentBidderUsername())
                && (second == null || second.maxBid() <= auction.getCurrentBid())) {
            return new AutoBidResolution(auction.getCurrentBidderUsername(), auction.getCurrentBid(), false);
        }

        double minAllowed = nextMinimumBid(auction);
        double secondHighest = second != null ? second.maxBid() : (auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingPrice());
        double resolvedBid = Math.min(winner.maxBid(), Math.max(minAllowed, secondHighest + auction.getMinIncrement()));

        boolean sameWinner = winner.username().equals(auction.getCurrentBidderUsername());
        boolean sameAmount = Double.compare(resolvedBid, auction.getCurrentBid()) == 0;
        return new AutoBidResolution(winner.username(), resolvedBid, !(sameWinner && sameAmount));
    }

    // dùng để collect auto lượt đặt giá candidates
    private List<AutoBidCandidate> collectAutoBidCandidates(Auction auction) {
        List<AutoBidCandidate> candidates = new ArrayList<>();
        double minAllowed = nextMinimumBid(auction);

        String currentLeader = auction.getCurrentBidderUsername();
        if (currentLeader != null && !currentLeader.isBlank()) {
            double leaderMax = auction.getCurrentBid();
            AutoBid leaderAutoBid = auction.getAutoBid(currentLeader);
            if (leaderAutoBid != null && leaderAutoBid.isEnabled()) {
                double effectiveBudget = getEffectiveBudgetForAuction(currentLeader, auction);
                double effectiveMax = Math.min(leaderAutoBid.getMaxBid(), effectiveBudget);
                if (effectiveMax < leaderAutoBid.getMaxBid())
                    notifyAutoBidInsufficientBalance(currentLeader, auction.getId());
                leaderMax = Math.max(leaderMax, effectiveMax);
            }
            candidates.add(new AutoBidCandidate(currentLeader, leaderMax, LocalDateTime.MIN));
        }

        for (AutoBid autoBid : auction.getAutoBids()) {
            if (!autoBid.isEnabled())
                continue;
            if (autoBid.getUsername().equals(currentLeader))
                continue;

            double effectiveBudget = getEffectiveBudgetForAuction(autoBid.getUsername(), auction);
            double effectiveMax = Math.min(autoBid.getMaxBid(), effectiveBudget);
            if (effectiveMax < minAllowed) {
                if (effectiveMax < autoBid.getMaxBid())
                    notifyAutoBidInsufficientBalance(autoBid.getUsername(), auction.getId());
                continue;
            }

            candidates.add(new AutoBidCandidate(autoBid.getUsername(), effectiveMax, autoBid.getCreatedAt()));
        }

        return candidates;
    }

    // dùng để lấy effective budget cho đấu giá
    private double getEffectiveBudgetForAuction(String username, Auction auction) {
        User user = ServiceUtil.getOrLoadUser(username);
        double effectiveBudget = user.getWallet().getAvailableBalance();
        if (username != null && username.equals(auction.getCurrentBidderUsername()))
            effectiveBudget += auction.getCurrentBid();
        return effectiveBudget;
    }

    // dùng để next minimum lượt đặt giá
    private double nextMinimumBid(Auction auction) {
        double currentReference = auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingPrice();
        return currentReference + auction.getMinIncrement();
    }

    // dùng để kiểm tra tính hợp lệ auto lượt đặt giá yêu cầu
    private void validateAutoBidRequest(Auction auction, User user, double maxBid) {
        ValidationUtil.validatePositiveAmount(maxBid, "AutoBid max");
        double minimumRequired = nextMinimumBid(auction);
        if (maxBid < minimumRequired)
            throw new ValidationException("AutoBid max must be greater than or equal to the current required bid");

        if (user.getUsername().equals(auction.getCurrentBidderUsername()) && maxBid < auction.getCurrentBid())
            throw new ValidationException("New AutoBid max cannot be lower than your current committed leading bid");

        double effectiveBudget = getEffectiveBudgetForAuction(user.getUsername(), auction);
        if (maxBid > effectiveBudget)
            throw new InsufficientBalanceException("AutoBid max exceeds available balance");
    }

    // dùng để bắt buộc phải có active đấu giá
    private Auction requireActiveAuction(String auctionId) {
        ValidationUtil.requiresNonBlank(auctionId, "Auction ID");
        Auction auction = RealtimeDatabase.getLiveAuction(auctionId);
        if (auction == null)
            throw new AuctionException("AutoBid is only available for active auctions");
        return auction;
    }

    // dùng để bắt buộc phải có active người dùng
    private User requireActiveUser(String username) {
        User user = RealtimeDatabase.getActiveUser(username);
        if (user == null)
            throw new AuthException("User not found");
        return user;
    }

    // dùng để thông báo auto lượt đặt giá insufficient số dư
    private void notifyAutoBidInsufficientBalance(String username, String auctionId) {
        ClientHandler userClient = RealtimeDatabase.getUserClient(username);
        if (userClient == null) return;
        userClient.sendEvent(new Event(
                EventType.SERVER_NOTICE,
                "AutoBid could not execute for auction " + auctionId + " due to insufficient available balance"
        ));
    }

    // dùng để phát sự kiện đấu giá lượt đặt giá sự kiện
    private void publishAuctionBidEvent(Auction auction, String message) {
        AuctionDto auctionDto = toAuctionDto(auction, false);
        AuctionChannel auctionChannel = RealtimeDatabase.getAuctionChannel(auction.getId());
        if (auctionChannel != null)
            auctionChannel.publish(new Event(EventType.BID_PLACED, message, auctionDto));
        RealtimeDatabase.getGlobalChannel().publish(new Event(EventType.BID_PLACED, message, auctionDto));
    }

    // dùng để bắt buộc phải có seller
    private void requireSeller(Auction auction, String username, String message) {
        if (auction == null || username == null || !ServiceUtil.isOwnerOrAdmin(auction.getSellerUsername(), username))
            throw new ValidationException(message);
    }

    // dùng để bắt buộc phải có available owned sản phẩm
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

    // dùng để lấy linked đấu giá sản phẩm
    private Item getLinkedAuctionItem(Auction auction) {
        if (auction == null) return null;

        String itemId = auction.getItemId();
        if (itemId == null || itemId.isBlank()) return null;

        return itemDao.findById(itemId);
    }

    // dùng để chuyển thành đấu giá đối tượng truyền tải dữ liệu (DTO)
    public AuctionDto toAuctionDto(Auction auction, boolean includeGallery) {
        return toAuctionDto(auction, getLinkedAuctionItem(auction), includeGallery);
    }

    // dùng để chuyển thành đấu giá đối tượng truyền tải dữ liệu (DTO)
    public AuctionDto toAuctionDto(Auction auction, Item item, boolean includeGallery) {
        List<String> gallery = includeGallery ? getGallery(item) : null;
        AuctionDto dto = AuctionMapper.toDto(auction, item, getThumbnail(item), gallery);
        dto.setWatcherCount(resolveWatcherCount(auction));
        dto.setActiveBidderCount(resolveActiveBidderCount(auction));
        return dto;
    }

    // dùng để giải quyết watcher count
    private int resolveWatcherCount(Auction auction) {
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE)
            return 0;

        AuctionChannel channel = RealtimeDatabase.getAuctionChannel(auction.getId());
        return channel == null ? 0 : channel.getObserverCount();
    }

    // dùng để giải quyết active bidder count
    private int resolveActiveBidderCount(Auction auction) {
        if (auction == null || auction.getBids() == null || auction.getBids().isEmpty())
            return 0;

        Set<String> bidders = new LinkedHashSet<>();
        for (Bid bid : auction.getBids()) {
            if (bid == null || bid.getBidderUsername() == null || bid.getBidderUsername().isBlank())
                continue;
            bidders.add(bid.getBidderUsername());
        }
        return bidders.size();
    }

    // dùng để phát sự kiện đấu giá cập nhật
    private void publishAuctionUpdate(Auction auction, String message) {
        if (auction == null) return;

        AuctionDto auctionDto = toAuctionDto(auction, false);
        AuctionChannel auctionChannel = RealtimeDatabase.getAuctionChannel(auction.getId());
        Event event = new Event(EventType.AUCTION_UPDATED, message, auctionDto);
        if (auctionChannel != null)
            auctionChannel.publish(event);
        RealtimeDatabase.getGlobalChannel().publish(event);
    }

    // dùng để phát sự kiện live audience cập nhật
    public void publishLiveAudienceUpdate(String auctionId) {
        if (auctionId == null || auctionId.isBlank())
            return;

        Auction auction = RealtimeDatabase.getLiveAuction(auctionId);
        if (auction == null)
            return;

        // dùng để phát sự kiện đấu giá cập nhật
        publishAuctionUpdate(auction, "Auction watcher count updated");
    }

    // dùng để xóa đấu giá cascade
    public void deleteAuctionCascade(Auction auction, boolean restoreLinkedItemToSeller) {
        if (auction == null)
            return;

        // dùng để release current leader khóa đồng bộ
        releaseCurrentLeaderLock(auction);

        if (restoreLinkedItemToSeller && auction.getItemId() != null && !auction.getItemId().isBlank())
            itemDao.updateAvailabilityStatus(auction.getItemId(), ItemStatus.AVAILABLE);

        bidDao.deleteByAuctionId(auction.getId());
        transactionDao.deleteByAuctionId(auction.getId());
        RealtimeDatabase.removeRuntimeAuction(auction.getId());
        auctionDao.deleteById(auction.getId());
        RealtimeDatabase.getGlobalChannel().publish(
            new Event(EventType.AUCTION_DELETED, "Auction deleted", auction.getId())
        );
    }

    // dùng để release current leader khóa đồng bộ
    private void releaseCurrentLeaderLock(Auction auction) {
        String currentBidderUsername = auction.getCurrentBidderUsername();
        double currentBid = auction.getCurrentBid();

        if (currentBidderUsername == null || currentBid <= 0)
            return;

        User activeBidder = RealtimeDatabase.getActiveUser(currentBidderUsername);
        if (activeBidder == null)
            return;

        Wallet wallet = activeBidder.getWallet();
        if (wallet == null || wallet.getLockedBalance() < currentBid)
            return;

        wallet.unlockBalance(currentBid);
        userDao.save(activeBidder, false);

        ClientHandler clientHandler = RealtimeDatabase.getUserClient(currentBidderUsername);
        if (clientHandler != null) {
            clientHandler.sendEvent(new Event(
                EventType.LOCKED_BALANCE_CHANGED,
                "Locked balance changed: -" + currentBid
            ));
        }
    }

    // dùng để cập nhật đấu giá sản phẩm state
    private void updateAuctionItemState(Auction auction, String ownerUsername, ItemStatus status) {
        Item item = getLinkedAuctionItem(auction);
        if (item == null) return;

        item.setOwnerUsername(ownerUsername);
        item.setAvailabilityStatus(status);
        itemDao.save(item);
    }

    // dùng để kiểm tra tính hợp lệ đấu giá cập nhật fields
    private void validateAuctionUpdateFields(String sellerUsername, double startingPrice, double minIncrement) {
        ValidationUtil.requiresNonBlank(sellerUsername, "Seller");
        ValidationUtil.validatePositiveAmount(startingPrice, "Starting price");
        ValidationUtil.validatePositiveAmount(minIncrement, "Min increment");
    }

    // dùng để kiểm tra tính hợp lệ đấu giá thời gian
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

    // dùng để phân tích cú pháp ngày thời gian
    private LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value);
    }

    // dùng để kiểm tra xem runtime đấu giá
    private boolean isRuntimeAuction(Auction auction) {
        if (auction == null) return false;
        AuctionStatus status = auction.getStatus();
        return status == AuctionStatus.UPCOMING || status == AuctionStatus.ACTIVE;
    }

    // chuyển auction từ ACTIVE -> ENDED, chuyển tiền các thứ...
    // dùng để settle đấu giá
    public void settleAuction(Auction auction) {
        String sellerUsername = auction.getSellerUsername();
        String winnerUsername = auction.getCurrentBidderUsername();

        if (winnerUsername != null) {
            auction.setStatus(AuctionStatus.AWAITING_PAYMENT);
            auctionDao.save(auction);
            // dùng để phát sự kiện đấu giá cập nhật
            publishAuctionUpdate(auction, "Auction ended, awaiting payment from winner");
        }
        else {
            // dùng để cập nhật đấu giá sản phẩm state
            updateAuctionItemState(auction, sellerUsername, ItemStatus.AVAILABLE);
            auction.setStatus(AuctionStatus.CANCELED);
            auctionDao.save(auction);
        }

        logger.info("auction settled: {} - {} (status: {})", auction.getAuctionName(), auction.getId(), auction.getStatus());
    }

    // Event cập nhật wallet của User
    // dùng để phát sự kiện số dư change
    private void publishBalanceChange(String username, double diff) {
        ClientHandler userClient = RealtimeDatabase.getUserClient(username);
        if (userClient == null) return;
        Event event = new Event(EventType.WALLET_CHANGED, "Wallet changed: " + diff);
        userClient.sendEvent(event);
    }

    // Event cập nhật lockedBalance của User
    // dùng để phát sự kiện locked số dư change
    private void publishLockedBalanceChange(String username, double diff) {
        ClientHandler userClient = RealtimeDatabase.getUserClient(username);
        if (userClient == null) return;
        Event event = new Event(EventType.LOCKED_BALANCE_CHANGED, "Locked balance changed: " + diff);
        userClient.sendEvent(event);
    }

    public Response getUserSettlements(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User user = ServiceUtil.requireSessionUser(client);
            List<Auction> auctions = auctionDao.findUserSettlements(user.getUsername());
            List<AuctionDto> result = new ArrayList<>();
            for (Auction auction : auctions) {
                result.add(toAuctionDto(auction, false));
            }
            return new Response(RequestStatus.SUCCESS, "Get user settlements successfully", result);
        });
    }

    public Response payAuction(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User winner = ServiceUtil.requireSessionUser(client);
            PayAuctionRequest data = JsonUtil.fromMap(request.getData(), PayAuctionRequest.class);
            ServiceUtil.validateRequestData(data);

            Auction auction = loadAuctionForSettlement(data.getAuctionId());
            synchronized (auction) {
                if (auction.getStatus() != AuctionStatus.AWAITING_PAYMENT) {
                    throw new AuctionException("Auction is not awaiting payment");
                }
                if (!winner.getUsername().equals(auction.getCurrentBidderUsername())) {
                    throw new AuctionException("Only the winning bidder can pay for this auction");
                }
                double finalBid = auction.getCurrentBid();
                if (finalBid <= 0) {
                    throw new AuctionException("Invalid final bid amount");
                }

                winner.tryLock(5);
                try {
                    winner.getWallet().payWinAuction(finalBid);
                    userDao.save(winner, false);
                } finally {
                    winner.unlock();
                }

                transactionDao.create(new Transaction(winner.getUsername(), TransactionType.AUCTION_PAY, finalBid, auction.getId()));

                auction.setStatus(AuctionStatus.AWAITING_DELIVERY);
                auctionDao.save(auction);

                publishBalanceChange(winner.getUsername(), -finalBid);
                publishLockedBalanceChange(winner.getUsername(), -finalBid);
                publishAuctionUpdate(auction, "Winner has paid for the auction");

                return new Response(RequestStatus.SUCCESS, "Paid for auction successfully", toAuctionDto(auction, false));
            }
        });
    }

    public Response confirmAuctionDelivery(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User sessionUser = ServiceUtil.requireSessionUser(client);
            ConfirmDeliveryRequest data = JsonUtil.fromMap(request.getData(), ConfirmDeliveryRequest.class);
            ServiceUtil.validateRequestData(data);

            Auction auction = loadAuctionForSettlement(data.getAuctionId());
            synchronized (auction) {
                if (auction.getStatus() != AuctionStatus.AWAITING_DELIVERY) {
                    throw new AuctionException("Auction is not awaiting delivery");
                }
                if (!sessionUser.getUsername().equals(auction.getSellerUsername()) && !ServiceUtil.isAdmin(sessionUser)) {
                    throw new AuctionException("Only the seller or an admin can confirm delivery");
                }

                completePaidAuction(auction);

                return new Response(RequestStatus.SUCCESS, "Delivery confirmed successfully", toAuctionDto(auction, false));
            }
        });
    }

    public Response resolveAuction(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);
            ResolveAuctionRequest data = JsonUtil.fromMap(request.getData(), ResolveAuctionRequest.class);
            ServiceUtil.validateRequestData(data);
            if (data.getAction() == null) {
                throw new ValidationException("Action cannot be null");
            }

            Auction auction = loadAuctionForSettlement(data.getAuctionId());
            synchronized (auction) {
                if (data.getAction() == AuctionResolutionAction.COMPLETE) {
                    if (auction.getStatus() != AuctionStatus.AWAITING_DELIVERY) {
                        throw new AuctionException("Only auctions awaiting delivery can be resolved as complete");
                    }
                    completePaidAuction(auction);
                } else if (data.getAction() == AuctionResolutionAction.CANCEL) {
                    if (auction.getStatus() == AuctionStatus.AWAITING_PAYMENT) {
                        cancelAwaitingPaymentAuction(auction);
                    } else if (auction.getStatus() == AuctionStatus.AWAITING_DELIVERY) {
                        cancelAwaitingDeliveryAuction(auction);
                    } else {
                        throw new AuctionException("Cannot cancel auction in status: " + auction.getStatus());
                    }
                } else {
                    throw new AuctionException("Unknown resolution action");
                }

                return new Response(RequestStatus.SUCCESS, "Auction resolved successfully", toAuctionDto(auction, false));
            }
        });
    }

    private Auction loadAuctionForSettlement(String auctionId) throws DatabaseException {
        ValidationUtil.requiresNonBlank(auctionId, "Auction ID");
        Auction auction = auctionDao.findById(auctionId);
        if (auction == null) {
            throw new AuctionException("Auction not found");
        }
        return auction;
    }

    private void completePaidAuction(Auction auction) throws DatabaseException {
        String sellerUsername = auction.getSellerUsername();
        String winnerUsername = auction.getCurrentBidderUsername();
        double finalBid = auction.getCurrentBid();

        User seller = ServiceUtil.getOrLoadUser(sellerUsername);
        seller.getWallet().deposit(finalBid);
        userDao.save(seller, false);

        transactionDao.create(new Transaction(sellerUsername, TransactionType.AUCTION_PROFIT, finalBid, auction.getId()));

        updateAuctionItemState(auction, winnerUsername, ItemStatus.AVAILABLE);

        auction.setStatus(AuctionStatus.COMPLETED);
        auctionDao.save(auction);

        publishBalanceChange(sellerUsername, finalBid);
        publishAuctionUpdate(auction, "Auction completed successfully");
    }

    private void cancelAwaitingPaymentAuction(Auction auction) throws DatabaseException {
        String winnerUsername = auction.getCurrentBidderUsername();
        double finalBid = auction.getCurrentBid();

        User winner = ServiceUtil.getOrLoadUser(winnerUsername);
        winner.getWallet().unlockBalance(finalBid);
        userDao.save(winner, false);

        updateAuctionItemState(auction, auction.getSellerUsername(), ItemStatus.AVAILABLE);

        auction.setStatus(AuctionStatus.CANCELED);
        auctionDao.save(auction);

        publishLockedBalanceChange(winnerUsername, -finalBid);
        publishAuctionUpdate(auction, "Auction canceled by admin");
    }

    private void cancelAwaitingDeliveryAuction(Auction auction) throws DatabaseException {
        String winnerUsername = auction.getCurrentBidderUsername();
        double finalBid = auction.getCurrentBid();

        User winner = ServiceUtil.getOrLoadUser(winnerUsername);
        winner.getWallet().deposit(finalBid);
        userDao.save(winner, false);

        transactionDao.create(new Transaction(winnerUsername, TransactionType.AUCTION_REFUND, finalBid, auction.getId()));

        updateAuctionItemState(auction, auction.getSellerUsername(), ItemStatus.AVAILABLE);

        auction.setStatus(AuctionStatus.CANCELED);
        auctionDao.save(auction);

        publishBalanceChange(winnerUsername, finalBid);
        publishAuctionUpdate(auction, "Auction canceled and refunded by admin");
    }
}
