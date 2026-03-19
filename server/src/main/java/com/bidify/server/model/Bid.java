package com.bidify.server.model;

import com.bidify.server.utility.IdGenerator;
import java.time.LocalDateTime;

public class Bid {
    private final String id = IdGenerator.genBidId();
    private AuctionItem item;
    private User bidder;
    private double amount;
    private LocalDateTime placeAt;

    public Bid(){};

    public Bid(AuctionItem item, User bidder, double amount){
        this.item = item;
        this.bidder = bidder;
        this.amount = amount;
        this.placeAt = LocalDateTime.now();
    }

    public AuctionItem getItem() { return item; }
    public void setItem(AuctionItem item) { this.item = item; }

    public User getBidder() { return bidder; }
    public void setBidder(User bidder) { this.bidder = bidder; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getPlaceAt() { return placeAt; }
    public void setPlacedAt(LocalDateTime time) { this.placeAt = time; }
}
