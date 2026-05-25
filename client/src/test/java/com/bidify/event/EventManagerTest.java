package com.bidify.event;

import com.bidify.common.enums.EventType;
import com.bidify.common.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class EventManagerTest {
    private EventManager eventManager;

    @BeforeEach
    void setup() { // reset sau mỗi test
        eventManager = EventManager.getInstance();
        eventManager.clear();
    }

    @Test
    void publishToListeners() { // gửi event đến 1 listener đã đăng kí
        AtomicInteger counter = new AtomicInteger(0);

        eventManager.subscribe(EventType.BID_PLACED, event -> counter.incrementAndGet());

        Event event = new Event(EventType.BID_PLACED, "Bid placed");
        eventManager.publish(event);

        assertEquals(1, counter.get());
    }

    @Test
    void unsubscribeFromEvent() { // hủy đăng kí lắng nghe event
        AtomicInteger counter = new AtomicInteger(0);

        Consumer<Event> listener = event -> counter.incrementAndGet();

        eventManager.subscribe(EventType.BID_PLACED, listener);
        eventManager.unsubscribe(EventType.BID_PLACED, listener);

        Event event = new Event(EventType.BID_PLACED, "Bid placed");
        eventManager.publish(event);

        assertEquals(0, counter.get());
    }

    @Test
    void publishNotifyCorrectType() { // gửi chỉ gửi event tới người đã đăng kí đúng event type
        AtomicInteger counter = new AtomicInteger(0);

        eventManager.subscribe(EventType.AUCTION_UPDATED, event -> counter.incrementAndGet());

        Event event = new Event(EventType.BID_PLACED, "Bid placed");
        eventManager.publish(event);

        assertEquals(0, counter.get());
    }

    @Test
    void publishToAllListeners() { // gửi event đến tất cả các listener đã đăng kí
        AtomicInteger counter = new AtomicInteger(0);

        eventManager.subscribe(EventType.BID_PLACED, event -> counter.incrementAndGet());
        eventManager.subscribe(EventType.BID_PLACED, event -> counter.incrementAndGet());

        Event event = new Event(EventType.BID_PLACED, "Bid placed");
        eventManager.publish(event);

        assertEquals(2, counter.get());
    }
}
