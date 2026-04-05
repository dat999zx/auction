package com.bidify.server.model.runtime;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bidify.common.model.Event;
import com.bidify.server.contract.Channel;
import com.bidify.server.contract.Observer;

public class AuctionChannel implements Channel {
    private final String auctionId;
    private final Set<Observer> observers = ConcurrentHashMap.newKeySet();
    
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

    public boolean hasObserver(Observer observer) {
        if (observer == null) return false;
        return observers.contains(observer);
    }
}
