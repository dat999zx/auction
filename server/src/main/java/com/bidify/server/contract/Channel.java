package com.bidify.server.contract;

import com.bidify.common.model.Event;

// Channel định nghĩa kênh phát sự kiện để các Observer đăng ký nhận thông báo realtime.
public interface Channel {
    void subscribe(Observer observer);    // đăng ký nhận tin
    void unsubscribe(Observer observer);  // hủy đăng ký
    void publish(Event event);            // phát tin đến tất cả người đăng ký
}
