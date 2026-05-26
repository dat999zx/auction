package com.bidify.server.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.ItemStatus;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.ItemImageLink;
import com.bidify.server.model.User;
import com.bidify.server.utility.PasswordUtil;

/**
 * Lớp kiểm thử (Unit Test) cho {@link ItemDao}.
 * Kiểm tra các tính năng:
 * - Thêm mới sản phẩm (Item) và tìm lại bằng ID.
 * - Cập nhật thông tin chi tiết sản phẩm.
 * - Xóa sản phẩm khỏi database.
 * - Truy vấn sản phẩm theo chủ sở hữu (owner).
 * - Truy vấn sản phẩm theo trạng thái khả dụng (availability status).
 * - Liên kết danh sách hình ảnh (Image) với sản phẩm và xóa các liên kết đó.
 * Có chú thích chi tiết bằng tiếng Việt.
 */
class ItemDaoTest {

    private final ItemDao itemDao = ItemDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    private final ImageDao imageDao = ImageDao.getInstance();

    private final List<String> createdUsernames = new ArrayList<>();
    private final List<String> createdItemIds = new ArrayList<>();
    private final List<String> createdImageIds = new ArrayList<>();

    @BeforeAll
    static void initDatabase() {
        SQLiteHelper.init();
    }

    @BeforeEach
    void setUp() {
        createdUsernames.clear();
        createdItemIds.clear();
        createdImageIds.clear();
    }

    @AfterEach
    void tearDown() {
        // Xóa liên kết ảnh sản phẩm trước để giữ ràng buộc khóa ngoại (FK)
        for (String itemId : createdItemIds) {
            SQLiteHelper.update("DELETE FROM ItemImageLinks WHERE itemId = ?", itemId);
            SQLiteHelper.update("DELETE FROM Items WHERE id = ?", itemId);
        }

        // Xóa người dùng thử nghiệm
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        }

        // Xóa hình ảnh thử nghiệm
        for (String imageId : createdImageIds) {
            SQLiteHelper.update("DELETE FROM Images WHERE id = ?", imageId);
        }
    }

    /**
     * Ca kiểm thử: Thêm mới sản phẩm và tìm lại thành công bằng ID.
     */
    @Test
    void createAndFindItemSuccessfully() {
        String owner = createTestUser("owner");
        Item item = new Item(owner, "Điện thoại iPhone 15", "Hàng new 99%", "Điện tử", "Điện thoại");

        itemDao.create(item);
        createdItemIds.add(item.getId());

        Item found = itemDao.findById(item.getId());

        assertNotNull(found);
        assertEquals(item.getId(), found.getId());
        assertEquals(item.getOwnerUsername(), found.getOwnerUsername());
        assertEquals("Điện thoại iPhone 15", found.getName());
        assertEquals("Hàng new 99%", found.getDescription());
        assertEquals("Điện tử", found.getCategory());
        assertEquals("Điện thoại", found.getProductType());
        assertEquals(ItemStatus.AVAILABLE, found.getAvailabilityStatus());
        assertNotNull(found.getCreatedAt());
    }

    /**
     * Ca kiểm thử: Lưu cập nhật thông tin sản phẩm thành công.
     */
    @Test
    void saveItemSuccessfully() {
        String owner = createTestUser("owner");
        Item item = new Item(owner, "Máy tính Dell", "Cũ", "Điện tử", "Laptop");
        itemDao.create(item);
        createdItemIds.add(item.getId());

        // Thay đổi thông tin sản phẩm
        item.setName("Máy tính Dell XPS 15");
        item.setDescription("Mới 100%");
        item.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);

        itemDao.save(item);

        Item updated = itemDao.findById(item.getId());
        assertNotNull(updated);
        assertEquals("Máy tính Dell XPS 15", updated.getName());
        assertEquals("Mới 100%", updated.getDescription());
        assertEquals(ItemStatus.LOCKED_IN_AUCTION, updated.getAvailabilityStatus());
    }

    /**
     * Ca kiểm thử: Xóa sản phẩm bằng ID thành công.
     */
    @Test
    void deleteByIdSuccessfully() {
        String owner = createTestUser("owner");
        Item item = new Item(owner, "Món đồ bỏ đi", "Mô tả", "Khác", "Khác");
        itemDao.create(item);
        createdItemIds.add(item.getId());

        assertNotNull(itemDao.findById(item.getId()));

        itemDao.deleteById(item.getId());

        assertNull(itemDao.findById(item.getId()), "Sản phẩm phải được xóa khỏi cơ sở dữ liệu.");
    }

    /**
     * Ca kiểm thử: Truy vấn danh sách sản phẩm thuộc về một chủ sở hữu.
     */
    @Test
    void findByOwnerUsernameSuccessfully() {
        String owner1 = createTestUser("owner1");
        String owner2 = createTestUser("owner2");

        Item item1 = new Item(owner1, "Đồ của owner 1 - A", "Mô tả", "Khác", "Khác");
        Item item2 = new Item(owner1, "Đồ của owner 1 - B", "Mô tả", "Khác", "Khác");
        Item item3 = new Item(owner2, "Đồ của owner 2", "Mô tả", "Khác", "Khác");

        itemDao.create(item1);
        itemDao.create(item2);
        itemDao.create(item3);
        createdItemIds.add(item1.getId());
        createdItemIds.add(item2.getId());
        createdItemIds.add(item3.getId());

        List<Item> owner1Items = itemDao.findByOwnerUsername(owner1);

        assertNotNull(owner1Items);
        assertEquals(2, owner1Items.size());
        assertTrue(owner1Items.stream().anyMatch(i -> i.getId().equals(item1.getId())));
        assertTrue(owner1Items.stream().anyMatch(i -> i.getId().equals(item2.getId())));
    }

    /**
     * Ca kiểm thử: Lọc danh sách sản phẩm theo trạng thái khả dụng.
     */
    @Test
    void findByAvailabilityStatusSuccessfully() {
        String owner = createTestUser("owner");
        Item item1 = new Item(owner, "Đồ rảnh rỗi", "Mô tả", "Khác", "Khác");
        item1.setAvailabilityStatus(ItemStatus.AVAILABLE);

        Item item2 = new Item(owner, "Đồ đang đấu giá", "Mô tả", "Khác", "Khác");
        item2.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);

        itemDao.create(item1);
        itemDao.create(item2);
        createdItemIds.add(item1.getId());
        createdItemIds.add(item2.getId());

        List<Item> lockedItems = itemDao.findByAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);

        assertNotNull(lockedItems);
        assertFalse(lockedItems.isEmpty());
        assertTrue(lockedItems.stream().anyMatch(i -> i.getId().equals(item2.getId())));
        assertFalse(lockedItems.stream().anyMatch(i -> i.getId().equals(item1.getId())));
    }

    /**
     * Ca kiểm thử: Kết hợp lọc theo cả chủ sở hữu và trạng thái sản phẩm.
     */
    @Test
    void findByOwnerUsernameAndAvailabilityStatusSuccessfully() {
        String owner1 = createTestUser("owner1");
        String owner2 = createTestUser("owner2");

        Item item1 = new Item(owner1, "Item A", "Mô tả", "Khác", "Khác");
        item1.setAvailabilityStatus(ItemStatus.AVAILABLE);

        Item item2 = new Item(owner1, "Item B", "Mô tả", "Khác", "Khác");
        item2.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);

        Item item3 = new Item(owner2, "Item C", "Mô tả", "Khác", "Khác");
        item3.setAvailabilityStatus(ItemStatus.LOCKED_IN_AUCTION);

        itemDao.create(item1);
        itemDao.create(item2);
        itemDao.create(item3);
        createdItemIds.add(item1.getId());
        createdItemIds.add(item2.getId());
        createdItemIds.add(item3.getId());

        List<Item> result = itemDao.findByOwnerUsernameAndAvailabilityStatus(owner1, ItemStatus.LOCKED_IN_AUCTION);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(item2.getId(), result.get(0).getId());
    }

    /**
     * Ca kiểm thử: Cập nhật nhanh trạng thái khả dụng của sản phẩm.
     */
    @Test
    void updateAvailabilityStatusSuccessfully() {
        String owner = createTestUser("owner");
        Item item = new Item(owner, "Đồ đổi trạng thái", "Mô tả", "Khác", "Khác");
        itemDao.create(item);
        createdItemIds.add(item.getId());

        assertEquals(ItemStatus.AVAILABLE, itemDao.findById(item.getId()).getAvailabilityStatus());

        itemDao.updateAvailabilityStatus(item.getId(), ItemStatus.LOCKED_IN_AUCTION);

        assertEquals(ItemStatus.LOCKED_IN_AUCTION, itemDao.findById(item.getId()).getAvailabilityStatus());
    }

    /**
     * Ca kiểm thử: Liên kết hình ảnh và sản phẩm, kiểm tra thứ tự hiển thị, ảnh đại diện và lấy ID ảnh.
     */
    @Test
    void saveAndGetItemImageLinksSuccessfully() {
        String owner = createTestUser("owner");
        Item item = new Item(owner, "Sản phẩm nhiều ảnh", "Mô tả", "Khác", "Khác");
        itemDao.create(item);
        createdItemIds.add(item.getId());

        // Tạo 2 ảnh giả lập lưu trong DB
        Image img1 = new Image("img-1", TimeUtil.nowInVietnam(), "uploads/img1.png");
        Image img2 = new Image("img-2", TimeUtil.nowInVietnam(), "uploads/img2.png");
        imageDao.create(img1);
        imageDao.create(img2);
        createdImageIds.add(img1.getId());
        createdImageIds.add(img2.getId());

        List<Image> images = new ArrayList<>();
        images.add(img1); // Phải là ảnh Primary (ảnh đầu tiên index 0)
        images.add(img2);

        // Lưu liên kết ảnh
        itemDao.saveItemImageLinks(item.getId(), images);

        // 1. Lấy danh sách liên kết và xác nhận
        List<ItemImageLink> links = itemDao.getItemImageLinks(item.getId());
        assertNotNull(links);
        assertEquals(2, links.size());

        // Ảnh đầu tiên (img1) được thiết lập displayOrder = 0 và isPrimary = true
        ItemImageLink link1 = links.stream().filter(l -> l.getImageId().equals("img-1")).findFirst().orElse(null);
        assertNotNull(link1);
        assertEquals(0, link1.getDisplayOrder());
        assertTrue(link1.isPrimary());

        // Ảnh thứ hai (img2) có displayOrder = 1 và isPrimary = false
        ItemImageLink link2 = links.stream().filter(l -> l.getImageId().equals("img-2")).findFirst().orElse(null);
        assertNotNull(link2);
        assertEquals(1, link2.getDisplayOrder());
        assertFalse(link2.isPrimary());

        // 2. Kiểm tra lấy ID danh sách ảnh bằng findImageIdsByItemId
        List<String> imageIds = itemDao.findImageIdsByItemId(item.getId());
        assertNotNull(imageIds);
        assertEquals(2, imageIds.size());
        assertEquals("img-1", imageIds.get(0)); // Sắp xếp đúng theo primary/order
        assertEquals("img-2", imageIds.get(1));

        // 3. Xóa toàn bộ liên kết hình ảnh
        itemDao.deleteItemImageLinks(item.getId());
        assertTrue(itemDao.getItemImageLinks(item.getId()).isEmpty(), "Toàn bộ liên kết hình ảnh của sản phẩm phải bị xóa.");
    }

    // --- Hàm trợ giúp (Helper Methods) ---

    private String createTestUser(String usernamePrefix) {
        String username = usernamePrefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        User user = new User(username, username, PasswordUtil.hash("pass123"));
        userDao.create(user);
        createdUsernames.add(username);
        return username;
    }
}
