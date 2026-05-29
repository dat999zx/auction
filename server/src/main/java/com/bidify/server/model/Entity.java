package com.bidify.server.model;

import java.time.LocalDateTime;

// Class cha chung cho tất cả entity trong server — cung cấp id và thời gian tạo
public abstract class Entity {
    // ID định danh duy nhất của entity
    private String id;
    // Thời điểm tạo entity
    private LocalDateTime createdAt;
    
    protected Entity() {}
    protected Entity(String id) { this.id = id; }
    protected Entity(String id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

