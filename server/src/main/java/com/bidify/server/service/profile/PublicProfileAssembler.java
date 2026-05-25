package com.bidify.server.service.profile;

import java.util.List;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.dto.PublicProfileDto;
import com.bidify.common.dto.PublicProfileStatsDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Image;
import com.bidify.server.model.User;
import com.bidify.server.service.ImageService;
import com.bidify.server.service.auction.AuctionDtoAssembler;

public class PublicProfileAssembler {
    private final AuctionDao auctionDao;
    private final BidDao bidDao;
    private final ImageDao imageDao;
    private final ImageService imageService;
    private final AuctionDtoAssembler auctionDtoAssembler;

    public PublicProfileAssembler(
            AuctionDao auctionDao,
            BidDao bidDao,
            ImageDao imageDao,
            ImageService imageService,
            AuctionDtoAssembler auctionDtoAssembler) {
        this.auctionDao = auctionDao;
        this.bidDao = bidDao;
        this.imageDao = imageDao;
        this.imageService = imageService;
        this.auctionDtoAssembler = auctionDtoAssembler;
    }

    public PublicProfileDto assemble(User user) {
        String profileImageBase64 = loadProfileImage(user);
        List<Auction> userAuctions = auctionDao.findBySellerUsername(user.getUsername());
        PublicProfileStatsDto stats = buildStats(user.getUsername(), userAuctions);
        AuctionDto[] auctionDtos = toAuctionDtos(userAuctions);

        return new PublicProfileDto(
                user.getUsername(),
                user.getNickname(),
                profileImageBase64,
                user.getEmail(),
                user.getPhoneNumber(),
                stats,
                auctionDtos);
    }

    private String loadProfileImage(User user) {
        if (user.getProfileImageId() == null || user.getProfileImageId().isBlank()) return null;
        try {
            Image image = imageDao.findById(user.getProfileImageId());
            return image == null ? null : imageService.getBase64Image(image.getFilePath());
        } catch (DatabaseException e) {
            return null;
        }
    }

    private PublicProfileStatsDto buildStats(String username, List<Auction> userAuctions) {
        int totalAuctions = userAuctions.size();
        int activeAuctions = 0;
        int closedAuctions = 0;
        int soldAuctions = 0;
        double activeVolume = 0.0;

        for (Auction auction : userAuctions) {
            if (auction.getStatus() == AuctionStatus.ACTIVE) {
                activeAuctions++;
                activeVolume += auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingPrice();
            } else if (auction.getStatus() != AuctionStatus.UPCOMING) {
                closedAuctions++;
                if (auction.getCurrentBidderUsername() != null && !auction.getCurrentBidderUsername().isBlank()) {
                    soldAuctions++;
                }
            }
        }

        int totalBids = bidDao.findByUsername(username).size();
        String sellRate = closedAuctions > 0
                ? String.format("%.1f%%", (double) soldAuctions / closedAuctions * 100)
                : "0.0%";

        return new PublicProfileStatsDto(
                totalAuctions,
                activeAuctions,
                closedAuctions,
                soldAuctions,
                totalBids,
                activeVolume,
                sellRate);
    }

    private AuctionDto[] toAuctionDtos(List<Auction> auctions) {
        AuctionDto[] auctionDtos = new AuctionDto[auctions.size()];
        for (int i = 0; i < auctions.size(); i++)
            auctionDtos[i] = auctionDtoAssembler.toAuctionDto(auctions.get(i), false);
        return auctionDtos;
    }
}
