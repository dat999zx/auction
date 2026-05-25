package com.bidify.server.contract;

import com.bidify.common.model.Event;

// Channel định nghĩa kênh phát sự kiện để các Observer đăng ký nhận thông báo realtime.
public interface Channel {
    void subscribe(Observer observer);
    void unsubscribe(Observer observer);
    void publish(Event event);
}
