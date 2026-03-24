package com.bidify.common.model;

import com.bidify.common.enums.EventType;

public class Event {
    private EventType type;
    private String message;
    private Object data;

    public Event(){}

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
