package com.bidify.server.dao;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.model.Auction;
import com.bidify.server.contract.ImplementAuctionDao;
import com.bidify.server.database.SQLiteHelper;
import java.util.List;
import java.util.ArrayList;

import java.time.LocalDateTime;

// giao tiếp với SQLite database về bảng Auctions
public class AuctionDao implements ImplementAuctionDao{
    // tạm thêm cái này để về sau seller tìm lại các auction theo trạng thái của mình
    public List<Auction> findByStatus(AuctionStatus status) {
        String sql = "SELECT * FROM Auctions WHERE status = ?";
        return SQLiteHelper.query(sql, rs -> {
            List<Auction> auctions = new ArrayList<>();
            while (rs.next()) {
                Auction auction = new Auction(rs.getString("id"));
                auction.setAuctionName(rs.getString("auctionName"));
                auction.setDescription(rs.getString("description"));
                auction.setCategory(rs.getString("category"));
                auction.setProductType(rs.getString("type"));
                auction.setStartingPrice(rs.getDouble("startingPrice"));
                auction.setMinIncrement(rs.getDouble("minIncrement"));
                auction.setSellerUsername(rs.getString("seller"));
                auction.setCurrentBid(rs.getDouble("currentBid"));
                auction.setCurrentBidderUsername(rs.getString("currentBidder"));
                auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
                auction.setStartTime(LocalDateTime.parse(rs.getString("startAt")));
                auction.setEndTime(LocalDateTime.parse(rs.getString("endTime")));
                auctions.add(auction);
            }
            return auctions;
        }, status.toString());
    }

    public Auction findById(String id){ // lấy auction theo id
        String sql = "SELECT * FROM Auctions WHERE id = ?";
        return SQLiteHelper.query(sql, rs -> {
            if (!rs.next()) return null;
            Auction auction = new Auction(rs.getString("id"));
            auction.setAuctionName(rs.getString("auctionName"));
            auction.setDescription(rs.getString("description"));
            auction.setCategory(rs.getString("category"));
            auction.setProductType(rs.getString("type"));
            auction.setStartingPrice(rs.getDouble("startingPrice"));
            auction.setMinIncrement(rs.getDouble("minIncrement"));
            auction.setSellerUsername(rs.getString("seller"));
            auction.setCurrentBid(rs.getDouble("currentBid"));
            auction.setCurrentBidderUsername(rs.getString("currentBidder"));
            auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
            auction.setStartTime(LocalDateTime.parse(rs.getString("startAt")));
            auction.setEndTime(LocalDateTime.parse(rs.getString("endTime")));
            return auction;
        }, id);
    }

    public boolean create(Auction auction){ // tạo auction
        String sql = """
            INSERT INTO Auctions(
            id,
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
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
""";

        return SQLiteHelper.update(sql, auction.getId(), 
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
                                        auction.getEndTime().toString());
    }

    public boolean deleteById(String id){ // xóa auction theo id
        String sql = "DELETE FROM Auctions WHERE id = ?";
        return SQLiteHelper.update(sql, id);
    }

    public boolean save(Auction auction){ // lưu auction
        if (auction == null) return false;
        return SQLiteHelper.update(
            """
                                    UPDATE Auctions SET 
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
