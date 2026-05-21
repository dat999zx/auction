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

    // dùng để tạo một đối tượng ImageDao
    private ImageDao() {}

    // dùng để lấy đối tượng Singleton
    public static ImageDao getInstance() { return instance; }

    // dùng để tạo
    public void create(Image image) throws DatabaseException {
        String sql = "INSERT INTO Images(id, createdAt, filePath) VALUES (?, ?, ?)";
        SQLiteHelper.update(sql, image.getId(), image.getCreatedAt().toString(), image.getFilePath());
    }

    // dùng để tìm kiếm bởi ID
    public Image findById(String imageId) throws DatabaseException {
        String sql = "SELECT * FROM Images WHERE id = ?";
        return SQLiteHelper.query(sql, rs -> rs.next() ? mapImage(rs) : null, imageId);
    }

    // dùng để xóa bởi ID
    public void deleteById(String imageId) throws DatabaseException {
        SQLiteHelper.update("DELETE FROM Images WHERE id = ?", imageId);
    }

    // dùng để tạo sản phẩm hình ảnh link
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

    // dùng để tìm kiếm sản phẩm links
    public List<ItemImageLink> findItemLinks(String itemId) throws DatabaseException {
        String sql = "SELECT * FROM ItemImageLinks WHERE itemId = ? ORDER BY isPrimary DESC, displayOrder ASC, createdAt ASC";
        return SQLiteHelper.query(sql, rs -> {
            List<ItemImageLink> links = new ArrayList<>();
            while (rs.next())
                links.add(mapItemLink(rs));
            return links;
        }, itemId);
    }

    // dùng để chuyển đổi hình ảnh
    private Image mapImage(ResultSet rs) throws SQLException {
        return new Image(
            rs.getString("id"),
            LocalDateTime.parse(rs.getString("createdAt")),
            rs.getString("filePath")
        );
    }

    // dùng để chuyển đổi sản phẩm link
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
