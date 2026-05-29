package com.bidify.server.model;

import java.time.LocalDateTime;

// Ảnh lưu trên server — chỉ chứa đường dẫn file, không lưu nội dung ảnh trực tiếp
public class Image extends Entity {
    // Đường dẫn file ảnh trên đĩa cứng server
    private String filePath;

    public Image(String id, LocalDateTime createdAt, String filePath) {
        super(id, createdAt);
        this.filePath = filePath;
    }

    public String getFilePath() { return filePath; }
}
