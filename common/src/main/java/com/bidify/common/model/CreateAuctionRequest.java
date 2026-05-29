package com.bidify.common.model;

// Dữ liệu gửi lên khi tạo phiên đấu giá mới
public class CreateAuctionRequest {
    // Username người bán tạo đấu giá
    private String seller;
    // ID vật phẩm đem ra đấu giá
    private String itemId;
    // Giá khởi điểm
    private double startingPrice;
    // Bước giá tối thiểu mỗi lần đặt
    private double minIncrement;
    // Thời gian bắt đầu phiên (format chuỗi)
    private String startTime;
    // Thời gian kết thúc phiên (format chuỗi)
    private String endTime;
    // Ngưỡng thời gian kích hoạt anti-sniping (HH:MM)
    private String triggerTime;
    // Thời gian gia hạn mỗi lần bị kích hoạt anti-sniping (HH:MM)
    private String extensionTime;
    // Tổng thời gian gia hạn tối đa (HH:MM)
    private String maxExtensionTime;

    public CreateAuctionRequest(
        String seller,
        String itemId,
        double startingPrice,
        double minIncrement,
        String startTime,
        String endTime,
        String triggerTime,
        String extensionTime,
        String maxExtensionTime
    ) {
        this.seller = seller;
        this.itemId = itemId;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.triggerTime = triggerTime;
        this.extensionTime = extensionTime;
        this.maxExtensionTime = maxExtensionTime;
    }

    public String getSeller(){ return seller; }

    public String getItemId() { return itemId; }

    public double getStartingPrice() { return startingPrice; }

    public double getMinIncrement() { return minIncrement; }

    public String getEndTime() { return endTime; }

    public String getStartTime() { return startTime; }

    public String getTriggerTime() { return triggerTime; }

    public String getExtensionTime() { return extensionTime; }

    public String getMaxExtensionTime() { return maxExtensionTime; }
}
