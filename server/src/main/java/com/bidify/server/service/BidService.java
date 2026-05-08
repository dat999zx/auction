package com.bidify.server.service;

import com.bidify.common.dto.BidDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.model.Response;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;

import java.util.ArrayList;
import java.util.List;

public class BidService {
    private static BidService instance = new BidService();
    private final BidDao bidDao = BidDao.getInstance();
    private final AuctionDao auctionDao = AuctionDao.getInstance();

    private BidService() {}

    public static BidService getInstance() { return instance; }

    public Response getUserBids(ClientHandler client) {
        return ServiceUtil.handleRequest(() -> {
            String username = client.getCurrentUsername();
            if (!client.isInSession() || username == null) return new Response(RequestStatus.UNAUTHORIZED, "Unauthorized");

            List<Bid> bids = bidDao.findByUsername(username);
            List<BidDto> dtos = new ArrayList<>();
            
            for (Bid bid : bids) {
                Auction auction = RealtimeDatabase.getRuntimeAuction(bid.getAuctionId());
                if (auction == null) auction = auctionDao.findById(bid.getAuctionId());

                dtos.add(new BidDto(
                        bid.getId(),
                        bid.getCreatedAt().toString(),
                        bid.getBidderUsername(),
                        bid.getAuctionId(),
                        bid.getAmount()
                ));
            }
            return new Response(RequestStatus.SUCCESS, "Bid history loaded", dtos);
        });
    }
}
