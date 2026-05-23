package com.bidify.server.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Image;
import com.bidify.server.model.ItemImageLink;

public class ImageDao {
    private static final ImageDao instance = new ImageDao();

    private ImageDao() {}

    public static ImageDao getInstance() { return instance; }

    // Lưu thực thể Image mới vào database.
    public void create(Image image) throws DatabaseException {
        String sql = "INSERT INTO Images(id, createdAt, filePath) VALUES (?, ?, ?)";
        SQLiteHelper.update(sql, image.getId(), image.getCreatedAt().toString(), image.getFilePath());
    }

    public Image findById(String imageId) throws DatabaseException {
        String sql = "SELECT * FROM Images WHERE id = ?";
        return SQLiteHelper.query(sql, rs -> rs.next() ? mapImage(rs) : null, imageId);
    }

    public void deleteById(String imageId) throws DatabaseException {
        SQLiteHelper.update("DELETE FROM Images WHERE id = ?", imageId);
    }

    // Lưu mối liên kết giữa Item và Image vào database.
    public void createItemImageLink(ItemImageLink link) throws DatabaseException {
        String sql = "INSERT INTO ItemImageLinks(id, createdAt, itemId, imageId, displayOrder, isPrimary) VALUES (?, ?, ?, ?, ?, ?)";
        SQLiteHelper.update(
            sql,
            link.getId(),
            link.getCreatedAt().toString(),
            link.getItemId(),
            link.getImageId(),
            link.getDisplayOrder(),
            link.isPrimary() ? 1 : 0
        );
    }

    // Lấy danh sách liên kết ảnh của một Item, ưu tiên ảnh chính (isPrimary).
    public List<ItemImageLink> findItemLinks(String itemId) throws DatabaseException {
        String sql = "SELECT * FROM ItemImageLinks WHERE itemId = ? ORDER BY isPrimary DESC, displayOrder ASC, createdAt ASC";
        return SQLiteHelper.query(sql, rs -> {
            List<ItemImageLink> links = new ArrayList<>();
            while (rs.next())
                links.add(mapItemLink(rs));
            return links;
        }, itemId);
    }

    private Image mapImage(ResultSet rs) throws SQLException {
        return new Image(
            rs.getString("id"),
            LocalDateTime.parse(rs.getString("createdAt")),
            rs.getString("filePath")
        );
    }

    private ItemImageLink mapItemLink(ResultSet rs) throws SQLException {
        return new ItemImageLink(
            rs.getString("id"),
            LocalDateTime.parse(rs.getString("createdAt")),
            rs.getString("itemId"),
            rs.getString("imageId"),
            rs.getInt("displayOrder"),
            rs.getInt("isPrimary") == 1
        );
    }
}
