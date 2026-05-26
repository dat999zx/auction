package com.bidify.server.service.auction;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.model.Event;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.model.Auction;
import com.bidify.server.model.runtime.AuctionChannel;
import com.bidify.server.network.ClientHandler;

public class AuctionRealtimePublisher {

    public void publishAuctionUpdate(Auction auction, AuctionDto dto, String message) {
        if (auction == null) return;
        AuctionChannel auctionChannel = RealtimeDatabase.getAuctionChannel(auction.getId());
        Event event = new Event(EventType.AUCTION_UPDATED, message, dto);
        if (auctionChannel != null)
            auctionChannel.publish(event);
        RealtimeDatabase.getGlobalChannel().publish(event);
    }

    public void publishAuctionBidEvent(Auction auction, AuctionDto dto, String message) {
        if (auction == null) return;
        AuctionChannel auctionChannel = RealtimeDatabase.getAuctionChannel(auction.getId());
        if (auctionChannel != null)
            auctionChannel.publish(new Event(EventType.BID_PLACED, message, dto));
        RealtimeDatabase.getGlobalChannel().publish(new Event(EventType.BID_PLACED, message, dto));
    }

    public void publishAuctionDeleted(Auction auction) {
        if (auction == null) return;
        RealtimeDatabase.getGlobalChannel().publish(
            new Event(EventType.AUCTION_DELETED, "Auction deleted", auction.getId())
        );
    }

    public void publishBalanceChange(String username, double diff) {
        ClientHandler userClient = RealtimeDatabase.getUserClient(username);
        if (userClient == null) return;
        userClient.sendEvent(new Event(EventType.WALLET_CHANGED, "Wallet changed: " + diff));
    }

    public void publishLockedBalanceChange(String username, double diff) {
        ClientHandler userClient = RealtimeDatabase.getUserClient(username);
        if (userClient == null) return;
        userClient.sendEvent(new Event(EventType.LOCKED_BALANCE_CHANGED, "Locked balance changed: " + diff));
    }
}
