package com.bidify.common.model;

public class SearchAuctionRequest {
    private String query;

    // dùng để tạo một đối tượng SearchAuctionRequest
    public SearchAuctionRequest() {}

    // dùng để tạo một đối tượng SearchAuctionRequest
    public SearchAuctionRequest(String query) { this.query = query; }

    // dùng để lấy truy vấn
    public String getQuery() { return query; }

    // dùng để thiết lập truy vấn
    public void setQuery(String query) { this.query = query;
    }
}
