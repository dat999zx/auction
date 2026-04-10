package com.bidify.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.bidify.common.enums.EventType;
import com.bidify.common.model.Event;

// quản lý event đến các controller
// cách dùng vd:
// HubController: eventManager.subscribe(EventType.type1, this::doSomething)
// bất kì lúc nào có Event của type1 được publish() thì hàm doSomething trong HubController sẽ được gọi
// kí hiệu "::" nghĩa là con trỏ chỉ đến method doSomething của HubController mà KHÔNG TỰ GỌI, tương tự 1 dạng Object để lưu vào map, dùng để gọi sau bằng .accept()
public class EventManager {
    private static final EventManager instance = new EventManager(); // singleton
    private final Map<EventType, List<Consumer<Event>>> listeners = new ConcurrentHashMap<>();
    // listeners có dạng listeners[EventType] = [listener1, listener2, ...]
    // listener ở đây là các method của controller
    // Consumer<Event> nó giống như Runnable, có thể tạo ra Object của 1 method chưa thực thi ngay mà dùng để chạy sau
    // Runnable thì ko có tham số còn Consumer<T> sẽ nhận vào tham số T
    // vd: Consumer<Event> listener = this::doSomething; // Object của method (chưa được gọi ngay)
    // listener.accept(event); // gọi method từ Object này
    // tương đương là gọi this.doSomething(Event event)

    private EventManager() {}

    public static EventManager getInstance() {
        return instance;
    }

    // đăng kí lắng nghe event
    public void subscribe(EventType type, Consumer<Event> listener) {
        if (type == null || listener == null) return;
        listeners.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>()).add(listener);
    }

    // hủy đăng kí lắng nghe event
    public void unsubscribe(EventType type, Consumer<Event> listener) {
        if (type == null || listener == null) return;
        List<Consumer<Event>> handlers = listeners.get(type);
        if (handlers != null) handlers.remove(listener);
    }

    // thông báo event cho tất cả các listener đã đăng kí
    public void publish(Event event) {
        if (event == null || event.getType() == null) return;

        List<Consumer<Event>> handlers = listeners.get(event.getType());
        if (handlers == null) return;

        for (Consumer<Event> listener : handlers)
            listener.accept(event);
    }

    public void clear() {
        listeners.clear();
    }
}
