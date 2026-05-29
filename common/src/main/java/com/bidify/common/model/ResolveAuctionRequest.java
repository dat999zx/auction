package com.bidify.common.model;

import com.bidify.common.enums.AuctionResolutionAction;

// Admin xử lý kết quả phiên đấu giá đã kết thúc
public class ResolveAuctionRequest {
    // ID phiên đấu giá cần xử lý kết quả
    private String auctionId;
    // Hành động xử lý (COMPLETE — hoàn tất, CANCEL — hủy bỏ)
    private AuctionResolutionAction action;

    public ResolveAuctionRequest(String auctionId, AuctionResolutionAction action) {
        this.auctionId = auctionId;
        this.action = action;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public AuctionResolutionAction getAction() {
        return action;
    }
}
