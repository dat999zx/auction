package com.bidify.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.Event;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;

/**
 * Lớp kiểm thử (Unit Test) dành cho ItemService.
 * Chịu trách nhiệm kiểm tra luồng tạo mới, cập nhật, và lấy thông tin kho đồ (Item).
 */
class ItemServiceTest {
    // Khởi tạo các Service và DAO theo mẫu Singleton để thao tác với DB
    // Singleton đảm bảo chỉ có 1 instance duy nhất được sử dụng trong suốt quá trình chạy test
    private final ItemService itemService = ItemService.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    // Danh sách lưu trữ các username được tạo ra trong quá trình chạy test.
    // Mục đích: Dọn dẹp sạch sẽ dữ liệu rác khỏi cơ sở dữ liệu thật sau khi test kết thúc.
    private final List<String> createdUsernames = new ArrayList<>();

    @BeforeAll
    static void initDatabase() {
        // Đảm bảo cấu trúc bảng (schema) của SQLite đã được khởi tạo trước khi bất kỳ test nào chạy
        // Việc này ngăn chặn lỗi "no such table" nếu file .db vừa bị xoá hoặc chạy trên máy mới.
        SQLiteHelper.init();
    }

    @BeforeEach
    void setUp() {
        // Làm sạch bộ nhớ tạm (RAM) và danh sách dọn dẹp trước mỗi test để đảm bảo các test độc lập
        // Điều này giúp tránh trường hợp dữ liệu của test trước làm sai lệch kết quả của test sau.
        RealtimeDatabase.clearAll();
        createdUsernames.clear();
    }

    @AfterEach
    void tearDown() {
        RealtimeDatabase.clearAll();
        
        // Dọn dẹp dữ liệu DB theo đúng thứ tự để không vi phạm khoá ngoại (Foreign Key Constraints).
        // Bắt buộc xoá từ bảng con (ItemImageLinks) lên bảng cha (Items) rồi tới ông (Users).
        for (String username : createdUsernames) {
            // 1. Xoá liên kết ảnh của các item thuộc về user này
            SQLiteHelper.update("DELETE FROM ItemImageLinks WHERE itemId IN (SELECT id FROM Items WHERE ownerUsername = ?)", username);
            // 2. Xoá toàn bộ Item của user
            SQLiteHelper.update("DELETE FROM Items WHERE ownerUsername = ?", username);
            // 3. Cuối cùng mới xoá User
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        }
    }

    @Test
    void createItemSuccessfullyCreatesNewItemInDatabase() {
        // === Bước 1: Chuẩn bị dữ liệu đầu vào (Arrange) ===
        String username = uniqueUsername("user");
        User user = createTestUser(username, "pass123");
        
        // Giả lập một client (người dùng) đang đăng nhập vào hệ thống
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Giả lập Payload (dữ liệu gửi từ Client lên) bằng HashMap.
        // Cấu trúc này mô phỏng lại chuỗi JSON chứa thông tin sản phẩm muốn tạo.
        Map<String, Object> itemPayload = new HashMap<>();
        itemPayload.put("ownerUsername", username);
        itemPayload.put("name", "Vintage Camera");
        itemPayload.put("description", "A rare vintage camera from 1980s");
        itemPayload.put("category", "Collectibles");
        itemPayload.put("productType", "Vintage");
        itemPayload.put("imagesBase64", new ArrayList<String>());

        // Đóng gói payload vào một đối tượng Request với loại request là CREATE_ITEM
        Request request = new Request(RequestType.CREATE_ITEM, itemPayload);

        // === Bước 2: Thực thi (Act) ===
        // Gọi trực tiếp phương thức create() của ItemService giống như cách Router gọi nó
        Response response = itemService.create(client, request);

        // === Bước 3: Kiểm tra kết quả (Assert) ===
        // Kiểm tra xem phản hồi trả về có phải là SUCCESS không
        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertNotNull(response.getData());

        // Kiểm tra đối chiếu trực tiếp xuống DB (SQLite) để chắc chắn bản ghi đã được INSERT thành công
        int count = SQLiteHelper.query(
            "SELECT COUNT(*) FROM Items WHERE ownerUsername = ?", 
            rs -> {
                try {
                    return rs.next() ? rs.getInt(1) : 0;
                } catch (Exception e) {
                    return 0;
                }
            }, 
            username
        );
        assertEquals(1, count);
    }

    @Test
    void getUserInventoryReturnsEmptyListForNewUser() {
        // === Bước 1: Chuẩn bị (Arrange) ===
        String username = uniqueUsername("user");
        User user = createTestUser(username, "pass123");
        
        // Giả lập user vừa mới được tạo và đăng nhập (chưa từng tạo item nào)
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Tạo request yêu cầu lấy danh sách kho đồ của bản thân
        Request request = new Request(RequestType.GET_MY_INVENTORY, null);

        // === Bước 2: Thực thi (Act) ===
        Response response = itemService.getMyInventory(client, request);

        // === Bước 3: Kiểm tra (Assert) ===
        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        // Dữ liệu trả về (getData) phải là một List
        List<?> items = (List<?>) response.getData();
        assertNotNull(items);
        // Xác nhận rằng list trả về là rỗng (size = 0) do user mới tạo chưa có món đồ nào
        assertTrue(items.isEmpty(), "Người dùng mới tạo không được phép có Item nào trong Inventory");
    }

    // --- Helper Methods ---

    /**
     * Hàm hỗ trợ tạo nhanh một User lưu thẳng xuống DB.
     * Đồng thời tự động ghi nhận username vào danh sách cần dọn dẹp sau test.
     */
    private User createTestUser(String username, String rawPassword) {
        User user = new User(username, username, PasswordUtil.hash(rawPassword));
        userDao.create(user);
        createdUsernames.add(username);
        return user;
    }

    /**
     * Hàm tạo username ngẫu nhiên duy nhất (dùng UUID).
     * Đảm bảo test không bị xung đột (conflict) dữ liệu nếu chạy song song hoặc chạy nhiều lần.
     */
    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    // --- Mock Classes ---

    /**
     * Giả lập một ClientHandler (thay cho Socket thật).
     * Giúp chúng ta kiểm tra logic server mà không cần phải mở kết nối mạng.
     */
    private static class TestClientHandler extends ClientHandler {
        TestClientHandler() { super(null); }
        
        // Ghi đè phương thức này để xác định Client giả đang trong phiên làm việc
        @Override public boolean isInSession() { return getCurrentUsername() != null; }
        
        // Bỏ qua (Mock) việc gửi sự kiện qua luồng Socket mạng để tránh lỗi NullPointerException
        @Override public void sendEvent(Event event) {}
    }
}