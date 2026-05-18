package com.bidify.server.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.bidify.common.enums.ItemStatus;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.ItemImageLink;

public class ItemDao {
    private static ItemDao instance = new ItemDao();

    private ItemDao() {}

    public static ItemDao getInstance() { return instance; }

    public void create(Item item) throws DatabaseException {
        LocalDateTime createdAt = item.getCreatedAt() == null ? LocalDateTime.now() : item.getCreatedAt();
        String sql = """
            INSERT INTO Items(
                id,
                createdAt,
                ownerUsername,
                name,
                description,
                category,
                productType,
                availabilityStatus
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        SQLiteHelper.update(
            sql,
            item.getId(),
            createdAt.toString(),
            item.getOwnerUsername(),
            item.getName(),
            item.getDescription(),
            item.getCategory(),
            item.getProductType(),
            item.getAvailabilityStatus().toString()
        );
    }

    public Item findById(String id) throws DatabaseException {
        String sql = "SELECT * FROM Items WHERE id = ?";
        return SQLiteHelper.query(sql, rs -> rs.next() ? mapItem(rs) : null, id);
    }

    public List<Item> findByOwnerUsername(String ownerUsername) throws DatabaseException {
        String sql = "SELECT * FROM Items WHERE ownerUsername = ? ORDER BY createdAt DESC";
        return findMany(sql, ownerUsername);
    }

    public List<Item> findByAvailabilityStatus(ItemStatus availabilityStatus) throws DatabaseException {
        String sql = "SELECT * FROM Items WHERE availabilityStatus = ? ORDER BY createdAt DESC";
        return findMany(sql, availabilityStatus.toString());
    }

    public List<Item> findByOwnerUsernameAndAvailabilityStatus(String ownerUsername, ItemStatus availabilityStatus) throws DatabaseException {
        String sql = """
            SELECT * FROM Items
            WHERE ownerUsername = ? AND availabilityStatus = ?
            ORDER BY createdAt DESC
            """;
        return findMany(sql, ownerUsername, availabilityStatus.toString());
    }

    public void save(Item item) throws DatabaseException {
        LocalDateTime createdAt = item.getCreatedAt() == null ? LocalDateTime.now() : item.getCreatedAt();
        String sql = """
            UPDATE Items SET
                createdAt = ?,
                ownerUsername = ?,
                name = ?,
                description = ?,
                category = ?,
                productType = ?,
                availabilityStatus = ?
            WHERE id = ?
            """;

        SQLiteHelper.update(
            sql,
            createdAt.toString(),
            item.getOwnerUsername(),
            item.getName(),
            item.getDescription(),
            item.getCategory(),
            item.getProductType(),
            item.getAvailabilityStatus().toString(),
            item.getId()
        );
    }

    public void updateAvailabilityStatus(String itemId, ItemStatus availabilityStatus) throws DatabaseException {
        String sql = "UPDATE Items SET availabilityStatus = ? WHERE id = ?";
        SQLiteHelper.update(sql, availabilityStatus.toString(), itemId);
    }

    public void deleteById(String id) throws DatabaseException {
        SQLiteHelper.update("DELETE FROM Items WHERE id = ?", id);
    }

    public void saveItemImageLinks(String itemId, List<Image> images) throws DatabaseException {
        String sql = "INSERT INTO ItemImageLinks(id, createdAt, itemId, imageId, displayOrder, isPrimary) VALUES (?, ?, ?, ?, ?, ?)";
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            SQLiteHelper.update(
                sql,
                UUID.randomUUID().toString().substring(0, 12),
                LocalDateTime.now().toString(),
                itemId,
                image.getId(),
                i,
                i == 0 ? 1 : 0
            );
        }
    }

    public List<ItemImageLink> getItemImageLinks(String itemId) throws DatabaseException {
        String sql = "SELECT * FROM ItemImageLinks WHERE itemId = ? ORDER BY isPrimary DESC, displayOrder ASC, createdAt ASC";
        return SQLiteHelper.query(sql, rs -> {
            List<ItemImageLink> images = new ArrayList<>();
            while (rs.next()) {
                images.add(new ItemImageLink(
                    rs.getString("id"),
                    LocalDateTime.parse(rs.getString("createdAt")),
                    rs.getString("itemId"),
                    rs.getString("imageId"),
                    rs.getInt("displayOrder"),
                    rs.getInt("isPrimary") == 1
                ));
            }
            return images;
        }, itemId);
    }

    public void deleteItemImageLinks(String itemId) throws DatabaseException {
        SQLiteHelper.update("DELETE FROM ItemImageLinks WHERE itemId = ?", itemId);
    }

    private List<Item> findMany(String sql, Object... params) throws DatabaseException {
        return SQLiteHelper.query(sql, rs -> {
            List<Item> items = new ArrayList<>();
            while (rs.next())
                items.add(mapItem(rs));
            return items;
        }, params);
    }

    private Item mapItem(ResultSet rs) throws SQLException {
        return new Item(
            rs.getString("id"),
            LocalDateTime.parse(rs.getString("createdAt")),
            rs.getString("ownerUsername"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("category"),
            rs.getString("productType"),
            ItemStatus.valueOf(rs.getString("availabilityStatus"))
        );
    }
}
