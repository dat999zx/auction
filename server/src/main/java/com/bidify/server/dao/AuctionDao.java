package com.bidify.server.dao;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.model.Auction;
import com.bidify.server.contract.ImplementAuctionDao;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;

import java.util.List;
import java.util.ArrayList;

import java.time.LocalDateTime;

// giao tiếp với SQLite database về bảng Auctions
public class AuctionDao implements ImplementAuctionDao{
    // tạm thêm cái này để về sau seller tìm lại các auction theo trạng thái của mình
    public List<Auction> findByStatus(AuctionStatus status) throws DatabaseException {
        String sql = "SELECT * FROM Auctions WHERE status = ?";
        return SQLiteHelper.query(sql, rs -> {
            List<Auction> auctions = new ArrayList<>();
            while (rs.next()) {
                Auction auction = new Auction(
                    rs.getString("id"),
                    LocalDateTime.parse(rs.getString("createdAt")),
                    rs.getString("auctionName"),
                    rs.getString("description"),
                    rs.getString("seller"),
                    rs.getString("currentBidder"),
                    rs.getString("category"),
                    rs.getString("type"),
                    rs.getDouble("startingPrice"),
                    rs.getDouble("minIncrement"),
                    LocalDateTime.parse(rs.getString("startAt")),
                    LocalDateTime.parse(rs.getString("endTime"))
                );
                auctions.add(auction);
            }
            return auctions;
        }, status.toString());
    }

    public Auction findById(String id) throws DatabaseException { // lấy auction theo id
        String sql = "SELECT * FROM Auctions WHERE id = ?";
        return SQLiteHelper.query(sql, rs -> {
            if (!rs.next()) return null;
            return new Auction(
                rs.getString("id"),
                LocalDateTime.parse(rs.getString("createdAt")),
                rs.getString("auctionName"),
                rs.getString("description"),
                rs.getString("seller"),
                rs.getString("currentBidder"),
                rs.getString("category"),
                rs.getString("type"),
                rs.getDouble("startingPrice"),
                rs.getDouble("minIncrement"),
                LocalDateTime.parse(rs.getString("startAt")),
                LocalDateTime.parse(rs.getString("endTime"))
            );
        }, id);
    }

    public void create(Auction auction) throws DatabaseException { // tạo auction
        LocalDateTime createdAt = auction.getCreatedAt() == null ? LocalDateTime.now() : auction.getCreatedAt();
        String sql = """
            INSERT INTO Auctions(
            id,
            createdAt,
            auctionName,
            description,
            category,
            type,
            startingPrice,
            minIncrement,
            seller,
            currentBid,
            currentBidder,
            status,
            startAt,
            endTime
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""";

        SQLiteHelper.update(sql,
            auction.getId(),
            createdAt.toString(),
            auction.getAuctionName(),
            auction.getDescription(),
            auction.getCategory(),
            auction.getProductType(),
            auction.getStartingPrice(),
            auction.getMinIncrement(),
            auction.getSellerUsername(),
            auction.getCurrentBid(),
            auction.getCurrentBidderUsername(),
            auction.getStatus().toString(),
            auction.getStartTime().toString(),
            auction.getEndTime().toString()
        );
    }

    public void deleteById(String id) throws DatabaseException { // xóa auction theo id
        String sql = "DELETE FROM Auctions WHERE id = ?";
        SQLiteHelper.update(sql, id);
    }

    public void save(Auction auction) throws DatabaseException { // lưu auction
        LocalDateTime createdAt = auction.getCreatedAt() == null ? LocalDateTime.now() : auction.getCreatedAt();
        SQLiteHelper.update(
            """
                                    UPDATE Auctions SET 
                                createdAt = ?,
                                auctionName = ?,
                            description = ?,
                        category = ?,
                    type = ?,
                startingPrice = ?,
            minIncrement = ?,
                seller = ?,
                    currentBid = ?,
                        currentBidder = ?,
                            status = ?,
                                startAt = ?,
                                    endTime = ?
                                        WHERE id = ?
            """,
            createdAt.toString(),
            auction.getAuctionName(),
            auction.getDescription(),
            auction.getCategory(),
            auction.getProductType(),
            auction.getStartingPrice(),
            auction.getMinIncrement(),
            auction.getSellerUsername(),
            auction.getCurrentBid(),
            auction.getCurrentBidderUsername(),
            auction.getStatus().toString(),
            auction.getStartTime().toString(),
            auction.getEndTime().toString(),
            auction.getId()
        );
    }
}
// CREATE_AUCTION, // tạo đấu giá
// GET_AUCTIONS, // xem list các cuột đấu giá
// GET_AUCTION_DETAIL, // xem chi tiết cuộc đấu giá
// DELETE_AUCTION, // xóa cuộc đấu giá
