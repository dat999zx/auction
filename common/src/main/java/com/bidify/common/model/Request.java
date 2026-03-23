package com.bidify.common.model;
import com.bidify.common.enums.RequestType;
import com.bidify.common.util.IdGenerator;

public class Request {
    private String id;
    private RequestType type;
    private Object data;

    public Request(){ id = IdGenerator.genRequestId(); }
    public Request(RequestType type, Object data){
        this.id = IdGenerator.genRequestId();
        this.type = type;
        this.data = data;
    }

    public String getId(){ return id; }
    public RequestType getType(){ return type; }
    public Object getData(){ return data; }
}