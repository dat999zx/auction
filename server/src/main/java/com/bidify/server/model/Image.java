package com.bidify.server.model;

import java.time.LocalDateTime;

public class Image extends Entity {
    private String filePath;

    public Image(String id, LocalDateTime createdAt, String filePath) {
        super(id, createdAt);
        this.filePath = filePath;
    }

    public String getFilePath() { return filePath; }
}
