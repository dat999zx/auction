package com.bidify.server.dao;

import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Bid;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDao {
    private static BidDao instance = new BidDao();

    private BidDao() {}

    public static BidDao getInstance() { return instance; }

    public void create(Bid bid) throws DatabaseException {
        String sql = "INSERT INTO Bids (id, auctionId, bidder, amount, bidTime) VALUES (?, ?, ?, ?, ?)";
        SQLiteHelper.update(sql,
                bid.getId(),
                bid.getCreatedAt().toString(),
                bid.getAuctionId(),
                bid.getBidderUsername(),
                bid.getAmount()
        );
    }

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
                        rs.getDouble("amount")
                );
                bids.add(bid);
            }
            return bids;
        }, auctionId);
    }

    public List<Bid> findByUsername(String username) throws DatabaseException {
        String sql = "SELECT * FROM Bids WHERE bidder = ? ORDER BY bidTime DESC";
        return SQLiteHelper.query(sql, rs -> {
            List<Bid> bids = new ArrayList<>();
            while (rs.next()) {
                Bid bid = new Bid(
                        rs.getString("id"),
                        LocalDateTime.parse(rs.getString("createdAt")),
                        rs.getString("auctionId"),
                        rs.getString("bidder"),
                        rs.getDouble("amount")
                );
                bids.add(bid);
            }
            return bids;
        }, username);
    }
}
