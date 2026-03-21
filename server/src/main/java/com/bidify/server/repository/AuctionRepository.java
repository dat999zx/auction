package com.bidify.server.repository;

import com.bidify.server.model.User;
import com.bidify.server.model.Auction;
import com.bidify.server.database.DatabaseManager;

public class AuctionRepository {
    public boolean save(Auction auction){
        String sql = """
            INSERT INTO Auctions(
            id, 
            auctionName,
            description,
            category,
            type,
            startingPrice,
            minIncrement,
            maxIncrement,
            seller,
            currentBidder,
            status,
            startAt, 
            endTime
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""";
        User currentBidder = auction.getCurrentBidder();
        String currentBidderName = (currentBidder != null) ? currentBidder.getNickname() : null;
        return DatabaseManager.update(sql, auction.getId(), 
                                    auction.getAuctionName(),
                                auction.getDescription(),
                            auction.getCategory(),
                        auction.getProductType(),
                    auction.getStartingPrice(),
                auction.getMinIncrement(),
                    auction.getMaxIncrement(),
                        auction.getSeller(),                                    
                            currentBidderName,
                                auction.getStatus().toString(),
                                    auction.getStartTime().toString(),
                                    auction.getEndTime().toString());
    }
}
// CREATE_AUCTION, // tạo đấu giá
// GET_AUCTIONS, // xem list các cuột đấu giá
// GET_AUCTION_DETAIL, // xem chi tiết cuộc đấu giá
// DELETE_AUCTION, // xóa cuộc đấu giá
