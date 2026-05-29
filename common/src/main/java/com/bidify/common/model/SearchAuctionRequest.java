package com.bidify.common.model;

// Dữ liệu gửi lên khi tìm kiếm phiên đấu giá
public class SearchAuctionRequest {
    // Từ khóa tìm kiếm (tên phiên, tên vật phẩm...)
    private String query;

    public SearchAuctionRequest(String query) { this.query = query; }

    public String getQuery() { return query; }

    public void setQuery(String query) { this.query = query;
    }
}
