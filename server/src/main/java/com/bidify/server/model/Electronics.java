package com.bidify.server.model;

public class Electronics extends Item {
    private String brand;
    private String model;
    private int yearManufactured;
    private boolean refurbished;
    private String warranty;
    
    public Electronics(String name, String description, double startingPrice, String sellerId) {
        super(name, description, startingPrice, sellerId);
    }

    @Override
    public String getItemType() {
        return "ELECTRONICS";
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getYearManufactured() {
        return yearManufactured;
    }

    public void setYearManufactured(int yearManufactured) {
        this.yearManufactured = yearManufactured;
    }

    public boolean isRefurbished() {
        return refurbished;
    }

    public void setRefurbished(boolean refurbished) {
        this.refurbished = refurbished;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }
}

