package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.utility.IdGenerator;

public class Bid {
    private final String id = IdGenerator.genBidId();
    private Auction auction;
    private String bidderUsername;
    private double amount;
    private LocalDateTime placeAt;

    public Bid(){};

    public Bid(Auction auction, String bidderUsername, double amount){
        this.auction = auction;
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.placeAt = LocalDateTime.now();
    }

    public String getId() { return id; }

    public Auction getItem() { return auction; }
    public void setItem(Auction item) { this.auction = item; }

    public String getBidderUserName() { return bidderUsername; }
    public void setBidderUsername(String bidderUsername) { this.bidderUsername = bidderUsername; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getPlaceAt() { return placeAt; }
    public void setPlacedAt(LocalDateTime time) { this.placeAt = time; }
}
