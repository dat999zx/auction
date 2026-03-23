package com.bidify.common.model;

import com.bidify.common.enums.MessageType;

public class Message {
    private MessageType type;
    private String message;
    private Object data;

    public Message(){}
    public Message(MessageType type, String message){
        this.type = type;
        this.message = message;
    }
    public Message(MessageType type, String message, Object data){
        this.type = type;
        this.message = message;
        this.data = data;
    }

    public MessageType getType(){ return type; }
    public String getMessage(){ return message; }
    public Object getData(){ return data; }
}
