package com.bidify.common.model;
import com.bidify.common.enums.RequestType;
import com.bidify.common.utility.IdGenerator;

public class Request {
    private String id;
    private RequestType type;
    private Object data;

    // dùng để tạo một đối tượng Request
    public Request(){ id = IdGenerator.genRequestId(); }
    // dùng để tạo một đối tượng Request
    public Request(RequestType type, Object data){
        this.id = IdGenerator.genRequestId();
        this.type = type;
        this.data = data;
    }

    // dùng để lấy ID
    public String getId(){ return id; }
    // dùng để lấy type
    public RequestType getType(){ return type; }
    // dùng để lấy data
    public Object getData(){ return data; }
}
