package com.bidify.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.bidify.common.core.Router;
import com.bidify.common.enums.EventType;
import com.bidify.common.model.Event;

/**
 * Client-side event manager using the generic Router pattern.
 * Manages one-to-many event subscriptions.
 */
public class EventManager extends Router<EventType, List<Consumer<Event>>> {
    private static final EventManager instance = new EventManager();

    // dùng để tạo một đối tượng EventManager
    private EventManager() {}

    // dùng để lấy đối tượng Singleton
    public static EventManager getInstance() {
        return instance;
    }

    /**
     * Subscribe to an event type.
     * Uses atomic computeIfAbsent to ensure thread-safety for collection management.
     */
    // dùng để đăng ký lắng nghe sự kiện
    public void subscribe(EventType type, Consumer<Event> listener) {
        if (type == null || listener == null) return;
        routes.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Unsubscribe from an event type.
     */
    // dùng để hủy đăng ký lắng nghe sự kiện
    public void unsubscribe(EventType type, Consumer<Event> listener) {
        if (type == null || listener == null) return;
        List<Consumer<Event>> handlers = routes.get(type);
        if (handlers != null) {
            handlers.remove(listener);
        }
    }

    /**
     * Publish an event to all subscribers.
     */
    // dùng để phát sự kiện
    public void publish(Event event) {
        if (event == null || event.getType() == null) return;

        List<Consumer<Event>> handlers = routes.get(event.getType());
        if (handlers != null) {
            for (Consumer<Event> listener : handlers) {
                listener.accept(event);
            }
        }
    }
}
