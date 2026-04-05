package com.bidify.server.contract;

import java.util.List;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.model.Auction;

public interface ImplementAuctionDao {
    List<Auction> findByStatus(AuctionStatus status);
    Auction findById(String id);
    void create(com.bidify.server.model.Auction auction);
    void deleteById(String id);
    void save(com.bidify.server.model.Auction auction);
}
