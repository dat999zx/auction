package com.bidify.server.model;

public class Art extends Item {
    private String artist;
    private String artworkType;
    private int yearCreated;
    private String medium;
    private boolean authenticated;
    private String authentication;

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
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getAuthentication() {
        return authentication;
    }

    public void setAuthentication(String authentication) {
        this.authentication = authentication;
    }
}

