package com.bidify.common.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// chuyển hướng các loại Key K về các Handler H
public abstract class Router<K, H> {
    protected final Map<K, H> routes = new ConcurrentHashMap<>();

    public void subscribe(K key, H handler) {
        if (key == null || handler == null) return;
        routes.put(key, handler);
    }

    public void unsubscribe(K key) {
        if (key == null) return;
        routes.remove(key);
    }

    public void clear() {
        routes.clear();
    }
}
