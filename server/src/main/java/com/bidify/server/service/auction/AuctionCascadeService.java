package com.bidify.server.service.auction;

import com.bidify.common.enums.EventType;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.model.Event;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.model.Wallet;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;

public class AuctionCascadeService {
    private final AuctionDao auctionDao;
    private final ItemDao itemDao;
    private final BidDao bidDao;
    private final TransactionDao transactionDao;
    private final UserDao userDao;
    private final AuctionDtoAssembler dtoAssembler;
    private final AuctionRealtimePublisher realtimePublisher;

    public AuctionCascadeService(
            AuctionDao auctionDao,
            ItemDao itemDao,
            BidDao bidDao,
            TransactionDao transactionDao,
            UserDao userDao,
            AuctionDtoAssembler dtoAssembler,
            AuctionRealtimePublisher realtimePublisher) {
        this.auctionDao = auctionDao;
        this.itemDao = itemDao;
        this.bidDao = bidDao;
        this.transactionDao = transactionDao;
        this.userDao = userDao;
        this.dtoAssembler = dtoAssembler;
        this.realtimePublisher = realtimePublisher;
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

    public void releaseCurrentLeaderLock(Auction auction) {
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

    public void updateAuctionItemState(Auction auction, String ownerUsername, ItemStatus status) {
        Item item = dtoAssembler.getLinkedAuctionItem(auction);
        if (item == null) return;

        item.setOwnerUsername(ownerUsername);
        item.setAvailabilityStatus(status);
        itemDao.save(item);
    }
}
