package com.bidify.common.dto;

public class PublicProfileStatsDto {
    private int totalAuctions;
    private int activeAuctions;
    private int closedAuctions;
    private int soldAuctions;
    private int totalBids;
    private double activeVolume;
    private String sellRate;
    private int completedSales;
    private int failedSales;
    private String completionRate;
    private String reputationLabel;
    private double starRating;
    private String starVisual;

    public PublicProfileStatsDto(
            int totalAuctions,
            int activeAuctions,
            int closedAuctions,
            int soldAuctions,
            int totalBids,
            double activeVolume,
            String sellRate,
            int completedSales,
            int failedSales,
            String completionRate,
            String reputationLabel,
            double starRating,
            String starVisual) {
        this.totalAuctions = totalAuctions;
        this.activeAuctions = activeAuctions;
        this.closedAuctions = closedAuctions;
        this.soldAuctions = soldAuctions;
        this.totalBids = totalBids;
        this.activeVolume = activeVolume;
        this.sellRate = sellRate;
        this.completedSales = completedSales;
        this.failedSales = failedSales;
        this.completionRate = completionRate;
        this.reputationLabel = reputationLabel;
        this.starRating = starRating;
        this.starVisual = starVisual;
    }

    public int getTotalAuctions() { return totalAuctions; }
    public int getActiveAuctions() { return activeAuctions; }
    public int getClosedAuctions() { return closedAuctions; }
    public int getSoldAuctions() { return soldAuctions; }
    public int getTotalBids() { return totalBids; }
    public double getActiveVolume() { return activeVolume; }
    public String getSellRate() { return sellRate; }
    public int getCompletedSales() { return completedSales; }
    public int getFailedSales() { return failedSales; }
    public String getCompletionRate() { return completionRate; }
    public String getReputationLabel() { return reputationLabel; }
    public double getStarRating() { return starRating; }
    public String getStarVisual() { return starVisual; }
}
