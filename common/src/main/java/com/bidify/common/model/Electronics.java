package com.bidify.common.model;

/**
 * Electronics - Item category for electronic items
 */
public class Electronics extends Item {
    private static final long serialVersionUID = 1L;
    
    private String brand;
    private String model;
    private int yearManufactured;
    private boolean isRefurbished; // Đã tái chế/sửa chữa hay chưa?
    private String warranty; // bảo hành
    
    public Electronics() {
        super();
    }
    
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
        return isRefurbished;
    }
    
    public void setRefurbished(boolean refurbished) {
        isRefurbished = refurbished;
    }
    
    public String getWarranty() {
        return warranty;
    }
    
    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }
}
