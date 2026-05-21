package com.bidify.server.dispatcher;

import com.bidify.common.core.Router;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;

public class RequestDispatcher extends Router<RequestType, RequestHandler> {
    private static final RequestDispatcher instance = new RequestDispatcher();

    // dùng để tạo một đối tượng RequestDispatcher
    private RequestDispatcher() {}

    // dùng để lấy đối tượng Singleton
    public static RequestDispatcher getInstance() { return instance; }

    // dùng để dispatch
    public Response dispatch(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            if (request == null || request.getType() == null)
                throw new ValidationException("Invalid request");

            RequestHandler handler = getHandler(request.getType());
            if (handler == null)
                throw new ValidationException("Unsupported request type: " + request.getType());

            return handler.handle(client, request);
        });
    }
}
