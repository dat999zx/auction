package com.bidify.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Bidder - User who participates in auctions by bidding
 */
public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    
    private double accountBalance; // số dư tk
    private int totalBidsPlaced; // tổng số lần đạo giá
    private int auctionsWon; // thắng đấu giá
    private List<String> watchedAuctionIds; // Danh sách đấu giá theo dõi

    
    public Bidder() {
        super();
        this.accountBalance = 0.0;
        this.totalBidsPlaced = 0;
        this.auctionsWon = 0;
        this.watchedAuctionIds = new ArrayList<>();
    }
    
    public Bidder(String username, String email, String password, String fullName) {
        super(username, email, password, fullName);
        this.accountBalance = 0.0;
        this.totalBidsPlaced = 0;
        this.auctionsWon = 0;
        this.watchedAuctionIds = new ArrayList<>();
    }
    
    @Override
    public String getUserType() {
        return "BIDDER";
    }
    
    public double getAccountBalance() {
        return accountBalance;
    }
    
    public void setAccountBalance(double accountBalance) {
        this.accountBalance = accountBalance;
    }
    
    public void addBalance(double amount) {
        this.accountBalance += amount;
    }
    
    public void deductBalance(double amount) {
        if (accountBalance >= amount) {
            this.accountBalance -= amount;
        }
    }
    
    public int getTotalBidsPlaced() {
        return totalBidsPlaced;
    }
    
    public void setTotalBidsPlaced(int totalBidsPlaced) {
        this.totalBidsPlaced = totalBidsPlaced;
    }
    
    public int getAuctionsWon() {
        return auctionsWon;
    }
    
    public void setAuctionsWon(int auctionsWon) {
        this.auctionsWon = auctionsWon;
    }
    
    public List<String> getWatchedAuctionIds() {
        return watchedAuctionIds;
    }
    
    public void addWatchedAuction(String auctionId) {
        if (!watchedAuctionIds.contains(auctionId)) {
            watchedAuctionIds.add(auctionId);
        }
    }
    
    public void removeWatchedAuction(String auctionId) {
        watchedAuctionIds.remove(auctionId);
    }
}
