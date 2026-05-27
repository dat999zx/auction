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

import com.bidify.common.utility.TimeUtil;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.ItemImageLink;
import com.bidify.server.model.User;
import com.bidify.server.utility.PasswordUtil;

/**
 * Lớp kiểm thử (Unit Test) cho {@link ImageDao}.
 * Kiểm tra các tính năng:
 * - Thêm mới hình ảnh (Image) và tìm lại theo ID (create & findById).
 * - Xóa hình ảnh theo ID (deleteById).
 * - Tạo liên kết hình ảnh với sản phẩm (createItemImageLink).
 * - Truy vấn danh sách liên kết ảnh của sản phẩm (findItemLinks) và xác thực thứ tự sắp xếp:
 *   + Ảnh chính (isPrimary = true) lên đầu.
 *   + Sau đó sắp xếp theo thứ tự hiển thị (displayOrder ASC).
 *   + Cuối cùng theo thời gian tạo (createdAt ASC).
 * Có chú thích chi tiết bằng tiếng Việt.
 */
class ImageDaoTest {

    private final ImageDao imageDao = ImageDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

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
        // Xóa liên kết ảnh trước để giữ ràng buộc khóa ngoại (FK)
        for (String itemId : createdItemIds) {
            SQLiteHelper.update("DELETE FROM ItemImageLinks WHERE itemId = ?", itemId);
            SQLiteHelper.update("DELETE FROM Items WHERE id = ?", itemId);
        }

        // Xóa người dùng
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        }

        // Xóa hình ảnh
        for (String imageId : createdImageIds) {
            SQLiteHelper.update("DELETE FROM Images WHERE id = ?", imageId);
        }
    }

    /**
     * Ca kiểm thử: Thêm mới ảnh thành công và tìm lại theo ID.
     */
    @Test
    void createAndFindImageSuccessfully() {
        Image image = new Image("img-test-1", TimeUtil.nowInVietnam(), "uploads/img1.png");
        imageDao.create(image);
        createdImageIds.add(image.getId());

        Image found = imageDao.findById(image.getId());

        assertNotNull(found);
        assertEquals(image.getId(), found.getId());
        assertEquals("uploads/img1.png", found.getFilePath());
        assertNotNull(found.getCreatedAt());
    }

    /**
     * Ca kiểm thử: Xóa ảnh theo ID thành công.
     */
    @Test
    void deleteImageByIdSuccessfully() {
        Image image = new Image("img-test-2", TimeUtil.nowInVietnam(), "uploads/img2.png");
        imageDao.create(image);
        createdImageIds.add(image.getId());

        assertNotNull(imageDao.findById(image.getId()));

        imageDao.deleteById(image.getId());

        assertNull(imageDao.findById(image.getId()), "Ảnh phải được xóa khỏi cơ sở dữ liệu.");
    }

    /**
     * Ca kiểm thử: Tạo liên kết ảnh và sản phẩm, kiểm tra thứ tự sắp xếp của findItemLinks.
     * Thứ tự sắp xếp mong đợi: isPrimary DESC, displayOrder ASC, createdAt ASC.
     */
    @Test
    void createAndFindItemImageLinkSuccessfully() {
        String owner = createTestUser("owner");
        
        // Tạo sản phẩm
        Item item = new Item(owner, "Sản phẩm", "Mô tả", "Danh mục", "Loại");
        itemDao.create(item);
        createdItemIds.add(item.getId());

        // Tạo 3 ảnh giả lập
        Image img1 = new Image("img-1", TimeUtil.nowInVietnam(), "uploads/img1.png");
        Image img2 = new Image("img-2", TimeUtil.nowInVietnam().plusSeconds(1), "uploads/img2.png");
        Image img3 = new Image("img-3", TimeUtil.nowInVietnam().plusSeconds(2), "uploads/img3.png");
        imageDao.create(img1);
        imageDao.create(img2);
        imageDao.create(img3);
        createdImageIds.add(img1.getId());
        createdImageIds.add(img2.getId());
        createdImageIds.add(img3.getId());

        // Tạo liên kết ảnh 1: không phải primary, displayOrder = 1
        ItemImageLink link1 = new ItemImageLink(UUID.randomUUID().toString().substring(0, 8), TimeUtil.nowInVietnam(), item.getId(), img1.getId(), 1, false);
        
        // Tạo liên kết ảnh 2: là primary (phải lên đầu), displayOrder = 2
        ItemImageLink link2 = new ItemImageLink(UUID.randomUUID().toString().substring(0, 8), TimeUtil.nowInVietnam().plusSeconds(1), item.getId(), img2.getId(), 2, true);
        
        // Tạo liên kết ảnh 3: không phải primary, displayOrder = 0 (phải đứng thứ hai vì displayOrder nhỏ hơn link1)
        ItemImageLink link3 = new ItemImageLink(UUID.randomUUID().toString().substring(0, 8), TimeUtil.nowInVietnam().plusSeconds(2), item.getId(), img3.getId(), 0, false);

        imageDao.createItemImageLink(link1);
        imageDao.createItemImageLink(link2);
        imageDao.createItemImageLink(link3);

        // Truy xuất danh sách liên kết
        List<ItemImageLink> links = imageDao.findItemLinks(item.getId());

        assertNotNull(links);
        assertEquals(3, links.size());

        // Thứ tự mong đợi: 
        // 1. link2 (isPrimary = true)
        // 2. link3 (isPrimary = false, displayOrder = 0)
        // 3. link1 (isPrimary = false, displayOrder = 1)
        assertEquals(link2.getImageId(), links.get(0).getImageId());
        assertEquals(link3.getImageId(), links.get(1).getImageId());
        assertEquals(link1.getImageId(), links.get(2).getImageId());
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
