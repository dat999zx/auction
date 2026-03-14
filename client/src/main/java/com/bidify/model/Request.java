package com.bidify.model;

public class Request {
    private String type;
    private Object data;

    public Request() {}

    public Request(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() { return type; }

    public Object getData() { return data; }
}