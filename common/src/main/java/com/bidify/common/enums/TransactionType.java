package com.bidify.common.enums;

public enum TransactionType {
    DEPOSIT,          // nạp tiền
    WITHDRAW,         // rút tiền
    BID_LOCKED,       // đóng băng tiền khi bid
    BID_UNLOCKED,     // hoàn tiền khi bị outbid
    AUCTION_PAY,      // thanh toán khi thắng đấu giá
    AUCTION_PROFIT    // nhận tiền khi bán được hàng
}
