package com.bidify.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

/**
 * Lớp kiểm thử (Unit Test) cho AuctionDtoAssembler.
 * Bình luận mã nguồn bằng tiếng Việt.
 */
public class AuctionDtoAssemblerTest {

    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final ImageDao imageDao = ImageDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    private AuctionDtoAssembler assembler;
    private final List<String> createdFilePaths = new ArrayList<>();

    @BeforeAll
    public static void setUpClass() {
        SQLiteHelper.init();
    }

    @BeforeEach
    public void setUp() {
        assembler = new AuctionDtoAssembler();
        cleanDb();
        RealtimeDatabase.clearAll();
        createdFilePaths.clear();
    }

    @AfterEach
    public void tearDown() {
        cleanDb();
        RealtimeDatabase.clearAll();
        // Xóa các file ảnh vật lý
        for (String fp : createdFilePaths) {
            try {
                Files.deleteIfExists(Paths.get(fp));
            } catch (IOException ignored) {}
        }
        createdFilePaths.clear();
    }

    private void cleanDb() {
        SQLiteHelper.update("DELETE FROM Bids");
        SQLiteHelper.update("DELETE FROM Users");
        SQLiteHelper.update("DELETE FROM ItemImageLinks");
        SQLiteHelper.update("DELETE FROM Items");
        SQLiteHelper.update("DELETE FROM Images");
        SQLiteHelper.update("DELETE FROM Auctions");
    }

    /**
     * Ca kiểm thử: Lắp ráp DTO thành công khi đấu giá chưa có ảnh nào.
     */
    @Test
    public void testToAuctionDtoWithoutImages() {
        String sellerName = "seller_dto";
        User seller = new User(sellerName, "Seller Dto", "pass");
        userDao.create(seller);

        Item item = new Item(sellerName, "Table", "Wooden", "Furniture", "Table");
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-dto-1", now, "Table Auction", "Desc", sellerName, item.getId(),
            null, 200.0, 20.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        auctionDao.create(auction);

        AuctionDto dto = assembler.toAuctionDto(auction, true);

        assertNotNull(dto);
        assertEquals("Table", dto.getAuctionName());
        assertNull(dto.getThumbnailBase64());
        assertTrue(dto.getGalleryBase64() == null || dto.getGalleryBase64().isEmpty());
        assertEquals(0, dto.getWatcherCount());
        assertEquals(0, dto.getActiveBidderCount());
    }

    /**
     * Ca kiểm thử: Chọn ảnh chính (isPrimary) làm thumbnail và lấy toàn bộ gallery.
     */
    @Test
    public void testToAuctionDtoWithImagesAndGallery() throws IOException {
        String sellerName = "seller_dto_img";
        User seller = new User(sellerName, "Seller Dto Img", "pass");
        userDao.create(seller);

        Item item = new Item(sellerName, "Laptop", "Desc", "Tech", "Laptop");
        itemDao.create(item);

        // Tạo 2 tệp ảnh giả lập
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        String imgId1 = "img-primary-" + UUID.randomUUID().toString().substring(0, 4);
        String imgId2 = "img-secondary-" + UUID.randomUUID().toString().substring(0, 4);
        
        Path path1 = tempDir.resolve(imgId1 + ".png");
        Path path2 = tempDir.resolve(imgId2 + ".png");
        
        Files.write(path1, new byte[]{1, 2, 3});
        Files.write(path2, new byte[]{4, 5, 6});
        
        createdFilePaths.add(path1.toString());
        createdFilePaths.add(path2.toString());

        Image img1 = new Image(imgId1, TimeUtil.nowInVietnam(), path1.toString());
        Image img2 = new Image(imgId2, TimeUtil.nowInVietnam(), path2.toString());
        
        imageDao.create(img1);
        imageDao.create(img2);

        // Lưu liên kết ảnh: Ảnh đầu tiên (img1) sẽ tự động là primary
        List<Image> images = new ArrayList<>();
        images.add(img1);
        images.add(img2);
        itemDao.saveItemImageLinks(item.getId(), images);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-dto-img", now, "Laptop Auction", "Desc", sellerName, item.getId(),
            null, 1000.0, 100.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );

        // Đóng gói DTO kèm gallery
        AuctionDto dto = assembler.toAuctionDto(auction, true);

        assertNotNull(dto);
        assertNotNull(dto.getThumbnailBase64(), "Thumbnail phải được tải");
        
        // Gallery phải chứa 2 ảnh
        assertNotNull(dto.getGalleryBase64());
        assertEquals(2, dto.getGalleryBase64().size());
    }

    /**
     * Ca kiểm thử: Tính toán số người xem (watcher count) và số bidder duy nhất (active bidder count).
     */
    @Test
    public void testWatcherAndActiveBidderCount() {
        String sellerName = "seller_dto_counts";
        User seller = new User(sellerName, "Seller Dto Counts", "pass");
        userDao.create(seller);

        Item item = new Item(sellerName, "Phone", "Desc", "Tech", "Phone");
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-dto-counts", now, "Phone Auction", "Desc", sellerName, item.getId(),
            null, 500.0, 50.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction);

        // Giả lập 2 observers xem phiên đấu giá
        TestClientHandler c1 = new TestClientHandler();
        c1.setCurrentUsername("user1");
        TestClientHandler c2 = new TestClientHandler();
        c2.setCurrentUsername("user2");
        
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), "user1");
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), "user2");

        // Giả lập bids từ 2 bidder khác nhau (user3 và user4), và một bid trùng lặp từ user3
        Bid bid1 = new Bid("bid-c-1", now, auction.getId(), "user3", 550.0, false);
        Bid bid2 = new Bid("bid-c-2", now.plusMinutes(1), auction.getId(), "user4", 600.0, false);
        Bid bid3 = new Bid("bid-c-3", now.plusMinutes(2), auction.getId(), "user3", 650.0, false);
        
        auction.addBid(bid1);
        auction.addBid(bid2);
        auction.addBid(bid3);

        AuctionDto dto = assembler.toAuctionDto(auction, false);

        assertNotNull(dto);
        // Watcher count: do channel chưa subscribe được nếu các user này chưa active trong RealtimeDatabase,
        // để thực hiện test watcher count, hãy add user vào RealtimeDatabase trước.
        // Hãy add active user
        User u1 = new User("user1", "U1", "pass");
        User u2 = new User("user2", "U2", "pass");
        userDao.create(u1);
        userDao.create(u2);
        RealtimeDatabase.addActiveUser(c1, u1);
        RealtimeDatabase.addActiveUser(c2, u2);
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), "user1");
        RealtimeDatabase.subscribeAuctionChannel(auction.getId(), "user2");

        dto = assembler.toAuctionDto(auction, false);
        assertEquals(2, dto.getWatcherCount(), "Có 2 người xem");
        assertEquals(2, dto.getActiveBidderCount(), "Có 2 bidder duy nhất (user3 và user4)");
    }

    private static class TestClientHandler extends ClientHandler {
        TestClientHandler() {
            super(null);
        }
        @Override
        public boolean isInSession() {
            return getCurrentUsername() != null;
        }
    }
}
