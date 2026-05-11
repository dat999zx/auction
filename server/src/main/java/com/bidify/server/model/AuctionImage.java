package com.bidify.server.model;

import java.time.LocalDateTime;

public class AuctionImage extends Entity {
    private String auctionId;
    private String filePath;
    private boolean isPrimary;

    public AuctionImage(String id, LocalDateTime createdAt, String auctionId, String filePath, boolean isPrimary) {
        super(id, createdAt);
        this.auctionId = auctionId;
        this.filePath = filePath;
        this.isPrimary = isPrimary;
    }

    public String getAuctionId() { return auctionId; }
    public String getFilePath() { return filePath; }
    public boolean isPrimary() { return isPrimary; }
}
