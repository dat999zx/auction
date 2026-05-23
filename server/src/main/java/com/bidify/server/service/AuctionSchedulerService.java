package com.bidify.server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.EventType;
import com.bidify.common.model.Event;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.model.Auction;

// Dịch vụ lập lịch cập nhật tự động trạng thái các phiên đấu giá theo thời gian thực.
public class AuctionSchedulerService {
    private static final long SYNC_INTERVAL_SECONDS = 1;
    private static final Logger logger = LoggerFactory.getLogger(AuctionSchedulerService.class);

    private static AuctionSchedulerService instance = new AuctionSchedulerService();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private AuctionSchedulerService() {}

    public static AuctionSchedulerService getInstance() { return instance; }

    public void start() {
        scheduler.scheduleAtFixedRate(this::syncRuntimeAuctions, 0, SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    // Đồng bộ trạng thái các phiên đấu giá UPCOMING/ACTIVE và chuyển trạng thái/settle nếu hết giờ.
    private void syncRuntimeAuctions() {
        List<Auction> runtimeAuctions = RealtimeDatabase.getAllRuntimeAuctions();
        LocalDateTime now = TimeUtil.nowInVietnam();

        for (Auction auction : runtimeAuctions) {
            if (auction == null) continue;

            AuctionStatus before = auction.getStatus();
            
            if (before == AuctionStatus.ACTIVE && now.isAfter(auction.getEndTime())) {
                logger.info("Auction expired: {} - {}. Settling...", auction.getAuctionName(), auction.getId());

                auctionService.settleAuction(auction);

                RealtimeDatabase.removeRuntimeAuction(auction.getId());
                publishStatusEvent(EventType.AUCTION_ENDED, "Auction ended", auction);
                continue;
            }

            if (before == AuctionStatus.UPCOMING && !now.isBefore(auction.getStartTime())) {
                auction.setStatus(AuctionStatus.ACTIVE);

                auctionDao.save(auction);
                
                logger.info("Auction started: {} - {}", auction.getAuctionName(), auction.getId());
                publishStatusEvent(EventType.AUCTION_UPDATED, "Auction is now active", auction);
            }
        }
    }

    private void publishStatusEvent(EventType eventType, String message, Auction auction) {
        AuctionDto auctionDto = auctionService.toAuctionDto(auction, false);
        RealtimeDatabase.getGlobalChannel().publish(new Event(eventType, message, auctionDto));
    }
}
