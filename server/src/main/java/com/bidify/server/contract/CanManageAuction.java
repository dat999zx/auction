package com.bidify.server.contract;

public interface CanManageAuction {
    void closeAuction(String auctionId);
    void deleteAuction(String auctionId);
}
