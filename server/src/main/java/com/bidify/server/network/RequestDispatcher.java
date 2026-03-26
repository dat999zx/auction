package com.bidify.server.network;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.service.AuthService;
import com.bidify.server.service.AuctionService;

// chuyển hướng request đúng vào các service tương ứng
public class RequestDispatcher {
    private final AuthService authService = new AuthService();
    private final AuctionService auctionService = new AuctionService();

    public Response dispatch(ClientHandler client, Request request){
        if (request == null || request.getType() == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");

        return switch (request.getType()) {
            case REGISTER -> authService.register(request);
            case LOGIN -> authService.login(client, request);
            case LOGOUT -> authService.logout(client, request);
            case CREATE_AUCTION -> auctionService.createAuction(client, request);
            case UPDATE_AUCTION -> auctionService.updateAuction(client, request);
            case GET_AUCTION_DETAIL -> auctionService.getAuctionDetail(client, request);
            case DELETE_AUCTION -> auctionService.deleteAuction(client, request);
            default -> new Response(RequestStatus.INVALID_REQUEST, "Invalid request type");

        };
    }
}
