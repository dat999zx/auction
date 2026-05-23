package com.bidify.server.utility;

import java.util.Comparator;
import java.util.List;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.dto.BidDto;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.Item;

public class AuctionMapper {
    private AuctionMapper() {}
    
    public static AuctionDto toDto(Auction auction) {
        return toDto(auction, null, null, null);
    }

    public static AuctionDto toDto(Auction auction, Item item) {
        return toDto(auction, item, null, null);
    }

    public static AuctionDto toDto(Auction auction, Item item, String thumbnailBase64, List<String> galleryBase64) {
        if (auction == null) return null;

        String auctionName = item != null ? item.getName() : auction.getAuctionName();
        String description = item != null ? item.getDescription() : auction.getDescription();
        String category = item != null ? item.getCategory() : null;
        String productType = item != null ? item.getProductType() : null;

        AuctionDto dto = new AuctionDto(
                auction.getId(),
                auction.getItemId(),
                auction.getCreatedAt().toString(),
                auctionName,
                description,
                auction.getSellerUsername(),
                auction.getCurrentBidderUsername(),
                category,
                productType,
                auction.getStartingPrice(),
                auction.getCurrentBid(),
                auction.getMinIncrement(),
                auction.getStartTime().toString(),
                auction.getEndTime().toString(),
                auction.getStatus().name()
        );
        dto.setAntiSnipingTriggerTime(TimeUtil.formatHHMM(auction.getAntiSnipingTriggerTime()));
        dto.setAntiSnipingExtensionTime(TimeUtil.formatHHMM(auction.getAntiSnipingExtensionTime()));
        dto.setMaxEndTime(auction.getMaxEndTime() != null ? auction.getMaxEndTime().toString() : null);
        dto.setThumbnailBase64(thumbnailBase64);
        dto.setGalleryBase64(galleryBase64);
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
                        bid.getAmount(),
                        bid.isAutoBidGenerated()
                ))
                .toList();
    }
}
