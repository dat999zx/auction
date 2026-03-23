package com.bidify.server.network;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.handler.AuthHandler;
import com.bidify.server.handler.AuctionHandler;

// chuyển hướng request đúng vào các handler tương ứng
public class RequestDispatcher {
    private final AuthHandler authHandler = new AuthHandler();
    private final AuctionHandler auctionHandler = new AuctionHandler();

    public Response dispatch(ClientHandler client, Request request){
        if (request == null || request.getType() == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");

        return switch (request.getType()) {
            case REGISTER, LOGIN, LOGOUT -> authHandler.handle(client, request);
            case CREATE_AUCTION, UPDATE_AUCTION, GET_AUCTION_DETAIL, DELETE_AUCTION -> auctionHandler.handle(client, request);
            default -> new Response(RequestStatus.INVALID_REQUEST, "Invalid request type");

        };
    }
}