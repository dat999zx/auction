package com.bidify.model;

public class Response {
    private String status;
    private String message;
    private Object data;

    public Response() {}

    public Response(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public Response(String status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getStatus() { return status; }

    public String getMessage() { return message; }

    public Object getData() { return data; }
}