package com.bidify.server.service.auction;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionResolutionAction;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.ConfirmDeliveryRequest;
import com.bidify.common.model.PayAuctionRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Item;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;
import com.bidify.common.model.ResolveAuctionRequest;

public class AuctionSettlementProcessor {
    private final AuctionDao auctionDao;
    private final TransactionDao transactionDao;
    private final AuctionDtoAssembler dtoAssembler;
    private final AuctionRealtimePublisher realtimePublisher;
    private final ItemDao itemDao = ItemDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    public AuctionSettlementProcessor(
            AuctionDao auctionDao,
            TransactionDao transactionDao,
            AuctionDtoAssembler dtoAssembler,
            AuctionRealtimePublisher realtimePublisher) {
        this.auctionDao = auctionDao;
        this.transactionDao = transactionDao;
        this.dtoAssembler = dtoAssembler;
        this.realtimePublisher = realtimePublisher;
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

                realtimePublisher.publishBalanceChange(winner.getUsername(), -finalBid);
                realtimePublisher.publishLockedBalanceChange(winner.getUsername(), -finalBid);
                realtimePublisher.publishAuctionUpdate(auction, toDto(auction), "Winner has paid for the auction");

                return new Response(RequestStatus.SUCCESS, "Paid for auction successfully", toDto(auction));
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

                return new Response(RequestStatus.SUCCESS, "Delivery confirmed successfully", toDto(auction));
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

                return new Response(RequestStatus.SUCCESS, "Auction resolved successfully", toDto(auction));
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

        realtimePublisher.publishBalanceChange(sellerUsername, finalBid);
        realtimePublisher.publishAuctionUpdate(auction, toDto(auction), "Auction completed successfully");
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

        realtimePublisher.publishLockedBalanceChange(winnerUsername, -finalBid);
        realtimePublisher.publishAuctionUpdate(auction, toDto(auction), "Auction canceled by admin");
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

        realtimePublisher.publishBalanceChange(winnerUsername, finalBid);
        realtimePublisher.publishAuctionUpdate(auction, toDto(auction), "Auction canceled and refunded by admin");
    }

    private void updateAuctionItemState(Auction auction, String ownerUsername, ItemStatus status) {
        Item item = dtoAssembler.getLinkedAuctionItem(auction);
        if (item == null) return;
        item.setOwnerUsername(ownerUsername);
        item.setAvailabilityStatus(status);
        itemDao.save(item);
    }

    private AuctionDto toDto(Auction auction) {
        return dtoAssembler.toAuctionDto(auction, false);
    }
}
