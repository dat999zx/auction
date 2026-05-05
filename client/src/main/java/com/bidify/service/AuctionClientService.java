package com.bidify.service;

import java.io.IOException;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.GetAuctionDetailRequest;
import com.bidify.common.model.JoinAuctionRequest;
import com.bidify.common.model.LeaveAuctionRequest;
import com.bidify.common.model.PlaceBidRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.network.SocketClient;

public class AuctionClientService {
    private final SocketClient client = SocketClient.getClient();


    // public AuctionDto[] getAuction() throws IOException {
    //     Response response = 
    // }

    public AuctionDto[] getLiveAuctions() throws IOException {
        Response response = client.send(new Request(RequestType.GET_LIVE_AUCTIONS, null));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new AuctionException(response.getMessage() == null ? "Cannot load live auctions." : response.getMessage());
        }

        AuctionDto[] auctions = JsonUtil.fromMap(response.getData(), AuctionDto[].class);
        if (auctions == null) throw new AuctionException("Cannot load live auctions.");
        return auctions;
    }

    public AuctionDto getAuctionDetail(String auctionId) throws IOException {
        Response response = client.send(new Request(RequestType.GET_AUCTION_DETAIL, new GetAuctionDetailRequest(auctionId)));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new AuctionException(response.getMessage() == null ? "Cannot load auction details." : response.getMessage());
        }

        AuctionDto auction = JsonUtil.fromMap(response.getData(), AuctionDto.class);
        if (auction == null) throw new AuctionException("Auction details came back in an unexpected format.");
        return auction;
    }

    public Response createAuction(CreateAuctionRequest data) throws IOException {
        Response response = client.send(new Request(RequestType.CREATE_AUCTION, data));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    public Response placeBid(String auctionId, double bidAmount) throws IOException {
        Response response = client.send(new Request(RequestType.PLACE_BID, new PlaceBidRequest(auctionId, bidAmount)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    public Response join(String auctionId) throws IOException {
        Response response = client.send(new Request(RequestType.JOIN_AUCTION, new JoinAuctionRequest(auctionId)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    public Response leave(String auctionId) throws IOException {
        Response response = client.send(new Request(RequestType.LEAVE_AUCTION, new LeaveAuctionRequest(auctionId)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }
}
