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

        @Override
        public void onEvent(Event event) {
            count++;
        }
    }

    @Test
    void publishSubscribedObservers() { // test publish thì observer sẽ nhận được event
        GlobalChannel channel = new GlobalChannel();
        TestObserver observer1 = new TestObserver();
        TestObserver observer2 = new TestObserver();

        channel.subscribe(observer1);
        channel.subscribe(observer2);
        channel.publish(new Event(EventType.SERVER_NOTICE, "Test event"));

        assertEquals(1, observer1.count);
        assertEquals(1, observer2.count);
    }

    @Test
    void cleanupObservers() { // test clear thì observer sẽ không nhận được event nữa
        GlobalChannel channel = new GlobalChannel();
        TestObserver observer = new TestObserver();

        channel.subscribe(observer);
        channel.clear();
        channel.publish(new Event(EventType.SERVER_NOTICE, "Test event"));

        assertEquals(0, observer.count);
    }
}
