package com.bidify.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/*
Abstract base class for all entities in the system:
Mỗi đối tượng (User, Item, Auction...) đều có id, createdAt, updatedAt
 */
public abstract class Entity implements Serializable { // Có thể gửi qua mạng
    private static final long serialVersionUID = 1L; //ID để nhận biết version khi serialize/deserialize (tránh lỗi khi update class)
    
    protected String id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;
    
    public Entity() {
        this.id = generateId();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    protected String generateId() {
        return System.currentTimeMillis() + "_" + System.nanoTime(); // Tạo ID duy nhất từ thời gian
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
