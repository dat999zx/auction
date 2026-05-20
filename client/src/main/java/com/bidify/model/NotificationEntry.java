package com.bidify.model;

import java.time.LocalDateTime;

/**
 * Lớp đại diện cho một mục thông báo trong ứng dụng client.
 * Lớp này bất biến ngoại trừ trạng thái đã đọc (read).
 */
public class NotificationEntry {
    // Tiêu đề của thông báo
    private final String title;
    
    // Nội dung thông báo chi tiết
    private final String message;
    
    // Thời điểm tạo thông báo
    private final LocalDateTime createdAt;
    
    // Trạng thái đã đọc hay chưa
    private boolean read;

    /**
     * Khởi tạo một mục thông báo mới.
     *
     * @param title Tiêu đề của thông báo
     * @param message Nội dung của thông báo
     * @param createdAt Thời gian tạo
     */
    public NotificationEntry(String title, String message, LocalDateTime createdAt) {
        this.title = title == null || title.isBlank() ? "Notification" : title;
        this.message = message == null || message.isBlank() ? "You have a new update." : message;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.read = false; // Mặc định là chưa đọc khi mới tạo
    }

    // Lấy tiêu đề thông báo
    public String getTitle() { return title; }

    // Lấy nội dung chi tiết thông báo
    public String getMessage() { return message; }

    // Lấy thời gian tạo thông báo
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Kiểm tra xem thông báo đã được đọc chưa
    public boolean isRead() { return read; }

    // Đánh dấu thông báo này là đã đọc
    public void markRead() {
        read = true;
    }
}
