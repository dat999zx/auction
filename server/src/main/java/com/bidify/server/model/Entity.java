package com.bidify.server.model;

import java.time.LocalDateTime;

public abstract class Entity {

    protected String id;
    protected LocalDateTime createdAt;
    protected LocalDateTime updatedAt;

    public Entity() {
        this.id = generateId();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    protected String generateId() {
        return System.currentTimeMillis() + "_" + System.nanoTime();
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

