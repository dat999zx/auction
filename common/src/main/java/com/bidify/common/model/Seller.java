package com.bidify.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Seller - User who creates auctions and sells items
 */
public class Seller extends User {
    private static final long serialVersionUID = 1L;
    
    private double totalRevenue; // tổng doanh thu
    private int totalAuctionsCreated; // tổng Số Cuộc Đấu Giá Được Tạo
    private int successfulSales;
    private List<String> inventoryItemIds;
    private boolean isVerified; // đã xác minh danh tính chưa?
    
    public Seller() {
        super();
        this.totalRevenue = 0.0;
        this.totalAuctionsCreated = 0;
        this.successfulSales = 0;
        this.inventoryItemIds = new ArrayList<>();
        this.isVerified = false;
    }
    
    public Seller(String username, String email, String password, String fullName) {
        super(username, email, password, fullName);
        this.totalRevenue = 0.0;
        this.totalAuctionsCreated = 0;
        this.successfulSales = 0;
        this.inventoryItemIds = new ArrayList<>();
        this.isVerified = false;
    }
    
    @Override
    public String getUserType() {
        return "SELLER";
    }
    
    public double getTotalRevenue() {
        return totalRevenue;
    }
    
    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
    
    public void addRevenue(double amount) {
        this.totalRevenue += amount;
    }
    
    public int getTotalAuctionsCreated() {
        return totalAuctionsCreated;
    }
    
    public void setTotalAuctionsCreated(int totalAuctionsCreated) {
        this.totalAuctionsCreated = totalAuctionsCreated;
    }
    
    public int getSuccessfulSales() {
        return successfulSales;
    }
    
    public void setSuccessfulSales(int successfulSales) {
        this.successfulSales = successfulSales;
    }
    
    public List<String> getInventoryItemIds() {
        return inventoryItemIds;
    }
    
    public void addInventoryItem(String itemId) {
        if (!inventoryItemIds.contains(itemId)) {
            inventoryItemIds.add(itemId);
        }
    }
    
    public void removeInventoryItem(String itemId) {
        inventoryItemIds.remove(itemId);
    }
    
    public boolean isVerified() {
        return isVerified;
    }
    
    public void setVerified(boolean verified) {
        isVerified = verified;
    }
}
