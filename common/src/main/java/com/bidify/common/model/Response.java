package com.bidify.common.model;
import com.bidify.common.enums.RequestStatus;

public class Response {
    private String id;
    private RequestStatus status;
    private String message;
    private Object data;

    // dùng để tạo một đối tượng Response
    public Response(RequestStatus status, String message) {
        this.status = status;
        this.message = message;
    }
    // dùng để tạo một đối tượng Response
    public Response(RequestStatus status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // dùng để thiết lập ID
    public void setId(String id){ this.id = id; }

    // dùng để lấy ID
    public String getId(){ return id; }
    // dùng để lấy trạng thái
    public RequestStatus getStatus(){ return status; }
    // dùng để lấy tin nhắn
    public String getMessage(){ return message; }
    // dùng để lấy data
    public Object getData(){ return data; }
}
