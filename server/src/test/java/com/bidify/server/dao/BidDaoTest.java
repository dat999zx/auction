package com.bidify.server.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import com.bidify.common.utility.TimeUtil;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.Item;

class BidDaoTest {
    private BidDao bidDao = BidDao.getInstance();
    private AuctionDao auctionDao = AuctionDao.getInstance();
    private ItemDao itemDao = ItemDao.getInstance();
    private String testAuctionId;
    private String testBidId;
    private final List<String> createdItemIds = new ArrayList<>();

    // dùng để khởi tạo cơ sở dữ liệu
    @BeforeAll
    static void initDatabase() {
        // Khởi tạo SQLite schema trước khi chạy tất cả tests
        SQLiteHelper.init();
    }

    // dùng để thiết lập up
    @BeforeEach
    void setUp() throws Exception {
        // Tạo auction test trước mỗi test case
        Auction auction = new Auction(
            "Test Auction for Bids",
            "Test Description",
            "seller",
            1000.0,
            TimeUtil.nowInVietnam().plusMinutes(1),
            TimeUtil.nowInVietnam().plusHours(1)
        );
        Item item = createItem("seller", "Test Auction Item");
        auction.setItemId(item.getId());
        auctionDao.create(auction);
        testAuctionId = auction.getId();

        // Tạo bid test
        Bid bid = new Bid(testAuctionId, "bidder1", 1100.0);
        bidDao.create(bid);
        testBidId = bid.getId();
    }

    // dùng để tear down
    @AfterEach
    void tearDown() throws Exception {
        // Dọn dẹp bids và auction test sau mỗi test case
        if (testAuctionId != null) {
            // Xóa tất cả bids của auction test
            List<Bid> bids = bidDao.findByAuctionId(testAuctionId);
            for (Bid bid : bids) {
                bidDao.deleteById(bid.getId());
            }
            // Xóa auction
            auctionDao.deleteById(testAuctionId);
        }
        for (String itemId : createdItemIds)
            itemDao.deleteById(itemId);
        createdItemIds.clear();
    }

    // dùng để tạo and tìm kiếm danh sách đặt giá bởi đấu giá ID successfully
    @Test
    void createAndFindBidsByAuctionIdSuccessfully() throws Exception {
        // Bid đã được tạo trong setUp()

        // Tìm bids theo auction ID
        List<Bid> foundBids = bidDao.findByAuctionId(testAuctionId);

        // Kiểm tra có ít nhất 1 bid và bid đầu tiên khớp dữ liệu
        assertTrue(foundBids.size() >= 1);
        Bid firstBid = foundBids.get(0);
        assertEquals(testBidId, firstBid.getId());
        assertEquals(testAuctionId, firstBid.getAuctionId());
        assertEquals("bidder1", firstBid.getBidderUsername());
        assertEquals(1100.0, firstBid.getAmount());
    }

    // dùng để tìm kiếm danh sách đặt giá bởi đấu giá ID returns empty when no danh sách đặt giá
    @Test
    void findBidsByAuctionIdReturnsEmptyWhenNoBids() throws Exception {
        // Tạo auction mới không có bids
        Auction newAuction = new Auction(
            "Empty Auction",
            "No bids",
            "seller2",
            500.0,
            TimeUtil.nowInVietnam().plusMinutes(1),
            TimeUtil.nowInVietnam().plusHours(1)
        );
        Item item = createItem("seller2", "Empty Auction Item");
        newAuction.setItemId(item.getId());
        auctionDao.create(newAuction);

        // Tìm bids của auction mới
        List<Bid> bids = bidDao.findByAuctionId(newAuction.getId());

        // Danh sách phải rỗng
        assertTrue(bids.isEmpty());

        // Cleanup
        auctionDao.deleteById(newAuction.getId());
    }

    // dùng để tìm kiếm danh sách đặt giá bởi username returns correct danh sách đặt giá
    @Test
    void findBidsByUsernameReturnsCorrectBids() throws Exception {
        // Tạo thêm bid từ cùng bidder
        Bid anotherBid = new Bid(testAuctionId, "bidder1", 1200.0);
        bidDao.create(anotherBid);

        // Tìm bids theo username
        List<Bid> userBids = bidDao.findByUsername("bidder1");

        // Phải có ít nhất 2 bids từ bidder1
        assertTrue(userBids.size() >= 2);
        boolean foundTestBid = userBids.stream()
            .anyMatch(b -> b.getId().equals(testBidId));
        // dùng để assert true
        assertTrue(foundTestBid);

        // Cleanup
        bidDao.deleteById(anotherBid.getId());
    }

    // dùng để đặt giá ordered bởi số tiền ascending
    @Test
    void bidsOrderedByAmountAscending() throws Exception {
        // Tạo nhiều bids với amount khác nhau
        Bid lowBid = new Bid(testAuctionId, "bidder2", 1050.0);
        Bid highBid = new Bid(testAuctionId, "bidder3", 1300.0);

        bidDao.create(lowBid);
        bidDao.create(highBid);

        // Tìm bids theo auction ID
        List<Bid> bids = bidDao.findByAuctionId(testAuctionId);

        // Kiểm tra thứ tự tăng dần theo amount
        assertTrue(bids.size() >= 3);
        // Bid đầu tiên phải có amount nhỏ nhất
        double firstAmount = bids.get(0).getAmount();
        for (int i = 1; i < bids.size(); i++) {
            assertTrue(bids.get(i).getAmount() >= firstAmount);
            firstAmount = bids.get(i).getAmount();
        }

        // Cleanup
        bidDao.deleteById(lowBid.getId());
        bidDao.deleteById(highBid.getId());
    }

    // dùng để đặt giá ordered bởi thời gian descending
    @Test
    void bidsOrderedByTimeDescending() throws Exception {
        // Tạo bids với thời gian khác nhau
        Thread.sleep(10); // Đảm bảo thời gian khác nhau
        Bid olderBid = new Bid(testAuctionId, "bidder1", 1150.0);
        bidDao.create(olderBid);

        Thread.sleep(10);
        Bid newerBid = new Bid(testAuctionId, "bidder1", 1250.0);
        bidDao.create(newerBid);

        // Tìm bids theo username (order by bidTime DESC)
        List<Bid> userBids = bidDao.findByUsername("bidder1");

        // Kiểm tra thứ tự giảm dần theo thời gian (mới nhất trước)
        assertTrue(userBids.size() >= 3);
        // Bid đầu tiên phải là mới nhất
        LocalDateTime firstTime = userBids.get(0).getCreatedAt();
        for (int i = 1; i < userBids.size(); i++) {
            assertTrue(userBids.get(i).getCreatedAt().isBefore(firstTime) ||
                      userBids.get(i).getCreatedAt().equals(firstTime));
            firstTime = userBids.get(i).getCreatedAt();
        }

        // Cleanup
        bidDao.deleteById(olderBid.getId());
        bidDao.deleteById(newerBid.getId());
    }
    // dùng để tạo sản phẩm
    private Item createItem(String ownerUsername, String name) {
        Item item = new Item(ownerUsername, name, "Test Description", "General", "Electronics");
        itemDao.create(item);
        createdItemIds.add(item.getId());
        return item;
    }
}
