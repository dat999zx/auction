package com.bidify.common.model;
import com.bidify.common.enums.RequestType;

public class Request {
    private RequestType type;
    private Object data;

    public Request(){}
    public Request(RequestType type, Object data){
        this.type = type;
        this.data = data;
    }

    public RequestType getType(){ return type; }
    public Object getData(){ return data; }
}