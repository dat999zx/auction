package com.bidify.server.network;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.service.AuthService;
import com.bidify.server.service.AuctionService;
import com.bidify.server.service.UserProfileService;
import com.bidify.server.service.TransactionService;
import com.bidify.server.service.BidService;

// chuyển hướng request đúng vào các service tương ứng
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(RequestDispatcher.class);
    private final AuthService authService = AuthService.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final UserProfileService userProfileService = UserProfileService.getInstance();
    private final TransactionService transactionService = TransactionService.getInstance();
    private final BidService bidService = BidService.getInstance();

    public Response dispatch(ClientHandler client, Request request){
        if (request == null || request.getType() == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");

        try {
            return switch (request.getType()) {
                // AuthService
                case REGISTER -> authService.register(request);
                case LOGIN -> authService.login(client, request);
                case LOGOUT -> authService.logout(client);

                //userProfileService
                case GET_PROFILE -> userProfileService.getProfile(client);
                case UPDATE_PROFILE -> userProfileService.updateProfile(client, request);
                case DEPOSIT -> userProfileService.deposit(client, request);
                case WITHDRAW -> userProfileService.withdraw(client, request);

                // TransactionService
                case GET_TRANSACTIONS -> transactionService.getUserTransactions(client);

                // BidService
                case GET_BID_HISTORY -> bidService.getUserBids(client);

                //AuctionService
                case JOIN_AUCTION -> auctionService.join(client, request);
                case LEAVE_AUCTION -> auctionService.leave(client, request);
                case CREATE_AUCTION -> auctionService.create(client, request);
                case UPDATE_AUCTION -> auctionService.update(client, request);
                case GET_LIVE_AUCTIONS -> auctionService.getAllLiveAuctions();
                case GET_AUCTION_DETAIL -> auctionService.getDetail(request);
                case DELETE_AUCTION -> auctionService.delete(client, request);
                case PLACE_BID -> auctionService.placeBid(client, request);
                case SEARCH_AUCTIONS -> auctionService.search(request); 

                default -> new Response(RequestStatus.INVALID_REQUEST, "Invalid request type");
            };
        }
        catch (Exception e) {
            logger.warn("Exception occurred", e);
            return new Response(RequestStatus.ERROR, "Unknown exception");
        }
        catch (StackOverflowError e) {
            logger.error("Exception occurred", e);
            return new Response(RequestStatus.ERROR, "Stack overflow error");
        }
    }
}
