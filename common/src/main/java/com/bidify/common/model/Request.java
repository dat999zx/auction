package com.bidify.common.model;
import com.bidify.common.enums.RequestType;
import com.bidify.common.utility.IdGenerator;

// Gói tin client gửi lên server — bao gồm loại request và dữ liệu kèm theo
public class Request {
    // ID tự sinh để định danh request (dùng để ghép response tương ứng)
    private String id;
    // Loại request (LOGIN, PLACE_BID, CREATE_AUCTION...)
    private RequestType type;
    // Dữ liệu đi kèm (LoginRequest, PlaceBidRequest... tuỳ type)
    private Object data;

    public Request(RequestType type, Object data){
        this.id = IdGenerator.genRequestId();
        this.type = type;
        this.data = data;
    }

    public String getId(){ return id; }
    public RequestType getType(){ return type; }
    public Object getData(){ return data; }
}
