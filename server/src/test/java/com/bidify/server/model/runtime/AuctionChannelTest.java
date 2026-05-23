package com.bidify.server.model.runtime;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.bidify.common.enums.EventType;
import com.bidify.common.model.Event;
import com.bidify.server.contract.Observer;

public class AuctionChannelTest {
    // observer test đơn giản để đếm số lần nhận event
    private class TestObserver implements Observer {
        private int count = 0;

        @Override
        public void onEvent(Event event) {
            count++;
        }
    }

    @Test
    void channelHasObserver() { // test channel có observer đã subscribed hay không
        AuctionChannel auctionChannel = new AuctionChannel("test");
        TestObserver observer = new TestObserver();

        auctionChannel.subscribe(observer);

        assertTrue(auctionChannel.hasObserver(observer));
    }

    @Test
    void unsubscribeShouldRemoveObserver() { // test unsubscribe thì observer sẽ không nhận được event nữa
        AuctionChannel channel = new AuctionChannel("test");
        TestObserver observer = new TestObserver();
        channel.subscribe(observer);

        channel.unsubscribe(observer);

        assertFalse(channel.hasObserver(observer));
    }

    @Test
    void publishSubscribedObservers() { // test publish thì observer sẽ nhận được event
        AuctionChannel channel = new AuctionChannel("test");
        TestObserver observer1 = new TestObserver();
        TestObserver observer2 = new TestObserver();

        channel.subscribe(observer1);
        channel.subscribe(observer2);
        channel.publish(new Event(EventType.BID_PLACED, "New bid"));

        assertEquals(1, observer1.count);
        assertEquals(1, observer2.count);
    }
}
