package com.bidify.server.dao;

import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Bid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDao {
    private static BidDao instance = new BidDao();

    // dùng để tạo một đối tượng BidDao
    private BidDao() {}

    // dùng để lấy đối tượng Singleton
    public static BidDao getInstance() { return instance; }

    // dùng để tạo
    public void create(Bid bid) throws DatabaseException {
        String sql = "INSERT INTO Bids (id, createdAt, auctionId, bidder, amount, autoBidGenerated) VALUES (?, ?, ?, ?, ?, ?)";
        SQLiteHelper.update(sql,
                bid.getId(),
                bid.getCreatedAt().toString(),
                bid.getAuctionId(),
                bid.getBidderUsername(),
                bid.getAmount(),
                bid.isAutoBidGenerated() ? 1 : 0
        );
    }

    // dùng để tìm kiếm bởi đấu giá ID
    public List<Bid> findByAuctionId(String auctionId) throws DatabaseException {
        String sql = "SELECT * FROM Bids WHERE auctionId = ? ORDER BY amount ASC";
        return SQLiteHelper.query(sql, rs -> {
            List<Bid> bids = new ArrayList<>();
            while (rs.next()) {
                Bid bid = new Bid(
                        rs.getString("id"),
                        LocalDateTime.parse(rs.getString("createdAt")),
                        rs.getString("auctionId"),
                        rs.getString("bidder"),
                        rs.getDouble("amount"),
                        rs.getInt("autoBidGenerated") == 1
                );
                bids.add(bid);
            }
            return bids;
        }, auctionId);
    }

    // dùng để tìm kiếm bởi username
    public List<Bid> findByUsername(String username) throws DatabaseException {
        String sql = "SELECT * FROM Bids WHERE bidder = ? ORDER BY createdAt DESC";
        return SQLiteHelper.query(sql, rs -> {
            List<Bid> bids = new ArrayList<>();
            while (rs.next()) {
                Bid bid = new Bid(
                        rs.getString("id"),
                        LocalDateTime.parse(rs.getString("createdAt")),
                        rs.getString("auctionId"),
                        rs.getString("bidder"),
                        rs.getDouble("amount"),
                        rs.getInt("autoBidGenerated") == 1
                );
                bids.add(bid);
            }
            return bids;
        }, username);
    }

    // dùng để xóa bởi ID
    public void deleteById(String bidId) throws DatabaseException {
        String sql = "DELETE FROM Bids WHERE id = ?";
        SQLiteHelper.update(sql, bidId);
    }

    // dùng để xóa bởi đấu giá ID
    public void deleteByAuctionId(String auctionId) throws DatabaseException {
        SQLiteHelper.update("DELETE FROM Bids WHERE auctionId = ?", auctionId);
    }

    // dùng để xóa bởi username
    public void deleteByUsername(String username) throws DatabaseException {
        SQLiteHelper.update("DELETE FROM Bids WHERE bidder = ?", username);
    }
}
