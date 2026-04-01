package com.bidify.common.utility;

import java.util.UUID;

public class IdGenerator {
    private IdGenerator(){}

    public static String genRequestId() { return "REQ-" + UUID.randomUUID().toString().substring(0, 12); }

    public static String genAuctionId(){ return "AUC-" + UUID.randomUUID().toString().substring(0, 12); }

    public static String genBidId(){ return "BID-" + UUID.randomUUID().toString().substring(0, 12); }

    public static String genItemId(){ return "ITM-" + UUID.randomUUID().toString().substring(0, 12); }
}

