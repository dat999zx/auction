package com.bidify.server.service.profile;

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

import com.bidify.common.dto.PublicProfileDto;
import com.bidify.common.dto.PublicProfileStatsDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
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
import com.bidify.server.service.ImageService;
import com.bidify.server.service.auction.AuctionDtoAssembler;

/**
 * Lớp kiểm thử (Unit Test) cho PublicProfileAssembler.
 * Đảm bảo các bình luận sử dụng tiếng Việt.
 */
public class PublicProfileAssemblerTest {

    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final BidDao bidDao = BidDao.getInstance();
    private final ImageDao imageDao = ImageDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    private final ImageService imageService = ImageService.getInstance();
    private final AuctionDtoAssembler auctionDtoAssembler = new AuctionDtoAssembler();

    private PublicProfileAssembler assembler;
    
    // Theo dõi các file ảnh tạm được tạo để xóa sau mỗi ca test
    private final List<String> createdFilePaths = new ArrayList<>();

    @BeforeAll
    public static void setUpClass() {
        SQLiteHelper.init();
    }

    @BeforeEach
    public void setUp() {
        assembler = new PublicProfileAssembler(
                auctionDao,
                bidDao,
                imageDao,
                imageService,
                auctionDtoAssembler
        );
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
        SQLiteHelper.update("DELETE FROM Transactions");
        SQLiteHelper.update("DELETE FROM Auctions");
        SQLiteHelper.update("DELETE FROM ItemImageLinks");
        SQLiteHelper.update("DELETE FROM Items");
        SQLiteHelper.update("DELETE FROM Users");
        SQLiteHelper.update("DELETE FROM Images");
    }

    /**
     * Ca kiểm thử: Người dùng không có đấu giá và không có bids nào.
     * Xác thực: Kết quả trả về stats có các chỉ số đều bằng 0, không lỗi.
     */
    @Test
    public void testAssembleWithNoAuctionsAndNoBids() {
        String username = "no_auc_user";
        User user = new User(username, "No Auc Nick", "pass");
        userDao.create(user);

        PublicProfileDto dto = assembler.assemble(user);

        assertNotNull(dto);
        assertEquals(username, dto.getUsername());
        assertEquals("No Auc Nick", dto.getNickname());
        assertNull(dto.getProfileImageBase64());
        
        PublicProfileStatsDto stats = dto.getStats();
        assertNotNull(stats);
        assertEquals(0, stats.getTotalAuctions());
        assertEquals(0, stats.getActiveAuctions());
        assertEquals(0, stats.getClosedAuctions());
        assertEquals(0, stats.getSoldAuctions());
        assertEquals(0, stats.getTotalBids());
        assertEquals(0.0, stats.getActiveVolume(), 0.001);
        assertEquals("0.0%", stats.getSellRate());
        assertEquals("New Seller", stats.getReputationLabel());
    }

    /**
     * Ca kiểm thử: Người dùng có đấu giá đang hoạt động (ACTIVE) và đấu giá đã kết thúc.
     * Xác thực:
     * - Tính toán Active Volume (thể tích hoạt động) đúng dựa trên startingPrice hoặc currentBid.
     * - Tính toán số lượng closedAuctions và soldAuctions chính xác.
     */
    @Test
    public void testAssembleWithAuctionsAndStats() {
        String sellerName = "seller_stats";
        User seller = new User(sellerName, "Seller Stats", "pass");
        userDao.create(seller);

        // Tạo 3 sản phẩm tương ứng cho 3 đấu giá
        Item item1 = new Item(sellerName, "Item 1", "Desc", "Cat", "Brand");
        Item item2 = new Item(sellerName, "Item 2", "Desc", "Cat", "Brand");
        Item item3 = new Item(sellerName, "Item 3", "Desc", "Cat", "Brand");
        itemDao.create(item1);
        itemDao.create(item2);
        itemDao.create(item3);

        LocalDateTime now = TimeUtil.nowInVietnam();

        // 1. Đấu giá ACTIVE chưa có ai bid (dùng startingPrice = 500)
        Auction activeNoBids = new Auction(
            "auc-active-nobid", now, "Title 1", "Desc", sellerName, item1.getId(),
            null, 500.0, 50.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        auctionDao.create(activeNoBids);

        // 2. Đấu giá ACTIVE đã có bid (dùng currentBid = 1200)
        Auction activeWithBids = new Auction(
            "auc-active-bid", now, "Title 2", "Desc", sellerName, item2.getId(),
            "bidder1", 1000.0, 100.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        activeWithBids.setCurrentBid(1200.0);
        auctionDao.create(activeWithBids);

        // 3. Đấu giá CLOSED (COMPLETED) đã bán thành công
        Auction closedSold = new Auction(
            "auc-closed-sold", now, "Title 3", "Desc", sellerName, item3.getId(),
            "bidder2", 2000.0, 200.0, now.minusDays(2), now.minusDays(1), AuctionStatus.COMPLETED
        );
        closedSold.setCurrentBid(2500.0);
        auctionDao.create(closedSold);

        // Giả lập 2 lượt bid của chính seller này tại đấu giá khác để tăng totalBids
        Bid bid1 = new Bid("bid-1", now, "other-auc-1", sellerName, 300.0, false);
        Bid bid2 = new Bid("bid-2", now, "other-auc-2", sellerName, 400.0, false);
        bidDao.create(bid1);
        bidDao.create(bid2);

        // Thực hiện lắp ráp profile công khai
        PublicProfileDto dto = assembler.assemble(seller);

        assertNotNull(dto);
        PublicProfileStatsDto stats = dto.getStats();
        assertNotNull(stats);
        
        // Thống kê đấu giá
        assertEquals(3, stats.getTotalAuctions(), "Tổng số đấu giá phải là 3");
        assertEquals(2, stats.getActiveAuctions(), "Đấu giá active phải là 2");
        assertEquals(1, stats.getClosedAuctions(), "Đấu giá closed phải là 1 (không tính UPCOMING hay ACTIVE)");
        assertEquals(1, stats.getSoldAuctions(), "Đấu giá sold phải là 1");
        assertEquals("100.0%", stats.getSellRate(), "Tỷ lệ bán hàng phải là 100.0%");

        // Active Volume: 500 (starting price của đấu giá 1) + 1200 (current bid của đấu giá 2) = 1700
        assertEquals(1700.0, stats.getActiveVolume(), 0.001, "Thể tích hoạt động phải là 1700.0");

        // Tổng số bids người này đặt ở các cuộc đấu giá khác
        assertEquals(2, stats.getTotalBids(), "Tổng số bids đã đặt phải là 2");
        
        // Uy tín của người bán
        assertEquals(1, stats.getCompletedSales());
        assertEquals(0, stats.getFailedSales());
        assertEquals("100.0%", stats.getCompletionRate());
        assertEquals("New Seller", stats.getReputationLabel()); // Dưới 3 giao dịch đánh giá thì vẫn là New Seller
    }

    /**
     * Ca kiểm thử: Người dùng có hình ảnh đại diện.
     * Xác thực: Đọc thành công file ảnh qua ImageService và trả về chuỗi Base64.
     */
    @Test
    public void testAssembleWithProfileImage() throws IOException {
        String username = "img_user";
        User user = new User(username, "Img Nick", "pass");
        
        // Ghi một file ảnh giả lập
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
        String imageId = "img-" + UUID.randomUUID().toString().substring(0, 8);
        Path imagePath = tempDir.resolve(imageId + ".png");
        byte[] fakeBytes = new byte[]{1, 2, 3, 4};
        Files.write(imagePath, fakeBytes);
        createdFilePaths.add(imagePath.toString());

        // Tạo bản ghi Image trong DB
        Image image = new Image(imageId, TimeUtil.nowInVietnam(), imagePath.toString());
        imageDao.create(image);

        // Gán ảnh đại diện cho user
        user.setProfileImageId(imageId);
        userDao.create(user);

        // Lắp ráp profile
        PublicProfileDto dto = assembler.assemble(user);

        assertNotNull(dto);
        assertNotNull(dto.getProfileImageBase64(), "Chuỗi Base64 ảnh đại diện không được null");
        assertFalse(dto.getProfileImageBase64().isBlank(), "Chuỗi Base64 không được trống");
    }

    /**
     * Ca kiểm thử: Xử lý ngoại lệ an toàn khi đường dẫn ảnh đại diện bị hỏng hoặc lỗi DB.
     * Xác thực: Trả về profileImageBase64 là null thay vì ném lỗi crash chương trình.
     */
    @Test
    public void testAssembleWithCorruptedProfileImage() {
        String username = "corrupted_user";
        User user = new User(username, "Corrupted Nick", "pass");
        user.setProfileImageId("non-existent-image-id");
        userDao.create(user);

        // Không tạo bản ghi Image trong DB -> sẽ gây ra việc tìm kiếm trả về null
        PublicProfileDto dto = assembler.assemble(user);

        assertNotNull(dto);
        assertNull(dto.getProfileImageBase64(), "Nếu ảnh đại diện không tồn tại trong DB, base64 trả về phải là null");
    }
}
