package com.bidify.common.model;
import com.bidify.common.enums.RequestStatus;

// Gói tin server trả về cho client sau khi xử lý request
public class Response {
    // ID khớp với Request tương ứng (để client biết response này cho request nào)
    private String id;
    // Kết quả xử lý: SUCCESS hoặc FAILED
    private RequestStatus status;
    // Thông báo mô tả kết quả (ví dụ: "Login successfully", "Username already exists")
    private String message;
    // Dữ liệu trả về (UserDto, AuctionDto... tuỳ request)
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
