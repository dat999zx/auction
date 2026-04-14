package com.bidify.server.model;

import java.time.LocalDateTime;

import com.bidify.common.utility.IdGenerator;

public abstract class Item extends Entity {
    private String name, owner, imageUrl;

    public Item(String name, String owner, String imageUrl) {
        super(IdGenerator.genItemId(), LocalDateTime.now());
        this.name = name;
        this.owner = owner;
        this.imageUrl = imageUrl;
    }

    public Item(String id, LocalDateTime createdAt, String name, String owner, String imageUrl) {
        super(id, createdAt);
        this.name = name;
        this.owner = owner;
        this.imageUrl = imageUrl;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}

