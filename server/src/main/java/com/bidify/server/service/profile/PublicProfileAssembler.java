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

    public static class ReputationSummary {
        private final int completedSales;
        private final int failedSales;
        private final String completionRate;
        private final String reputationLabel;
        private final double starRating;
        private final String starVisual;

        public ReputationSummary(
                int completedSales,
                int failedSales,
                String completionRate,
                String reputationLabel,
                double starRating,
                String starVisual) {
            this.completedSales = completedSales;
            this.failedSales = failedSales;
            this.completionRate = completionRate;
            this.reputationLabel = reputationLabel;
            this.starRating = starRating;
            this.starVisual = starVisual;
        }

        public int completedSales() { return completedSales; }
        public int failedSales() { return failedSales; }
        public String completionRate() { return completionRate; }
        public String reputationLabel() { return reputationLabel; }
        public double starRating() { return starRating; }
        public String starVisual() { return starVisual; }

        public static ReputationSummary from(int completedSales, int failedSales, int ratedSales) {
            String completionRate = ratedSales > 0
                    ? String.format("%.1f%%", (double) completedSales / ratedSales * 100)
                    : "0.0%";
            double rateValue = ratedSales > 0 ? (double) completedSales / ratedSales * 100 : 0.0;
            
            double starRating = ratedSales > 0 ? ((double) completedSales / ratedSales) * 5.0 : 5.0;
            
            int fullStars = (int) Math.round(starRating);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 5; i++) {
                if (i < fullStars) {
                    sb.append("★");
                } else {
                    sb.append("☆");
                }
            }
            String starVisual = sb.toString();

            String label;
            if (ratedSales < 3) {
                label = "New Seller";
            } else if (completedSales >= 10 && rateValue >= 95.0) {
                label = "Top Seller";
            } else if (rateValue >= 80.0) {
                label = "Reliable Seller";
            } else {
                label = "Needs Review";
            }
            return new ReputationSummary(completedSales, failedSales, completionRate, label, starRating, starVisual);
        }
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
        int completedSales = 0;
        int failedSales = 0;
        int ratedSales = 0;
        double activeVolume = 0.0;

        for (Auction auction : userAuctions) {
            AuctionStatus status = auction.getStatus();
            if (status == AuctionStatus.ACTIVE) {
                activeAuctions++;
                activeVolume += auction.getCurrentBid() > 0 ? auction.getCurrentBid() : auction.getStartingPrice();
            } else if (status != AuctionStatus.UPCOMING) {
                closedAuctions++;
                if (auction.getCurrentBidderUsername() != null && !auction.getCurrentBidderUsername().isBlank()) {
                    soldAuctions++;
                }
            }

            if (status == AuctionStatus.COMPLETED) {
                completedSales++;
                ratedSales++;
            } else if (status == AuctionStatus.PAID || status == AuctionStatus.AWAITING_DELIVERY) {
                ratedSales++;
            } else if (status == AuctionStatus.CANCELED || status == AuctionStatus.BANNED) {
                failedSales++;
                ratedSales++;
            }
        }

        int totalBids = bidDao.findByUsername(username).size();
        String sellRate = closedAuctions > 0
                ? String.format("%.1f%%", (double) soldAuctions / closedAuctions * 100)
                : "0.0%";
        ReputationSummary reputation = ReputationSummary.from(completedSales, failedSales, ratedSales);

        return new PublicProfileStatsDto(
                totalAuctions,
                activeAuctions,
                closedAuctions,
                soldAuctions,
                totalBids,
                activeVolume,
                sellRate,
                reputation.completedSales(),
                reputation.failedSales(),
                reputation.completionRate(),
                reputation.reputationLabel(),
                reputation.starRating(),
                reputation.starVisual());
    }

    private AuctionDto[] toAuctionDtos(List<Auction> auctions) {
        AuctionDto[] auctionDtos = new AuctionDto[auctions.size()];
        for (int i = 0; i < auctions.size(); i++)
            auctionDtos[i] = auctionDtoAssembler.toAuctionDto(auctions.get(i), false);
        return auctionDtos;
    }
}
