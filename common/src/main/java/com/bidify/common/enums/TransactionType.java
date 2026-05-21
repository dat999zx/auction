package com.bidify.common.enums;

public enum TransactionType {
    DEPOSIT,          // nạp tiền
    WITHDRAW,         // rút tiền
    AUCTION_PAY,      // bidder khi thắng đấu giá
    AUCTION_PROFIT,   // seller nhận tiền
    AUCTION_REFUND    // hoàn tiền cho bidder khi hủy
}
