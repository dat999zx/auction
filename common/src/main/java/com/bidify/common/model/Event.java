package com.bidify.common.model;

import com.bidify.common.enums.EventType;

public class Event {
    private EventType type;
    private String message;
    private Object data;

    // dùng để tạo một đối tượng Event
    public Event(){}

    // dùng để tạo một đối tượng Event
    public Event(EventType type, String message){
        this.type = type;
        this.message = message;
    }

    // dùng để tạo một đối tượng Event
    public Event(EventType type, String message, Object data){
        this.type = type;
        this.message = message;
        this.data = data;
    }

    // dùng để lấy type
    public EventType getType(){ return type; }
    // dùng để lấy tin nhắn
    public String getMessage(){ return message; }
    // dùng để lấy data
    public Object getData(){ return data; }
}
