package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.utility.IdGenerator;

public class Bid {
    private final String id = IdGenerator.genBidId();
    private Auction item;
    private User bidder;
    private double amount;
    private LocalDateTime placeAt;

    public Bid(){};

    public Bid(Auction item, User bidder, double amount){
        this.item = item;
        this.bidder = bidder;
        this.amount = amount;
        this.placeAt = LocalDateTime.now();
    }

    public String getId() { return id; }

    public Auction getItem() { return item; }
    public void setItem(Auction item) { this.item = item; }

    public User getBidder() { return bidder; }
    public void setBidder(User bidder) { this.bidder = bidder; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getPlaceAt() { return placeAt; }
    public void setPlacedAt(LocalDateTime time) { this.placeAt = time; }
}
