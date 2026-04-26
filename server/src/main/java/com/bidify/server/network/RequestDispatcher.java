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

        Response response;
        try {
            switch (request.getType()) {
                case REGISTER -> response = authService.register(request);
                case LOGIN -> response = authService.login(client, request);
                case LOGOUT -> response = authService.logout(client, request);
                case JOIN_AUCTION -> response = auctionService.join(client, request);
                case LEAVE_AUCTION -> response = auctionService.leave(client, request);
                case CREATE_AUCTION -> response = auctionService.create(client, request);
                case UPDATE_AUCTION -> response = auctionService.update(client, request);
                case GET_LIVE_AUCTIONS -> response = auctionService.getAllLiveAuctions();
                case GET_AUCTION_DETAIL -> response = auctionService.getDetail(request);
                case DELETE_AUCTION -> response = auctionService.delete(client, request);
                case PLACE_BID -> response = auctionService.placeBid(client, request);
                default -> response = new Response(RequestStatus.INVALID_REQUEST, "Invalid request type");
            }
            return response;
        }
        catch (Exception e) {
            e.printStackTrace();
            return new Response(RequestStatus.ERROR, "Unknown exception");
        }
        catch (StackOverflowError e) {
            e.printStackTrace();
            return new Response(RequestStatus.ERROR, "Stack overflow error");
        }
    }
}
