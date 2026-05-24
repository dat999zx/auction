package com.bidify.server.service.auction;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.model.JoinAuctionRequest;
import com.bidify.common.model.LeaveAuctionRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.model.Auction;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;

public class AuctionAudienceService {
    private final AuctionDtoAssembler dtoAssembler;
    private final AuctionRealtimePublisher realtimePublisher;

    public AuctionAudienceService(AuctionDtoAssembler dtoAssembler, AuctionRealtimePublisher realtimePublisher) {
        this.dtoAssembler = dtoAssembler;
        this.realtimePublisher = realtimePublisher;
    }

    public Response join(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            JoinAuctionRequest data = JsonUtil.fromMap(request.getData(), JoinAuctionRequest.class);
            ServiceUtil.validateRequestData(data);

            ServiceUtil.requireSession(client);

            String auctionId = data.getAuctionId();
            String username = client.getCurrentUsername();

            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");
            Auction auction = RealtimeDatabase.getRuntimeAuction(auctionId);
            if (auction == null)
                throw new AuctionException("Auction not found");

            if (!isRuntimeAuction(auction))
                throw new AuctionException("Auction not found");
            if (RealtimeDatabase.isWatchingAuction(username, auctionId))
                return new Response(RequestStatus.SUCCESS, "You are already watching this auction");

            RealtimeDatabase.subscribeAuctionChannel(auctionId, username);

            AuctionDto auctionDto = dtoAssembler.toAuctionDto(auction, false);
            publishAuctionUpdate(auction, "Auction watcher count updated");
            return new Response(RequestStatus.SUCCESS, "Join auction successfully", auctionDto);
        });
    }

    public Response leave(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            LeaveAuctionRequest data = JsonUtil.fromMap(request.getData(), LeaveAuctionRequest.class);
            ServiceUtil.validateRequestData(data);
            ServiceUtil.requireSession(client);

            String auctionId = data.getAuctionId();
            String username = client.getCurrentUsername();

            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");

            if (!RealtimeDatabase.isWatchingAuction(username, auctionId))
                throw new AuctionException("You are not watching this auction");

            RealtimeDatabase.unsubscribeAuctionChannel(auctionId, username);
            Auction auction = RealtimeDatabase.getRuntimeAuction(auctionId);
            if (auction != null)
                publishAuctionUpdate(auction, "Auction watcher count updated");
            return new Response(RequestStatus.SUCCESS, "Leave auction successfully");
        });
    }

    public void publishLiveAudienceUpdate(String auctionId) {
        if (auctionId == null || auctionId.isBlank())
            return;

        Auction auction = RealtimeDatabase.getLiveAuction(auctionId);
        if (auction == null)
            return;

        publishAuctionUpdate(auction, "Auction watcher count updated");
    }

    private void publishAuctionUpdate(Auction auction, String message) {
        realtimePublisher.publishAuctionUpdate(auction, dtoAssembler.toAuctionDto(auction, false), message);
    }

    private boolean isRuntimeAuction(Auction auction) {
        if (auction == null) return false;
        AuctionStatus status = auction.getStatus();
        return status == AuctionStatus.UPCOMING || status == AuctionStatus.ACTIVE;
    }
}
