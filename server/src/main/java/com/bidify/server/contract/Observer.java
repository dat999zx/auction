package com.bidify.server.contract;

import com.bidify.common.model.Event;

// Observer định nghĩa giao diện nhận thông báo sự kiện từ các kênh phát (Channel).
public interface Observer {
    void onEvent(Event event);
}
