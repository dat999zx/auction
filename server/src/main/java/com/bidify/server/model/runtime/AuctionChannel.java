package com.bidify.server.model.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bidify.common.model.Event;
import com.bidify.server.contract.Channel;
import com.bidify.server.contract.Observer;

public class AuctionChannel implements Channel {                         
    private final String auctionId;                                             // kênh này của phiên nào
    private final Set<Observer> observers = ConcurrentHashMap.newKeySet();      // danh sách người đang xem
    
    public AuctionChannel(String auctionId) { this.auctionId = auctionId; }
    public String getAuctionId() { return auctionId; }

    @Override
    public void subscribe(Observer observer) { observers.add(observer); }

    @Override
    public void unsubscribe(Observer observer) { observers.remove(observer); }

    @Override
    public void publish(Event event) {
        for (Observer observer : observers)
            observer.onEvent(event);
    }

    // → Tiện ích: kiểm tra ai đó có trong phòng không / đếm số người / dọn sạch khi server tắt
    public boolean hasObserver(Observer observer) {
        if (observer == null) return false;
        return observers.contains(observer);
    }

    public int getObserverCount() {
        return observers.size();
    }

    public void clear() { observers.clear(); }
}
