package com.bidify.server.contract;

import java.util.List;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.model.Auction;

public interface ImplementAuctionDao {
    List<Auction> findByStatus(AuctionStatus status);
    Auction findById(String id);
    boolean create(com.bidify.server.model.Auction auction);
    boolean deleteById(String id);
    boolean save(com.bidify.server.model.Auction auction);
}
