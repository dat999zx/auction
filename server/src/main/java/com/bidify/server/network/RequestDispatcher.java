package com.bidify.server.network;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.handler.AuthHandler;

// chuyển hướng request đúng vào các handler tương ứng
public class RequestDispatcher {
    private final AuthHandler authHandler = new AuthHandler();

    public Response dispatch(Request request){
        if (request == null || request.getType() == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");

        return switch (request.getType()) {
            case REGISTER, LOGIN -> authHandler.handle(request);
            default -> new Response(RequestStatus.INVALID_REQUEST, "Invalid request type");
        };
    }
}