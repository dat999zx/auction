package com.bidify.server.utility;

import com.bidify.common.dto.AuctionDto;
import com.bidify.server.model.Auction;

public class AuctionMapper {
    private AuctionMapper() {}
    
    public static AuctionDto toDto(Auction auction) {
        if (auction == null) return null;
        return new AuctionDto(
                auction.getId(),
                auction.getCreatedAt().toString(),
                auction.getAuctionName(),
                auction.getDescription(),
                auction.getSellerUsername(),
                auction.getCurrentBidderUsername(),
                auction.getCategory(),
                auction.getProductType(),
                auction.getStartingPrice(),
                auction.getCurrentBid(),
                auction.getMinIncrement(),
                auction.getStartTime().toString(),
                auction.getEndTime().toString(),
                auction.getStatus().name()
        );
    }
}