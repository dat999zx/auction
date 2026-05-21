package com.bidify.common.model;

public class SearchAuctionRequest {
    private String query;

    // dùng để tạo một đối tượng SearchAuctionRequest
    public SearchAuctionRequest() {}

    // dùng để tạo một đối tượng SearchAuctionRequest
    public SearchAuctionRequest(String query) { this.query = query; }

    public String getQuery() { return query; }

    public void setQuery(String query) { this.query = query;
    }
}
