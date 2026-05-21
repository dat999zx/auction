package com.bidify.common.utility;

import java.util.UUID;

public class IdGenerator {
    // dùng để tạo một đối tượng IdGenerator
    private IdGenerator(){}

    // dùng để gen yêu cầu ID
    public static String genRequestId() { return "REQ-" + genId(); }
    
    // dùng để gen đấu giá ID
    public static String genAuctionId(){ return "AUC-" + genId(); }
    
    // dùng để gen lượt đặt giá ID
    public static String genBidId(){ return "BID-" + genId(); }
    
    // dùng để gen sản phẩm ID
    public static String genItemId(){ return "ITM-" + genId(); }
    
    // dùng để gen giao dịch ID
    public static String genTransactionId(){ return "TRS-" + genId(); }

    // dùng để gen hình ảnh ID
    public static String genImageId() { return "IMG-" + genId(); }
    
    // dùng để gen ID
    private static String genId() { return UUID.randomUUID().toString().substring(0, 12); }
}
