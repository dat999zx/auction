package com.bidify.server.contract;

import com.bidify.common.model.Event;

// Channel là nơi mà các Observer đăng ký để nhận thông báo khi có Event mới
public interface Channel {
    void subscribe(Observer observer); // đăng ký theo dõi
    void unsubscribe(Observer observer); // hủy theo dõi
    void publish(Event event); // thông báo cho tất cả các Observer đã đăng ký
}
