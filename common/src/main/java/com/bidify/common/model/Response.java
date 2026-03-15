package com.bidify.common.model;
import com.bidify.common.enums.RequestStatus;

public class Response {
    private RequestStatus status;
    private String message;
    private Object data;

    public Response() { this.status = RequestStatus.NOT_FOUND; }
    public Response(RequestStatus status, String message) {
        this.status = status;
        this.message = message;
    }
    public Response(RequestStatus status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public RequestStatus getStatus(){ return status; }
    public String getMessage(){ return message; }
    public Object getData(){ return data; }
}