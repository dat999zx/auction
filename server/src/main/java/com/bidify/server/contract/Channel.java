package com.bidify.server.contract;

import com.bidify.common.model.Event;

// Channel là nơi mà các Observer đăng ký để nhận thông báo khi có Event mới
public interface Channel {
    // dùng để đăng ký lắng nghe sự kiện
    void subscribe(Observer observer); // đăng ký theo dõi
    // dùng để hủy đăng ký lắng nghe sự kiện
    void unsubscribe(Observer observer); // hủy theo dõi
    // dùng để phát sự kiện
    void publish(Event event); // thông báo cho tất cả các Observer đã đăng ký
}
