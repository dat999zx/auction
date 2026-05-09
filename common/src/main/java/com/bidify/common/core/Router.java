package com.bidify.common.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Router<K, H> {
    protected final Map<K, H> routes = new ConcurrentHashMap<>();

    public void register(K key, H handler) {
        if (key == null || handler == null) return;
        if (routes.putIfAbsent(key, handler) != null) {
            throw new IllegalStateException("Duplicate route registered for: " + key);
        }
    }

    protected H getHandler(K key) {
        if (key == null) return null;
        return routes.get(key);
    }

    public void unregister(K key) {
        if (key == null) return;
        routes.remove(key);
    }

    public boolean hasRoute(K key) {
        return routes.containsKey(key);
    }

    public void clear() {
        routes.clear();
    }
}
