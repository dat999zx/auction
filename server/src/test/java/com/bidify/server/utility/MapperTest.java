package com.bidify.server.utility;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.dto.BidDto;
import com.bidify.common.dto.ItemDto;
import com.bidify.common.dto.UserDto;
import com.bidify.common.dto.WalletDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.ItemStatus;
import com.bidify.common.enums.UserRole;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.model.Wallet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử cho các Mappers trong Bidify.
 * Đảm bảo các đối tượng Server Model được chuyển đổi chính xác sang Client DTOs.
 */
public class MapperTest {

    private static Path tempAvatarPath;

    @BeforeAll
    public static void setUpClass() throws Exception {
        SQLiteHelper.init();
        // Tạo đường dẫn tạm cho avatar kiểm thử
        Path uploadsDir = Paths.get("server", "uploads");
        Files.createDirectories(uploadsDir);
        tempAvatarPath = uploadsDir.resolve("temp_test_avatar_" + UUID.randomUUID().toString() + ".png").toAbsolutePath();
        Files.write(tempAvatarPath, "dummy image bytes data".getBytes());
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
        if (tempAvatarPath != null) {
            Files.deleteIfExists(tempAvatarPath);
        }
    }

    @BeforeEach
    public void setUp() {
        cleanDb();
    }

    @AfterEach
    public void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        try {
            UserDao.getInstance().deleteByUsername("test_user_mapper");
            ImageDao.getInstance().deleteById("img_test_avatar");
        } catch (Exception e) {
            // bỏ qua lỗi dọn dẹp
        }
    }

    @Test
    public void testWalletMapper() {
        // Test null wallet
        assertNull(WalletMapper.toDto(null), "Null Wallet phải được map sang null Dto");

        // Test mapping hợp lệ
        Wallet wallet = new Wallet(200000.5);
        wallet.setlockedBalance(50000.0);
        WalletDto dto = WalletMapper.toDto(wallet);

        assertNotNull(dto, "WalletDto không được null");
        assertEquals(150000.5, dto.getBalance(), "Số dư khả dụng phải chính xác");
        assertEquals(50000.0, dto.getLockedBalance(), "Số dư bị khóa phải chính xác");
    }

    @Test
    public void testItemMapper() {
        // Test null item
        assertNull(ItemMapper.toDto(null), "Null Item phải được map sang null Dto");

        // Test mapping hợp lệ
        Item item = new Item("owner_usr", "Laptop Gaming", "Mô tả máy tính", "Electronics", "Laptop");
        item.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);
        
        ItemDto dto = ItemMapper.toDto(item);

        assertNotNull(dto, "ItemDto không được null");
        assertEquals(item.getId(), dto.getId(), "ID phải chính xác");
        assertEquals(item.getCreatedAt().toString(), dto.getCreatedAt(), "Thời gian tạo phải chính xác");
        assertEquals("owner_usr", dto.getOwnerUsername(), "Tên chủ sở hữu phải chính xác");
        assertEquals("Laptop Gaming", dto.getName(), "Tên sản phẩm phải chính xác");
        assertEquals("Mô tả máy tính", dto.getDescription(), "Mô tả sản phẩm phải chính xác");
        assertEquals("Electronics", dto.getCategory(), "Danh mục sản phẩm phải chính xác");
        assertEquals("Laptop", dto.getProductType(), "Loại sản phẩm phải chính xác");
        assertEquals(ItemStatus.LOCKED_IN_AUCTION.name(), dto.getAvailabilityStatus(), "Trạng thái sẵn sàng phải chính xác");
    }

    @Test
    public void testUserMapperWithoutImage() {
        // Test null user
        assertNull(UserMapper.toDto(null), "Null User phải được map sang null Dto");

        // Tạo user không có ảnh đại diện
        User user = new User("test_user_mapper", "Mappie", "pbkdf2");
        user.setEmail("mapper@test.com");
        user.setPhoneNumber("0987654321");
        user.setRole(UserRole.USER);
        user.getWallet().deposit(100.0);

        UserDto dto = UserMapper.toDto(user);

        assertNotNull(dto);
        assertEquals("test_user_mapper", dto.getUsername());
        assertEquals("Mappie", dto.getNickname());
        assertEquals(UserRole.USER, dto.getRole());
        assertNull(dto.getProfileImageBase64(), "Ảnh đại diện base64 phải là null");
        assertEquals("mapper@test.com", dto.getEmail());
        assertEquals("0987654321", dto.getPhoneNumber());
        assertNotNull(dto.getWallet());
        assertEquals(100.0, dto.getWallet().getBalance());
    }

    @Test
    public void testUserMapperWithImage() throws Exception {
        // Lưu ảnh đại diện vào Database
        Image image = new Image("img_test_avatar", TimeUtil.nowInVietnam(), tempAvatarPath.toString());
        ImageDao.getInstance().create(image);

        // Lưu user có ảnh đại diện vào database (hoặc khởi tạo đối tượng)
        User user = new User("test_user_mapper", "Mappie", "pbkdf2");
        user.setEmail("mapper@test.com");
        user.setPhoneNumber("0987654321");
        user.setRole(UserRole.USER);
        user.setProfileImageId("img_test_avatar");
        user.getWallet().deposit(100.0);

        UserDto dto = UserMapper.toDto(user);

        assertNotNull(dto);
        assertEquals("test_user_mapper", dto.getUsername());
        assertNotNull(dto.getProfileImageBase64(), "Ảnh đại diện base64 phải được load");
        
        // Giải mã thử chuỗi Base64
        byte[] decodedBytes = java.util.Base64.getDecoder().decode(dto.getProfileImageBase64());
        assertEquals("dummy image bytes data", new String(decodedBytes), "Chuỗi ảnh được decode phải khớp dữ liệu file");
    }

    @Test
    public void testAuctionMapperWithoutItemAndBids() {
        assertNull(AuctionMapper.toDto(null));

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-mapper-1",
            now,
            "Đấu giá tủ lạnh",
            "Mô tả tủ lạnh",
            "seller_usr",
            "item-id-123",
            "bidder_usr",
            5000.0,
            100.0,
            now.plusDays(1),
            now.plusDays(2),
            AuctionStatus.ACTIVE
        );

        AuctionDto dto = AuctionMapper.toDto(auction);

        assertNotNull(dto);
        assertEquals("auc-mapper-1", dto.getId());
        assertEquals("item-id-123", dto.getItemId());
        assertEquals("Đấu giá tủ lạnh", dto.getAuctionName(), "Phải dùng auctionName của Auction khi Item null");
        assertEquals("Mô tả tủ lạnh", dto.getDescription(), "Phải dùng description của Auction khi Item null");
        assertEquals("seller_usr", dto.getSellerUsername());
        assertEquals("bidder_usr", dto.getCurrentBidderUsername());
        assertEquals(5000.0, dto.getStartingPrice());
        assertEquals(100.0, dto.getMinIncrement());
        assertEquals(AuctionStatus.ACTIVE.name(), dto.getStatus());
        assertTrue(dto.getBidHistory().isEmpty(), "Lịch sử đặt giá phải trống");
        assertNull(dto.getCategory(), "Category phải null");
        assertNull(dto.getThumbnailBase64());
        assertNull(dto.getGalleryBase64());
    }

    @Test
    public void testAuctionMapperWithItemAndBidsHistory() {
        LocalDateTime now = TimeUtil.nowInVietnam();
        
        // 1. Tạo Auction
        Auction auction = new Auction(
            "auc-mapper-2",
            now,
            "Auction Name",
            "Auction Desc",
            "seller_usr",
            "item-id-456",
            "bidder_usr",
            5000.0,
            100.0,
            now.plusDays(1),
            now.plusDays(2),
            AuctionStatus.ACTIVE
        );

        // 2. Tạo Item liên kết
        Item item = new Item("seller_usr", "Tivi Sony 4K", "Mô tả tivi Sony", "Home Appliance", "TV");

        // 3. Tạo một số Bids (được tạo ở thời gian khác nhau để check sắp xếp giảm dần)
        Bid bid1 = new Bid("bid-1", now.minusMinutes(10), "auc-mapper-2", "bidder1", 5100.0, false);
        Bid bid2 = new Bid("bid-2", now.minusMinutes(5), "auc-mapper-2", "bidder2", 5200.0, true);
        Bid bid3 = new Bid("bid-3", now.minusMinutes(15), "auc-mapper-2", "bidder3", 5000.0, false);

        auction.addBids(Arrays.asList(bid1, bid2, bid3));

        // 4. Các tham số ảnh giả lập
        String thumbnail = "base64-thumbnail";
        List<String> gallery = Arrays.asList("gallery-1", "gallery-2");

        // Thực hiện mapping
        AuctionDto dto = AuctionMapper.toDto(auction, item, thumbnail, gallery);

        assertNotNull(dto);
        assertEquals("auc-mapper-2", dto.getId());
        assertEquals("Tivi Sony 4K", dto.getAuctionName(), "Phải ưu tiên dùng name của Item");
        assertEquals("Mô tả tivi Sony", dto.getDescription(), "Phải ưu tiên dùng description của Item");
        assertEquals("Home Appliance", dto.getCategory(), "Category phải được lấy từ Item");
        assertEquals("TV", dto.getProductType(), "ProductType phải được lấy từ Item");

        // Check thumbnail và gallery
        assertEquals("base64-thumbnail", dto.getThumbnailBase64());
        assertEquals(2, dto.getGalleryBase64().size());
        assertEquals("gallery-1", dto.getGalleryBase64().get(0));

        // Check bid history (sắp xếp giảm dần theo thời gian tạo: bid2 (5 phút trước) -> bid1 (10 phút trước) -> bid3 (15 phút trước))
        List<BidDto> history = dto.getBidHistory();
        assertEquals(3, history.size(), "Lịch sử đấu giá phải có 3 phần tử");
        
        assertEquals("bid-2", history.get(0).getId(), "Phần tử đầu tiên phải là bid mới nhất (5 phút trước)");
        assertEquals("bid-1", history.get(1).getId(), "Phần tử thứ hai là bid cũ hơn (10 phút trước)");
        assertEquals("bid-3", history.get(2).getId(), "Phần tử cuối cùng là bid cũ nhất (15 phút trước)");
        
        assertTrue(history.get(0).isAutoBidGenerated(), "Phải giữ đúng cờ isAutoBidGenerated");
        assertFalse(history.get(1).isAutoBidGenerated());
    }
}
