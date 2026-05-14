package com.bidify.server.utility;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.dto.BidDto;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;

import java.util.Comparator;
import java.util.List;

public class AuctionMapper {
    private AuctionMapper() {}
    
    public static AuctionDto toDto(Auction auction) {
        if (auction == null) return null;
        AuctionDto dto = new AuctionDto(
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
        dto.setBidHistory(mapBidHistory(auction));
        return dto;
    }

    private static List<BidDto> mapBidHistory(Auction auction) {
        return auction.getBids().stream()
                .sorted(Comparator.comparing(Bid::getCreatedAt).reversed())
                .map(bid -> new BidDto(
                        bid.getId(),
                        bid.getCreatedAt().toString(),
                        bid.getAuctionId(),
                        bid.getBidderUsername(),
                        bid.getAmount()
                ))
                .toList();
    }
}
