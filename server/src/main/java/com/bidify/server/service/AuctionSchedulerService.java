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
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.model.Auction;

// tự động chạy, update status của auction sau các khoảng thời gian nhất định, đảm bảo rằng auction sẽ được cập nhật đúng status khi đến thời điểm bắt đầu hoặc kết thúc
public class AuctionSchedulerService {
    private static final long SYNC_INTERVAL_SECONDS = 1; // số giây mỗi lần đồng bộ
    private static final Logger logger = LoggerFactory.getLogger(AuctionSchedulerService.class);

    private static AuctionSchedulerService instance = new AuctionSchedulerService();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private AuctionSchedulerService() {}

    public static AuctionSchedulerService getInstance() { return instance; }

    public void start() {
        scheduler.scheduleAtFixedRate(this::syncRuntimeAuctions, 0, SYNC_INTERVAL_SECONDS, TimeUnit.SECONDS); // chạy method này mỗi SYNC_INTERVAL_SECONDS giây
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    // đồng bộ tất cả các auction đang chạy trong runtime với database, cập nhật status của chúng nếu cần thiết, và phát Event nếu có thay đổi status
    private void syncRuntimeAuctions() {
        List<Auction> runtimeAuctions = RealtimeDatabase.getAllRuntimeAuctions();
        LocalDateTime now = LocalDateTime.now();

        for (Auction auction : runtimeAuctions) {
            if (auction == null) continue;

            AuctionStatus before = auction.getStatus();
            
            // kiểm tra xem Auction đang ACTIVE đã hết giờ chưa
            if (before == AuctionStatus.ACTIVE && now.isAfter(auction.getEndTime())) {
                logger.info("Auction expired: {} - {}. Settling...", auction.getAuctionName(), auction.getId());

                auctionService.settleAuction(auction);

                RealtimeDatabase.removeRuntimeAuction(auction.getId());
                publishStatusEvent(EventType.AUCTION_ENDED, "Auction ended", auction);
                continue;
            }

            // kiểm tra các transition khác (UPCOMING -> ACTIVE)
            if (before == AuctionStatus.UPCOMING && !now.isBefore(auction.getStartTime())) {
                auction.setStatus(AuctionStatus.ACTIVE);

                auctionDao.save(auction);
                
                logger.info("Auction started: {} - {}", auction.getAuctionName(), auction.getId());
                publishStatusEvent(EventType.AUCTION_UPDATED, "Auction is now active", auction);
            }
        }
    }

    // phát Event về trạng thái mới của auction
    private void publishStatusEvent(EventType eventType, String message, Auction auction) {
        AuctionDto auctionDto = auctionService.toAuctionDto(auction, false);
        RealtimeDatabase.getGlobalChannel().publish(new Event(eventType, message, auctionDto));
    }
}
