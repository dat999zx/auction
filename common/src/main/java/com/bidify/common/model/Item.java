package com.bidify.common.model;

/**
 * Abstract Item class - represents an item being sold in an auction
 */
public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;
    
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double estimatedValue; // giá trị ước tính
    protected String sellerId;
    protected String category; // thể loại
    protected String condition; // tình trạng
    protected String imageUrl; // URL/link ảnh của sản phẩm
    
    public Item() {
        super();
    }
    
    public Item(String name, String description, double startingPrice, String sellerId) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.sellerId = sellerId;
    }
    
    // Abstract method
    public abstract String getItemType();
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public double getStartingPrice() {
        return startingPrice;
    }
    
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }
    
    public double getEstimatedValue() {
        return estimatedValue;
    }
    
    public void setEstimatedValue(double estimatedValue) {
        this.estimatedValue = estimatedValue;
    }
    
    public String getSellerId() {
        return sellerId;
    }
    
    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public void setCondition(String condition) {
        this.condition = condition;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
