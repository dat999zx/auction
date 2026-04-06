package com.bidify.server.model.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bidify.common.model.Event;
import com.bidify.server.contract.Channel;
import com.bidify.server.contract.Observer;

// kênh tổng của toàn hệ thống, chứa tất cả client observer
// dùng để gửi Event đến tất cả client
public class GlobalChannel implements Channel {
    private final Set<Observer> observers = ConcurrentHashMap.newKeySet();

    @Override
    public void subscribe(Observer observer) { observers.add(observer); }

    @Override
    public void unsubscribe(Observer observer) { observers.remove(observer); }

    @Override
    public void publish(Event event) {
        for (Observer observer : observers)
            observer.onEvent(event);
    }

    public void clear() { observers.clear(); }
}
