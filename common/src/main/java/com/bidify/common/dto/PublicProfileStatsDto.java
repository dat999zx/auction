package com.bidify.common.dto;

public class PublicProfileStatsDto {
    private int totalAuctions;
    private int activeAuctions;
    private int closedAuctions;
    private int soldAuctions;
    private int totalBids;
    private double activeVolume;
    private String sellRate;

    // dùng để tạo một đối tượng PublicProfileStatsDto
    public PublicProfileStatsDto() {}

    // dùng để tạo một đối tượng PublicProfileStatsDto
    public PublicProfileStatsDto(int totalAuctions, int activeAuctions, int closedAuctions, int soldAuctions, int totalBids, double activeVolume, String sellRate) {
        this.totalAuctions = totalAuctions;
        this.activeAuctions = activeAuctions;
        this.closedAuctions = closedAuctions;
        this.soldAuctions = soldAuctions;
        this.totalBids = totalBids;
        this.activeVolume = activeVolume;
        this.sellRate = sellRate;
    }

    public int getTotalAuctions() { return totalAuctions; }
    public int getActiveAuctions() { return activeAuctions; }
    public int getClosedAuctions() { return closedAuctions; }
    public int getSoldAuctions() { return soldAuctions; }
    public int getTotalBids() { return totalBids; }
    public double getActiveVolume() { return activeVolume; }
    public String getSellRate() { return sellRate; }
}
