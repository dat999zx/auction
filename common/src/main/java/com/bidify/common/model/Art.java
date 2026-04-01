package com.bidify.common.model;

/**
 * Art - Item category for art pieces and collectibles
 */
public class Art extends Item {
    private static final long serialVersionUID = 1L;
    
    private String artist;
    private String artworkType; // loại tác phẩm nghệ thuật
    private int yearCreated;
    private String medium; // chất liệu / kỹ thuật sáng tác
    private boolean isAuthenticated; // đã xác thực 
    private String authentication; // xác thực
    
    public Art() {
        super();
    }
    
    public Art(String name, String description, double startingPrice, String sellerId) {
        super(name, description, startingPrice, sellerId);
    }
    
    @Override
    public String getItemType() {
        return "ART";
    }
    
    public String getArtist() {
        return artist;
    }
    
    public void setArtist(String artist) {
        this.artist = artist;
    }
    
    public String getArtworkType() {
        return artworkType;
    }
    
    public void setArtworkType(String artworkType) {
        this.artworkType = artworkType;
    }
    
    public int getYearCreated() {
        return yearCreated;
    }
    
    public void setYearCreated(int yearCreated) {
        this.yearCreated = yearCreated;
    }
    
    public String getMedium() {
        return medium;
    }
    
    public void setMedium(String medium) {
        this.medium = medium;
    }
    
    public boolean isAuthenticated() {
        return isAuthenticated;
    }
    
    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }
    
    public String getAuthentication() {
        return authentication;
    }
    
    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }
}
