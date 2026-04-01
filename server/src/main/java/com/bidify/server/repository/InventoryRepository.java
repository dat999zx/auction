package com.bidify.server.repository;

import java.util.ArrayList;
import java.util.List;

import com.bidify.common.model.InventoryItemSummary;
import com.bidify.server.database.DatabaseManager;
import com.bidify.server.model.Auction;
import com.bidify.server.model.InventoryItem;

public class InventoryRepository {
    public boolean existsByAuctionIdAndOwner(String auctionId, String ownerUsername) {
        Boolean exists = DatabaseManager.query(
            "SELECT id FROM Items WHERE auctionId = ? AND ownerUsername = ?",
            rs -> rs != null && rs.next(),
            auctionId,
            ownerUsername
        );
        return exists != null && exists;
    }

    public boolean saveWonAuction(Auction auction) {
        if (auction == null || auction.getCurrentBidder() == null) {
            return false;
        }

        String ownerUsername = auction.getCurrentBidder().getUsername();
        if (ownerUsername == null || ownerUsername.isBlank()) {
            return false;
        }

        if (existsByAuctionIdAndOwner(auction.getId(), ownerUsername)) {
            return true;
        }

        InventoryItem item = new InventoryItem(auction, ownerUsername);
        return DatabaseManager.update(
            """
            INSERT INTO Items(
                id,
                auctionId,
                ownerUsername,
                auctionName,
                description,
                sellerUsername,
                wonPrice,
                wonAt
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            item.getId(),
            item.getAuctionId(),
            item.getOwnerUsername(),
            item.getAuctionName(),
            item.getDescription(),
            item.getSellerUsername(),
            item.getWonPrice(),
            item.getWonAt()
        );
    }

    public List<InventoryItemSummary> findByOwnerUsername(String ownerUsername) {
        List<InventoryItemSummary> items = DatabaseManager.query(
            """
            SELECT id, auctionId, auctionName, description, sellerUsername, ownerUsername, wonPrice, wonAt
            FROM Items
            WHERE ownerUsername = ?
            ORDER BY wonAt DESC
            """,
            rs -> {
                List<InventoryItemSummary> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(new InventoryItemSummary(
                        rs.getString("id"),
                        rs.getString("auctionId"),
                        rs.getString("auctionName"),
                        rs.getString("description"),
                        rs.getString("sellerUsername"),
                        rs.getString("ownerUsername"),
                        rs.getDouble("wonPrice"),
                        rs.getString("wonAt")
                    ));
                }
                return rows;
            },
            ownerUsername
        );
        return items == null ? new ArrayList<>() : items;
    }
}
