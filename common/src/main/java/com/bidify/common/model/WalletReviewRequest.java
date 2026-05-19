package com.bidify.common.model;

public class WalletReviewRequest {
    private String walletRequestId;
    private boolean approved;

    public WalletReviewRequest() {}

    public WalletReviewRequest(String walletRequestId, boolean approved) {
        this.walletRequestId = walletRequestId;
        this.approved = approved;
    }

    public String getWalletRequestId() { return walletRequestId; }
    public boolean isApproved() { return approved; }
}
