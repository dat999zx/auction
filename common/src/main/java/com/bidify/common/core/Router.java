package com.bidify.common.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Router<K, H> {
    protected final Map<K, H> routes = new ConcurrentHashMap<>();

    // dùng để đăng ký
    public void register(K key, H handler) {
        if (key == null || handler == null) return;
        if (routes.putIfAbsent(key, handler) != null) {
            throw new IllegalStateException("Duplicate route registered for: " + key);
        }
    }

    // dùng để lấy trình xử lý
    protected H getHandler(K key) {
        if (key == null) return null;
        return routes.get(key);
    }

    // dùng để unregister
    public void unregister(K key) {
        if (key == null) return;
        routes.remove(key);
    }

    // dùng để kiểm tra xem có route
    public boolean hasRoute(K key) {
        return routes.containsKey(key);
    }

    // dùng để xóa sạch
    public void clear() {
        routes.clear();
    }
}
