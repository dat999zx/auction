package com.bidify.common.model;
import com.bidify.common.enums.RequestStatus;

public class Response {
    private String id;
    private RequestStatus status;
    private String message;
    private Object data;

    public Response(Request request) {
        this.id = request.getId();
        this.status = RequestStatus.NOT_FOUND;
    }
    public Response(Request request, RequestStatus status, String message) {
        this.id = request.getId();
        this.status = status;
        this.message = message;
    }
    public Response(Request request, RequestStatus status, String message, Object data) {
        this.id = request.getId();
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public String getId(){ return id; }
    public RequestStatus getStatus(){ return status; }
    public String getMessage(){ return message; }
    public Object getData(){ return data; }
}