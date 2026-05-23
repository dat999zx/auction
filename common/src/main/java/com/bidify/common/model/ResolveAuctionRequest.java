package com.bidify.common.model;

import com.bidify.common.enums.AuctionResolutionAction;

public class ResolveAuctionRequest {
    private String auctionId;
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
