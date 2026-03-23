package com.bidify.server.handler;

import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.service.AuthService;
import com.bidify.common.enums.RequestStatus;

// chuyển hướng vào service tương ứng
public class AuthHandler {
    private final AuthService authService = new AuthService();

    public Response handle(ClientHandler client, Request request) {
        return switch (request.getType()) {
            case REGISTER -> authService.register(request);
            case LOGIN -> authService.login(client, request);
            case LOGOUT -> authService.logout(client, request);
            default -> new Response(request, RequestStatus.FAILED, "Invalid auth request");
        };
    }
}
