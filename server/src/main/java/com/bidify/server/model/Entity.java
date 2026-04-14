package com.bidify.server.model;

import java.time.LocalDateTime;

public abstract class Entity {
    private String id;
    private LocalDateTime createdAt;
    
    public Entity() {}
    public Entity(String id) { this.id = id; }
    public Entity(String id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

