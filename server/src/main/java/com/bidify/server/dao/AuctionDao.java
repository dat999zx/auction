package com.bidify.server.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Auction;

// giao tiếp với SQLite database về bảng Auctions
public class AuctionDao {
    private static AuctionDao instance = new AuctionDao();

    private AuctionDao() {}

    public static AuctionDao getInstance() { return instance; }

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
                    rs.getString("itemId"),
                    rs.getString("currentBidder"),
                    rs.getDouble("startingPrice"),
                    rs.getDouble("minIncrement"),
                    LocalDateTime.parse(rs.getString("startAt")),
                    LocalDateTime.parse(rs.getString("endTime")),
                    AuctionStatus.valueOf(rs.getString("status"))
                );
                auction.setCurrentBid(rs.getDouble("currentBid"));
                auction.getBids().addAll(BidDao.getInstance().findByAuctionId(auction.getId()));
                auctions.add(auction);
            }
            return auctions;
        }, status.toString());
    }

    public List<Auction> findBySellerUsername(String sellerUsername) throws DatabaseException {
        String sql = "SELECT * FROM Auctions WHERE seller = ? ORDER BY createdAt DESC";
        return SQLiteHelper.query(sql, rs -> {
            List<Auction> auctions = new ArrayList<>();
            while (rs.next()) {
                Auction auction = new Auction(
                    rs.getString("id"),
                    LocalDateTime.parse(rs.getString("createdAt")),
                    rs.getString("auctionName"),
                    rs.getString("description"),
                    rs.getString("seller"),
                    rs.getString("itemId"),
                    rs.getString("currentBidder"),
                    rs.getDouble("startingPrice"),
                    rs.getDouble("minIncrement"),
                    LocalDateTime.parse(rs.getString("startAt")),
                    LocalDateTime.parse(rs.getString("endTime")),
                    AuctionStatus.valueOf(rs.getString("status"))
                );
                auction.setCurrentBid(rs.getDouble("currentBid"));
                auction.getBids().addAll(BidDao.getInstance().findByAuctionId(auction.getId()));
                auctions.add(auction);
            }
            return auctions;
        }, sellerUsername);
    }

    public Auction findById(String id) throws DatabaseException {
        String sql = "SELECT * FROM Auctions WHERE id = ?";
        return SQLiteHelper.query(sql, rs -> {
            if (!rs.next()) return null;
            Auction auction = new Auction(
                rs.getString("id"),
                LocalDateTime.parse(rs.getString("createdAt")),
                rs.getString("auctionName"),
                rs.getString("description"),
                rs.getString("seller"),
                rs.getString("itemId"),
                rs.getString("currentBidder"),
                rs.getDouble("startingPrice"),
                rs.getDouble("minIncrement"),
                LocalDateTime.parse(rs.getString("startAt")),
                LocalDateTime.parse(rs.getString("endTime")),
                AuctionStatus.valueOf(rs.getString("status"))
            );
            auction.setCurrentBid(rs.getDouble("currentBid"));
            auction.getBids().addAll(BidDao.getInstance().findByAuctionId(id));
            return auction;
        }, id);
    }

    public void create(Auction auction) throws DatabaseException {
        LocalDateTime createdAt = auction.getCreatedAt() == null ? LocalDateTime.now() : auction.getCreatedAt();
        String sql = """
            INSERT INTO Auctions(
            id,
            createdAt,
            auctionName,
            description,
            itemId,
            startingPrice,
            minIncrement,
            seller,
            currentBid,
            currentBidder,
            status,
            startAt,
            endTime
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""";

        SQLiteHelper.update(sql,
            auction.getId(),
            createdAt.toString(),
            auction.getAuctionName(),
            auction.getDescription(),
            auction.getItemId(),
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

    public void deleteById(String id) throws DatabaseException {
        String sql = "DELETE FROM Auctions WHERE id = ?";
        SQLiteHelper.update(sql, id);
    }

    public void save(Auction auction) throws DatabaseException {
        LocalDateTime createdAt = auction.getCreatedAt() == null ? LocalDateTime.now() : auction.getCreatedAt();
        SQLiteHelper.update(
            """
            UPDATE Auctions SET 
            createdAt = ?,
            auctionName = ?,
            description = ?,
            itemId = ?,
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
            auction.getItemId(),
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

    // lấy tổng số tiền đã bid (nếu là đang là bidder cao nhất) của 1 user
    // là lockedBalance
    public double sumWinningBidsForUser(String username) throws DatabaseException {
        String sql = "SELECT SUM(currentBid) FROM Auctions WHERE currentBidder = ? AND status = 'ACTIVE'";
        return SQLiteHelper.query(sql, rs -> {
            if (!rs.next()) return 0.0;
            return rs.getDouble(1);
        }, username);
    }
}
