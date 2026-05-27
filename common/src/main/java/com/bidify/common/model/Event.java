package com.bidify.common.model;

import com.bidify.common.enums.EventType;

// Gói tin server chủ động đẩy về client (realtime) — không phải trả lời request
public class Event {
    // Loại sự kiện (BID_PLACED, AUCTION_STARTED, AUCTION_ENDED...)
    private EventType type;
    // Thông báo kèm theo sự kiện
    private String message;
    // Dữ liệu đi kèm sự kiện (AuctionDto mới, BidDto...)
    private Object data;

    public Event(EventType type, String message){
        this.type = type;
        this.message = message;
    }

    public Event(EventType type, String message, Object data){
        this.type = type;
        this.message = message;
        this.data = data;
    }

    public EventType getType(){ return type; }
    public String getMessage(){ return message; }
    public Object getData(){ return data; }
}
