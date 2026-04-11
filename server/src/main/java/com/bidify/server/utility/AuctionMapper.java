package com.bidify.server.utility;

import com.bidify.common.dto.AuctionDto;
import com.bidify.server.model.Auction;

public class AuctionMapper {
    private AuctionMapper() {}
    
    public static AuctionDto toDto(Auction auction) {
        if (auction == null) return null;
        return new AuctionDto(
            auction.getId(),
            auction.getAuctionName(),
            auction.getDescription(),
            auction.getSellerUsername(),
            auction.getEndTime() == null ? "" : auction.getEndTime().toString(),
            auction.getStartingPrice(),
            auction.getCurrentBid(),
            auction.getBidCount()
        );
    }
}
