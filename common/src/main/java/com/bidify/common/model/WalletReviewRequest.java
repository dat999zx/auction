package com.bidify.common.model;

public class WalletReviewRequest {
    private String walletRequestId;
    private boolean approved;

    // dùng để tạo một request trống phục vụ cho việc parse JSON
    public WalletReviewRequest() {}

    // dùng để tạo một request review cụ thể với ID và trạng thái duyệt
    public WalletReviewRequest(String walletRequestId, boolean approved) {
        this.walletRequestId = walletRequestId;
        this.approved = approved;
    }

    // dùng để lấy ID của yêu cầu nạp/rút tiền cần review
    public String getWalletRequestId() { return walletRequestId; }
    // dùng để kiểm tra xem yêu cầu được chấp thuận (approve) hay từ chối (deny)
    public boolean isApproved() { return approved; }
}
