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

        // dùng để xử lý sự kiện sự kiện
        @Override
        public void onEvent(Event event) {
            count++;
        }
    }

    // dùng để channel kiểm tra xem có observer
    @Test
    void channelHasObserver() { // test channel có observer đã subscribed hay không
        AuctionChannel auctionChannel = new AuctionChannel("test");
        TestObserver observer = new TestObserver();

        auctionChannel.subscribe(observer);

        assertTrue(auctionChannel.hasObserver(observer));
    }

    // dùng để hủy đăng ký lắng nghe sự kiện should xóa observer
    @Test
    void unsubscribeShouldRemoveObserver() { // test unsubscribe thì observer sẽ không nhận được event nữa
        AuctionChannel channel = new AuctionChannel("test");
        TestObserver observer = new TestObserver();
        channel.subscribe(observer);

        channel.unsubscribe(observer);

        assertFalse(channel.hasObserver(observer));
    }

    // dùng để phát sự kiện subscribed observers
    @Test
    void publishSubscribedObservers() { // test publish thì observer sẽ nhận được event
        AuctionChannel channel = new AuctionChannel("test");
        TestObserver observer1 = new TestObserver();
        TestObserver observer2 = new TestObserver();

        channel.subscribe(observer1);
        channel.subscribe(observer2);
        channel.publish(new Event(EventType.BID_PLACED, "New bid"));

        // dùng để assert equals
        assertEquals(1, observer1.count);
        // dùng để assert equals
        assertEquals(1, observer2.count);
    }
}
