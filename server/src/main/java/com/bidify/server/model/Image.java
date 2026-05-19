package com.bidify.server.model;

import java.time.LocalDateTime;

public class Image extends Entity {
    private String filePath;

    // dùng để tạo một đối tượng Image
    public Image(String id, LocalDateTime createdAt, String filePath) {
        // dùng để super
        super(id, createdAt);
        this.filePath = filePath;
    }

    // dùng để lấy file path
    public String getFilePath() { return filePath; }
}
