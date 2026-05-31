package com.bidify.common.model;

// Admin duyệt hoặc từ chối yêu cầu nạp/rút tiền của user
public class WalletReviewRequest {
    // ID yêu cầu nạp/rút tiền cần duyệt
    private String walletRequestId;
    // true = duyệt, false = từ chối
    private boolean approved;

    public WalletReviewRequest(String walletRequestId, boolean approved) {
        this.walletRequestId = walletRequestId;
        this.approved = approved;
    }

    public String getWalletRequestId() { return walletRequestId; }
    public boolean isApproved() { return approved; }
}
