package com.bidify.server.model;

import java.time.LocalDateTime;

public abstract class Entity {
    private String id;
    private LocalDateTime createdAt;
    
    // dùng để tạo một đối tượng Entity
    protected Entity() {}
    // dùng để tạo một đối tượng Entity
    protected Entity(String id) { this.id = id; }
    // dùng để tạo một đối tượng Entity
    protected Entity(String id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    // dùng để lấy ID
    public String getId() { return id; }
    // dùng để thiết lập ID
    public void setId(String id) { this.id = id; }
    // dùng để lấy created tại
    public LocalDateTime getCreatedAt() { return createdAt; }
    // dùng để thiết lập created tại
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

