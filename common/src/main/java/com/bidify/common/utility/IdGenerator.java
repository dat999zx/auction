package com.bidify.common.utility;

import java.util.UUID;

public class IdGenerator {
    private IdGenerator(){}

    public static String genRequestId() { return "REQ-" + genId(); }
    
    public static String genAuctionId(){ return "AUC-" + genId(); }
    
    public static String genBidId(){ return "BID-" + genId(); }
    
    public static String genItemId(){ return "ITM-" + genId(); }
    
    public static String genTransactionId(){ return "TRS-" + genId(); }
    
    private static String genId() { return UUID.randomUUID().toString().substring(0, 12); }
}
