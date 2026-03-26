package com.bidify.server.repository;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.model.User;
import com.bidify.server.model.Auction;
import com.bidify.server.database.DatabaseManager;

import java.time.LocalDateTime;

public class AuctionRepository {
    public Auction findById(String id){
        String sql = "SELECT * FROM Auctions WHERE id = ?";
        return DatabaseManager.query(sql, rs -> {
            if (!rs.next()) return null;
            Auction auction = new Auction(rs.getString("id"));
            auction.setAuctionName(rs.getString("auctionName"));
            auction.setDescription(rs.getString("description"));
            auction.setCategory(rs.getString("category"));
            auction.setProductType(rs.getString("type"));
            auction.setStartingPrice(rs.getDouble("startingPrice"));
            auction.setMinIncrement(rs.getDouble("minIncrement"));
            auction.setMaxIncrement(rs.getDouble("maxIncrement"));
            auction.setSeller(rs.getString("seller"));
            auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
            auction.setStartTime(LocalDateTime.parse(rs.getString("startAt")));
            auction.setEndTime(LocalDateTime.parse(rs.getString("endTime")));
            return auction;
        }, id);
    }

    public boolean update(Auction auction){
        String sql = """
            UPDATE Auctions SET
                auctionName = ?,
                description = ?,
                startingPrice = ?,
                startAt = ?,
                endTime = ?
            WHERE id = ?
        """;
        return DatabaseManager.update(sql,
                auction.getAuctionName(),
                auction.getDescription(),
                auction.getStartingPrice(),
                auction.getStartTime().toString(),
                auction.getEndTime().toString(),
                auction.getId());
    }

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
    public boolean deleteById(String id){
        String sql = "DELETE FROM Auctions WHERE id = ?";
        return DatabaseManager.update(sql, id);
    }
}
// CREATE_AUCTION, // tạo đấu giá
// GET_AUCTIONS, // xem list các cuột đấu giá
// GET_AUCTION_DETAIL, // xem chi tiết cuộc đấu giá
// DELETE_AUCTION, // xóa cuộc đấu giá
