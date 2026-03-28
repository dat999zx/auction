package com.bidify.server.contract;

import com.bidify.common.model.Event;

public interface CanManageSystem {
    void sendEvent(Event event);
    void sendEvent(String username, Event event);
}
