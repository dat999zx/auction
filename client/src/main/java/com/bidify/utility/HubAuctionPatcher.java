package com.bidify.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bidify.common.dto.AuctionDto;

public class HubAuctionPatcher {
    public static boolean replaceAuction(AuctionDto[] auctions, AuctionDto updatedAuction) {
        if (auctions == null || updatedAuction == null || updatedAuction.getId() == null)
            return false;

        for (int i = 0; i < auctions.length; i++) {
            AuctionDto existing = auctions[i];
            if (existing == null || existing.getId() == null)
                continue;
            if (!updatedAuction.getId().equals(existing.getId()))
                continue;
            auctions[i] = updatedAuction;
            return true;
        }

        return false;
    }

    public static boolean containsAuction(AuctionDto[] auctions, String auctionId) {
        if (auctions == null || auctionId == null || auctionId.isBlank())
            return false;

        for (AuctionDto auction : auctions) {
            if (auction == null || auction.getId() == null)
                continue;
            if (auctionId.equals(auction.getId()))
                return true;
        }

        return false;
    }

    public static AuctionDto[] removeAuction(AuctionDto[] auctions, String auctionId) {
        if (auctions == null || auctions.length == 0 || auctionId == null || auctionId.isBlank())
            return new AuctionDto[0];

        List<AuctionDto> remaining = new ArrayList<>();
        for (AuctionDto auction : auctions) {
            if (auction == null || auction.getId() == null || auctionId.equals(auction.getId()))
                continue;
            remaining.add(auction);
        }
        return remaining.toArray(AuctionDto[]::new);
    }

    public static AuctionDto[] upsertAuction(AuctionDto[] auctions, AuctionDto updatedAuction) {
        if (updatedAuction == null || updatedAuction.getId() == null || updatedAuction.getId().isBlank())
            return auctions == null ? new AuctionDto[0] : auctions;

        List<AuctionDto> next = new ArrayList<>(Arrays.asList(auctions == null ? new AuctionDto[0] : auctions));
        for (int i = 0; i < next.size(); i++) {
            AuctionDto existing = next.get(i);
            if (existing == null || existing.getId() == null)
                continue;
            if (!updatedAuction.getId().equals(existing.getId()))
                continue;
            next.set(i, updatedAuction);
            return next.toArray(AuctionDto[]::new);
        }

        next.add(updatedAuction);
        return next.toArray(AuctionDto[]::new);
    }
}
