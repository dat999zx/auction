package com.bidify.server.handler;


import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.service.AuctionService;
import com.bidify.server.service.AuthService;
import com.bidify.common.enums.RequestStatus;

public class AuctionHandler {
    private final AuctionService auctionService = new AuctionService();
    
    public Response handle(Request request){
        return switch (request.getType()){
            case CREATE_AUCTION -> auctionService.createAuction(request);
            default -> new Response(RequestStatus.FAILED, "Invalid auth request");
        };
    }

}
