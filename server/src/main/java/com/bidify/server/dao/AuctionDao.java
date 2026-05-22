package com.bidify.server.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Auction;

// giao tiếp với SQLite database về bảng Auctions
public class AuctionDao {
    private static AuctionDao instance = new AuctionDao();

    // dùng để tạo một đối tượng AuctionDao
    private AuctionDao() {}

    // dùng để lấy đối tượng Singleton
    public static AuctionDao getInstance() { return instance; }

    // tạm thêm cái này để về sau seller tìm lại các auction theo trạng thái của mình
    // dùng để tìm kiếm bởi trạng thái
    public List<Auction> findByStatus(AuctionStatus status) throws DatabaseException {
        String sql = "SELECT * FROM Auctions WHERE status = ?";
        return SQLiteHelper.query(sql, rs -> {
            List<Auction> auctions = new ArrayList<>();
            while (rs.next()) {
                auctions.add(mapRowToAuction(rs));
            }
            return auctions;
        }, status.toString());
    }

    // dùng để tìm kiếm bởi seller username
    public List<Auction> findBySellerUsername(String sellerUsername) throws DatabaseException {
        String sql = "SELECT * FROM Auctions WHERE seller = ? ORDER BY createdAt DESC";
        return SQLiteHelper.query(sql, rs -> {
            List<Auction> auctions = new ArrayList<>();
            while (rs.next()) {
                auctions.add(mapRowToAuction(rs));
            }
            return auctions;
        }, sellerUsername);
    }

    // dùng để tìm kiếm các phiên đấu giá đã kết thúc cần xử lý của user (winner hoặc seller)
    public List<Auction> findUserSettlements(String username) throws DatabaseException {
        String sql = "SELECT * FROM Auctions WHERE (seller = ? OR currentBidder = ?) AND status NOT IN ('ACTIVE', 'UPCOMING') ORDER BY createdAt DESC";
        return SQLiteHelper.query(sql, rs -> {
            List<Auction> auctions = new ArrayList<>();
            while (rs.next()) {
                auctions.add(mapRowToAuction(rs));
            }
            return auctions;
        }, username, username);
    }


    // dùng để tìm kiếm bởi ID
    public Auction findById(String id) throws DatabaseException {
        String sql = "SELECT * FROM Auctions WHERE id = ?";
        return SQLiteHelper.query(sql, rs -> {
            if (!rs.next()) return null;
            return mapRowToAuction(rs);
        }, id);
    }

    private Auction mapRowToAuction(java.sql.ResultSet rs) throws java.sql.SQLException {
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
        
        int triggerMinutes = rs.getInt("antiSnipingTriggerTime");
        auction.setAntiSnipingTriggerTime(java.time.Duration.ofMinutes(triggerMinutes));
        
        int extensionMinutes = rs.getInt("antiSnipingExtensionTime");
        auction.setAntiSnipingExtensionTime(java.time.Duration.ofMinutes(extensionMinutes));
        
        String maxEndTime = rs.getString("maxEndTime");
        if (maxEndTime != null && !maxEndTime.isBlank()) {
            auction.setMaxEndTime(LocalDateTime.parse(maxEndTime));
        }
        
        auction.getBids().addAll(BidDao.getInstance().findByAuctionId(auction.getId()));
        return auction;
    }

    // dùng để tạo
    public void create(Auction auction) throws DatabaseException {
        LocalDateTime createdAt = auction.getCreatedAt() == null ? TimeUtil.nowInVietnam() : auction.getCreatedAt();
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
            endTime,
            antiSnipingTriggerTime,
            antiSnipingExtensionTime,
            maxEndTime
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            auction.getEndTime().toString(),
            auction.getAntiSnipingTriggerTime() != null ? auction.getAntiSnipingTriggerTime().toMinutes() : 0,
            auction.getAntiSnipingExtensionTime() != null ? auction.getAntiSnipingExtensionTime().toMinutes() : 0,
            auction.getMaxEndTime() != null ? auction.getMaxEndTime().toString() : null
        );
    }

    // dùng để xóa bởi ID
    public void deleteById(String id) throws DatabaseException {
        String sql = "DELETE FROM Auctions WHERE id = ?";
        SQLiteHelper.update(sql, id);
    }

    // dùng để lưu
    public void save(Auction auction) throws DatabaseException {
        LocalDateTime createdAt = auction.getCreatedAt() == null ? TimeUtil.nowInVietnam() : auction.getCreatedAt();
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
            endTime = ?,
            antiSnipingTriggerTime = ?,
            antiSnipingExtensionTime = ?,
            maxEndTime = ?
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
            auction.getAntiSnipingTriggerTime() != null ? auction.getAntiSnipingTriggerTime().toMinutes() : 0,
            auction.getAntiSnipingExtensionTime() != null ? auction.getAntiSnipingExtensionTime().toMinutes() : 0,
            auction.getMaxEndTime() != null ? auction.getMaxEndTime().toString() : null,
            auction.getId()
        );
    }

    public List<Auction> findAll() throws DatabaseException {
        String sql = "SELECT * FROM Auctions ORDER BY createdAt DESC";
        return SQLiteHelper.query(sql, rs -> {
            List<Auction> auctions = new ArrayList<>();
            while (rs.next()) {
                auctions.add(mapRowToAuction(rs));
            }
            return auctions;
        });
    }

    // lấy tổng số tiền đã bid (nếu là đang là bidder cao nhất) của 1 user
    // là lockedBalance
    // dùng để tính tổng winning danh sách đặt giá cho người dùng
    public double sumWinningBidsForUser(String username) throws DatabaseException {
        String sql = "SELECT SUM(currentBid) FROM Auctions WHERE currentBidder = ? AND status IN ('ACTIVE', 'AWAITING_PAYMENT')";
        return SQLiteHelper.query(sql, rs -> {
            if (!rs.next()) return 0.0;
            return rs.getDouble(1);
        }, username);
    }
}
