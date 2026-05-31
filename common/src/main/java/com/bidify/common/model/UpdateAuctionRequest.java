package com.bidify.common.model;

// Dữ liệu gửi lên khi chỉnh sửa thông tin phiên đấu giá
public class UpdateAuctionRequest {
    // ID phiên đấu giá cần cập nhật
    private String auctionId;
    // Tên mới của phiên đấu giá
    private String auctionName;
    // Mô tả mới
    private String description;
    // Giá khởi điểm mới
    private double startingPrice;
    // Bước giá tối thiểu mới
    private double minIncrement;
    // Thời gian bắt đầu mới
    private String startTime;
    // Thời gian kết thúc mới
    private String endTime;
    // Tin nhắn thông báo gửi cho những người đang tham gia
    private String message;
    // Ngưỡng anti-sniping mới (HH:MM)
    private String triggerTime;
    // Thời gian gia hạn mới (HH:MM)
    private String extensionTime;
    // Tổng gia hạn tối đa mới (HH:MM)
    private String maxExtensionTime;

    public UpdateAuctionRequest(String auctionId, String auctionName, String description, double startingPrice, double minIncrement,
            String startTime, String endTime, String message, String triggerTime, String extensionTime, String maxExtensionTime) {
        this.auctionId = auctionId;
        this.auctionName = auctionName;
        this.description = description;
        this.startingPrice = startingPrice;
        this.minIncrement = minIncrement;
        this.startTime = startTime;
        this.endTime = endTime;
        this.message = message;
        this.triggerTime = triggerTime;
        this.extensionTime = extensionTime;
        this.maxExtensionTime = maxExtensionTime;
    }

    public String getAuctionId() { return auctionId; }

    public String getAuctionName() { return auctionName; }

    public String getDescription() { return description; }

    public double getStartingPrice() { return startingPrice; }

    public double getMinIncrement() { return minIncrement; }

    public String getStartTime() { return startTime; }

    public String getEndTime() { return endTime; }

    public String getMessage() { return message; }

    public String getTriggerTime() { return triggerTime; }

    public String getExtensionTime() { return extensionTime; }

    public String getMaxExtensionTime() { return maxExtensionTime; }
}
