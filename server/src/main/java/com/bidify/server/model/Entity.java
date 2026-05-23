package com.bidify.server.model;

import java.time.LocalDateTime;

public abstract class Entity {
    private String id;
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

