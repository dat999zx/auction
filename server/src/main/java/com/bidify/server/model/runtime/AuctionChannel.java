package com.bidify.server.model.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bidify.common.model.Event;
import com.bidify.server.contract.Channel;
import com.bidify.server.contract.Observer;

public class AuctionChannel implements Channel {
    private final String auctionId;
    private final Set<Observer> observers = ConcurrentHashMap.newKeySet();
    
    // dùng để tạo một đối tượng AuctionChannel
    public AuctionChannel(String auctionId) { this.auctionId = auctionId; }
    public String getAuctionId() { return auctionId; }

    // dùng để đăng ký lắng nghe sự kiện
    @Override
    public void subscribe(Observer observer) { observers.add(observer); }

    // dùng để hủy đăng ký lắng nghe sự kiện
    @Override
    public void unsubscribe(Observer observer) { observers.remove(observer); }

    // dùng để phát sự kiện
    @Override
    public void publish(Event event) {
        for (Observer observer : observers)
            observer.onEvent(event);
    }

    // dùng để kiểm tra xem có observer
    public boolean hasObserver(Observer observer) {
        if (observer == null) return false;
        return observers.contains(observer);
    }

    public int getObserverCount() {
        return observers.size();
    }

    // dùng để xóa sạch
    public void clear() { observers.clear(); }
}
