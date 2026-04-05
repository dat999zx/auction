package com.bidify.server.contract;

import com.bidify.common.model.Event;

// theo dõi 1 channel
// khi có Event mới sẽ được thông báo qua onEvent()
public interface Observer {
    void onEvent(Event event);
}
