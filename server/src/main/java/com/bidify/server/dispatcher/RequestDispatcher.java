package com.bidify.server.dispatcher;

import com.bidify.common.core.Router;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;

public class RequestDispatcher extends Router<RequestType, RequestHandler> {
    private static final RequestDispatcher instance = new RequestDispatcher();

    private RequestDispatcher() {}

    public static RequestDispatcher getInstance() { return instance; }

    public Response dispatch(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            if (request == null || request.getType() == null) {
                return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");
            }

            RequestHandler handler = getHandler(request.getType());
            if (handler == null)
                return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");

            return handler.handle(client, request);
        });
    }
}
