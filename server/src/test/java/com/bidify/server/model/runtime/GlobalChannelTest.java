package com.bidify.server.model.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.bidify.common.enums.EventType;
import com.bidify.common.model.Event;
import com.bidify.server.contract.Observer;

public class GlobalChannelTest {
    // observer test đơn giản để đếm số lần nhận event
    private class TestObserver implements Observer {
        private int count = 0;

        // dùng để xử lý sự kiện sự kiện
        @Override
        public void onEvent(Event event) {
            count++;
        }
    }

    // dùng để phát sự kiện subscribed observers
    @Test
    void publishSubscribedObservers() { // test publish thì observer sẽ nhận được event
        GlobalChannel channel = new GlobalChannel();
        TestObserver observer1 = new TestObserver();
        TestObserver observer2 = new TestObserver();

        channel.subscribe(observer1);
        channel.subscribe(observer2);
        channel.publish(new Event(EventType.SERVER_NOTICE, "Test event"));

        // dùng để assert equals
        assertEquals(1, observer1.count);
        // dùng để assert equals
        assertEquals(1, observer2.count);
    }

    // dùng để dọn dẹp tài nguyên observers
    @Test
    void cleanupObservers() { // test clear thì observer sẽ không nhận được event nữa
        GlobalChannel channel = new GlobalChannel();
        TestObserver observer = new TestObserver();

        channel.subscribe(observer);
        channel.clear();
        channel.publish(new Event(EventType.SERVER_NOTICE, "Test event"));

        // dùng để assert equals
        assertEquals(0, observer.count);
    }
}
