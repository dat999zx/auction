package com.bidify.server.service.auction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.DisableAutoBidRequest;
import com.bidify.common.model.Event;
import com.bidify.common.model.PlaceBidRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.SetAutoBidRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.exception.InsufficientBalanceException;
import com.bidify.server.model.Auction;
import com.bidify.server.model.AutoBid;
import com.bidify.server.model.Bid;
import com.bidify.server.model.User;
import com.bidify.server.model.Wallet;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;

public class AuctionBidProcessor {
    private final AuctionDao auctionDao;
    private final BidDao bidDao;
    private final AuctionDtoAssembler dtoAssembler;
    private final AuctionRealtimePublisher realtimePublisher;

    public AuctionBidProcessor(
            AuctionDao auctionDao,
            BidDao bidDao,
            UserDao userDao,
            AuctionDtoAssembler dtoAssembler,
            AuctionRealtimePublisher realtimePublisher) {
        this.auctionDao = auctionDao;
        this.bidDao = bidDao;
        this.dtoAssembler = dtoAssembler;
        this.realtimePublisher = realtimePublisher;
    }

    public Response placeBid(ClientHandler client, Request request) {
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

                    double origCurrentBid = auction.getCurrentBid();
                    String origCurrentBidder = auction.getCurrentBidderUsername();
                    LocalDateTime origEndTime = auction.getEndTime();

                    wallet.lockBalance(bidAmount);
                    boolean walletLocked = true;

                    try {
                        Bid bid = new Bid(auction.getId(), username, bidAmount, false);
                        auction.placeBid(bid);

                        try {
                            bidDao.create(bid);
                            auctionDao.save(auction);

                            User prevBidder = RealtimeDatabase.getActiveUser(prevBidderUsername);
                            if (prevBidder != null) {
                                prevBidder.getWallet().unlockBalance(prevBid);
                                realtimePublisher.publishLockedBalanceChange(prevBidderUsername, -prevBid);
                            }

                            AutoBid existingAutoBid = auction.getAutoBid(username);
                            if (existingAutoBid != null && bidAmount > existingAutoBid.getMaxBid())
                                existingAutoBid.setMaxBid(bidAmount);

                            applyAutoBidResolution(auction);

                            realtimePublisher.publishLockedBalanceChange(username, bidAmount);
                            realtimePublisher.publishAuctionBidEvent(auction, dtoAssembler.toAuctionDto(auction, false), "New bid placed");
                        } catch (Exception e) {
                            auction.setCurrentBid(origCurrentBid);
                            auction.setCurrentBidderUsername(origCurrentBidder);
                            auction.setEndTime(origEndTime);
                            auction.removeBid(bid);
                            throw e;
                        }
                    } catch (Exception e) {
                        if (walletLocked)
                            wallet.unlockBalance(bidAmount);
                        throw e;
                    }
                }

                return new Response(RequestStatus.SUCCESS, "Place bid successfully");
            } finally {
                user.unlock();
            }
        });
    }

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
                        realtimePublisher.publishAuctionBidEvent(auction, dtoAssembler.toAuctionDto(auction, false), "New bid placed");
                }
                return new Response(RequestStatus.SUCCESS, "AutoBid saved successfully");
            } finally {
                user.unlock();
            }
        });
    }

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

    private record AutoBidCandidate(String username, double maxBid, LocalDateTime priorityTime) {}

    private record AutoBidResolution(String winnerUsername, double resolvedBid, boolean stateChanged) {}

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

        boolean lockedWinnerExtra = false;
        double winnerExtraAmount = 0;

        boolean lockedWinnerNew = false;
        double winnerNewAmount = 0;

        boolean unlockedPrev = false;
        User previousWinner = null;
        double prevAmount = 0;

        try {
            if (winnerUsername.equals(previousLeader)) {
                double extraNeeded = resolvedBid - previousBid;
                if (extraNeeded > 0) {
                    winnerWallet.lockBalance(extraNeeded);
                    lockedWinnerExtra = true;
                    winnerExtraAmount = extraNeeded;
                    realtimePublisher.publishLockedBalanceChange(winnerUsername, extraNeeded);
                }
            } else {
                winnerWallet.lockBalance(resolvedBid);
                lockedWinnerNew = true;
                winnerNewAmount = resolvedBid;
                realtimePublisher.publishLockedBalanceChange(winnerUsername, resolvedBid);

                if (previousLeader != null) {
                    previousWinner = ServiceUtil.getOrLoadUser(previousLeader);
                    previousWinner.getWallet().unlockBalance(previousBid);
                    unlockedPrev = true;
                    prevAmount = previousBid;
                    realtimePublisher.publishLockedBalanceChange(previousLeader, -previousBid);
                }
            }

            Bid autoBid = new Bid(auction.getId(), winnerUsername, resolvedBid, true);
            double origCurrentBid = auction.getCurrentBid();
            String origCurrentBidder = auction.getCurrentBidderUsername();

            auction.setCurrentBidderUsername(winnerUsername);
            auction.setCurrentBid(resolvedBid);
            auction.addBid(autoBid);

            try {
                bidDao.create(autoBid);
            } catch (Exception e) {
                auction.setCurrentBidderUsername(origCurrentBidder);
                auction.setCurrentBid(origCurrentBid);
                auction.removeBid(autoBid);
                throw e;
            }
        } catch (Exception e) {
            if (lockedWinnerExtra)
                winnerWallet.unlockBalance(winnerExtraAmount);
            if (lockedWinnerNew)
                winnerWallet.unlockBalance(winnerNewAmount);
            if (unlockedPrev && previousWinner != null)
                previousWinner.getWallet().lockBalance(prevAmount);
            throw e;
        }

        return true;
    }

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

    private double getEffectiveBudgetForAuction(String username, Auction auction) {
        User user = ServiceUtil.getOrLoadUser(username);
        double effectiveBudget = user.getWallet().getAvailableBalance();
        if (username != null && username.equals(auction.getCurrentBidderUsername()))
            effectiveBudget += auction.getCurrentBid();
        return effectiveBudget;
    }

    private double nextMinimumBid(Auction auction) {
        double currentReference = auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingPrice();
        return currentReference + auction.getMinIncrement();
    }

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

    private Auction requireActiveAuction(String auctionId) {
        ValidationUtil.requiresNonBlank(auctionId, "Auction ID");
        Auction auction = RealtimeDatabase.getLiveAuction(auctionId);
        if (auction == null)
            throw new AuctionException("AutoBid is only available for active auctions");
        return auction;
    }

    private User requireActiveUser(String username) {
        User user = RealtimeDatabase.getActiveUser(username);
        if (user == null)
            throw new AuthException("User not found");
        return user;
    }

    private void notifyAutoBidInsufficientBalance(String username, String auctionId) {
        ClientHandler userClient = RealtimeDatabase.getUserClient(username);
        if (userClient == null) return;
        userClient.sendEvent(new Event(
                EventType.SERVER_NOTICE,
                "AutoBid could not execute for auction " + auctionId + " due to insufficient available balance"
        ));
    }
}
