package com.bidify.common.model;
import com.bidify.common.enums.RequestStatus;

public class Response {
    private String id;
    private RequestStatus status;
    private String message;
    private Object data;

    public Response(RequestStatus status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public Response(RequestStatus status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public void setId(String id){ this.id = id; }

    public String getId(){ return id; }
    public RequestStatus getStatus(){ return status; }
    public String getMessage(){ return message; }
    public Object getData(){ return data; }
}
