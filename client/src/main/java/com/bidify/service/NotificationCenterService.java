package com.bidify.service;

import com.bidify.model.NotificationEntry;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Dịch vụ quản lý các thông báo trong bộ nhớ (in-memory) cho phiên làm việc hiện tại.
 * Hỗ trợ giới hạn số lượng thông báo tối đa và thông báo cho người lắng nghe (listeners) khi có thay đổi.
 */
public class NotificationCenterService {
    // Giới hạn số lượng thông báo mặc định
    private static final int DEFAULT_LIMIT = 30;
    
    // Thể hiện duy nhất (Singleton pattern) của dịch vụ
    private static final NotificationCenterService instance = new NotificationCenterService(DEFAULT_LIMIT);

    // Giới hạn số lượng thông báo cho đối tượng dịch vụ hiện tại
    private final int limit;
    
    // Danh sách lưu trữ các thông báo, được đồng bộ hóa khi truy cập
    private final List<NotificationEntry> notifications = new ArrayList<>();
    
    // Danh sách lưu trữ các hàm lắng nghe (listeners) khi có sự thay đổi thông báo
    private final List<Consumer<List<NotificationEntry>>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Khởi tạo dịch vụ với một giới hạn số lượng thông báo xác định.
     *
     * @param limit Số lượng thông báo tối đa được giữ lại trong bộ nhớ
     */
    public NotificationCenterService(int limit) {
        this.limit = Math.max(1, limit);
    }

    /**
     * Lấy thể hiện duy nhất của dịch vụ.
     *
     * @return Thể hiện duy nhất của NotificationCenterService
     */
    public static NotificationCenterService getInstance() {
        return instance;
    }

    /**
     * Thêm một thông báo mới vào đầu danh sách (thông báo mới nhất ở trên cùng).
     * Nếu vượt quá giới hạn cấu hình, thông báo cũ nhất (ở cuối) sẽ bị loại bỏ.
     *
     * @param title Tiêu đề của thông báo
     * @param message Nội dung chi tiết của thông báo
     */
    public synchronized void add(String title, String message) {
        // Thêm vào đầu danh sách (vị trí số 0) để thông báo mới nhất hiển thị trước
        notifications.add(0, new NotificationEntry(title, message, LocalDateTime.now()));
        
        // Loại bỏ các thông báo cũ vượt quá giới hạn cho phép
        while (notifications.size() > limit) {
            notifications.remove(notifications.size() - 1);
        }
        
        // Thông báo cho tất cả người lắng nghe biết có sự thay đổi
        notifyListeners();
    }

    /**
     * Lấy danh sách các thông báo hiện có dưới dạng không thể chỉnh sửa trực tiếp.
     *
     * @return Danh sách thông báo bất biến
     */
    public synchronized List<NotificationEntry> getNotifications() {
        return Collections.unmodifiableList(new ArrayList<>(notifications));
    }

    /**
     * Lấy số lượng thông báo chưa đọc.
     *
     * @return Số lượng thông báo chưa đọc
     */
    public synchronized int getUnreadCount() {
        int count = 0;
        for (NotificationEntry entry : notifications) {
            if (!entry.isRead()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Đánh dấu toàn bộ thông báo hiện tại là đã đọc.
     */
    public synchronized void markAllRead() {
        for (NotificationEntry entry : notifications) {
            entry.markRead();
        }
        // Cập nhật lại UI thông qua listeners
        notifyListeners();
    }

    /**
     * Đăng ký một hàm lắng nghe sự thay đổi của danh sách thông báo.
     *
     * @param listener Hàm callback nhận danh sách thông báo mới
     */
    public void addListener(Consumer<List<NotificationEntry>> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Hủy đăng ký hàm lắng nghe sự thay đổi.
     *
     * @param listener Hàm callback cần hủy
     */
    public void removeListener(Consumer<List<NotificationEntry>> listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Xóa sạch toàn bộ thông báo trong bộ nhớ.
     */
    public synchronized void clear() {
        notifications.clear();
        notifyListeners();
    }

    /**
     * Kích hoạt gọi các hàm lắng nghe sự thay đổi của thông báo với một bản chụp dữ liệu hiện tại.
     */
    private void notifyListeners() {
        List<NotificationEntry> snapshot = getNotifications();
        for (Consumer<List<NotificationEntry>> listener : listeners) {
            listener.accept(snapshot);
        }
    }
}
