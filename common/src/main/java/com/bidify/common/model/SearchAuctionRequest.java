package com.bidify.common.model;

public class SearchAuctionRequest {
    private String query;

    public SearchAuctionRequest(String query) { this.query = query; }

    public String getQuery() { return query; }

    public void setQuery(String query) { this.query = query;
    }
}
