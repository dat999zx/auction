package com.bidify.server.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.dto.UserDto;
import com.bidify.common.dto.PublicProfileDto;
import com.bidify.common.dto.PublicProfileStatsDto;
import com.bidify.common.model.PublicProfileRequest;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdatePasswordRequest;
import com.bidify.common.model.UpdateProfileRequest;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;

/**
 * Lớp kiểm thử (Unit Test) cho {@link UserProfileService}.
 * Kiểm tra các tính năng: Lấy thông tin cá nhân, cập nhật nickname, đổi mật khẩu.
 * Chứa chú thích chi tiết cho từng phương thức và ca kiểm thử.
 */
class UserProfileServiceTest {

    // Service cần kiểm thử
    private final UserProfileService userProfileService = UserProfileService.getInstance();
    
    // Đối tượng truy cập dữ liệu để kiểm tra trực tiếp SQLite
    private final UserDao userDao = UserDao.getInstance();
    
    // Lưu các username được tạo ra trong quá trình chạy test để dọn dẹp sau mỗi ca test
    private final List<String> createdUsernames = new ArrayList<>();

    /**
     * Khởi tạo cơ sở dữ liệu SQLite trước khi chạy toàn bộ các bài test.
     * Đảm bảo schema của bảng Users đã được thiết lập đúng.
     */
    @BeforeAll
    static void initDatabase() {
        SQLiteHelper.init();
    }

    /**
     * Chuẩn bị trạng thái trước mỗi ca test:
     * - Xóa sạch thông tin bộ nhớ đệm (RealtimeDatabase).
     * - Reset danh sách tài khoản đã được tạo.
     */
    @BeforeEach
    void setUp() {
        RealtimeDatabase.clearAll();
        createdUsernames.clear();
    }

    /**
     * Dọn dẹp tài nguyên sau mỗi ca test:
     * - Xóa sạch thông tin phiên làm việc trong RAM.
     * - Xóa toàn bộ tài khoản thử nghiệm đã thêm vào SQLite để tránh rác cơ sở dữ liệu.
     */
    @AfterEach
    void tearDown() {
        RealtimeDatabase.clearAll();
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        }
    }

    /**
     * Ca kiểm thử: Lấy thông tin cá nhân thành công.
     * Kiểm tra xem thông tin tài khoản (username, nickname, role) trả về có chính xác không.
     */
    @Test
    void getProfileSuccessfully() {
        String username = uniqueUsername("profile-ok");
        User user = createUser(username, "MyNickname", "pass123");
        user.setEmail("profile@example.com");
        user.setPhoneNumber("0912345678");
        userDao.save(user, false);

        // Giả lập client đang đăng nhập tài khoản này
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Gọi service lấy profile
        Response response = userProfileService.getProfile(client);

        // Xác nhận trạng thái Response là SUCCESS và chứa đúng thông tin
        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Profile loaded successfully", response.getMessage());
        
        assertNotNull(response.getData());
        // Ép kiểu dữ liệu trả về sang UserDto để kiểm tra các trường
        UserDto dto = (UserDto) response.getData();
        assertEquals(username, dto.getUsername());
        assertEquals("MyNickname", dto.getNickname());
        assertEquals("profile@example.com", dto.getEmail());
        assertEquals("0912345678", dto.getPhoneNumber());
    }

    /**
     * Ca kiểm thử: Lấy thông tin cá nhân thất bại khi phiên làm việc không hợp lệ (Chưa đăng nhập).
     */
    @Test
    void getProfileFailsWhenSessionInvalid() {
        TestClientHandler client = new TestClientHandler();
        // Client chưa đăng nhập (username = null)
        client.setCurrentUsername(null);

        Response response = userProfileService.getProfile(client);

        // Báo lỗi do username bị null khi gọi getOrLoadUser
        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Username cannot be empty", response.getMessage());
    }

    /**
     * Ca kiểm thử: Lấy thông tin cá nhân thất bại khi tài khoản không tồn tại trong hệ thống.
     */
    @Test
    void getProfileFailsWhenUserNotFound() {
        String username = uniqueUsername("non-exist");
        
        TestClientHandler client = new TestClientHandler();
        // Gán username nhưng không lưu user này vào DB và không đưa vào online list
        client.setCurrentUsername(username);

        Response response = userProfileService.getProfile(client);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertTrue(response.getMessage().contains("User not found"));
    }

    /**
     * Ca kiểm thử: Cập nhật nickname thành công và đồng bộ chính xác xuống database.
     */
    @Test
    void updateProfileSuccessfullyUpdatesNickname() {
        String username = uniqueUsername("update-ok");
        User user = createUser(username, "OldNickname", "pass123");

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Tạo yêu cầu cập nhật nickname mới
        String newNickname = "NewNickname";
        UpdateProfileRequest updateData = new UpdateProfileRequest(newNickname, null);
        Request request = new Request(RequestType.UPDATE_PROFILE, updateData);

        Response response = userProfileService.updateProfile(client, request);

        // Kiểm tra kết quả trả về
        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Profile updated successfully", response.getMessage());

        UserDto dto = (UserDto) response.getData();
        assertEquals(newNickname, dto.getNickname());

        // Kiểm tra trực tiếp trong SQLite để chắc chắn nickname đã được lưu
        User updatedUser = userDao.findByUsername(username);
        assertNotNull(updatedUser);
        assertEquals(newNickname, updatedUser.getNickname());
    }

    /**
     * Ca kiểm thử: Cập nhật nickname thất bại khi không truyền thay đổi nào.
     */
    @Test
    void updateProfileFailsWhenNoChangesProvided() {
        String username = uniqueUsername("no-change");
        User user = createUser(username, "MyNickname", "pass123");

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Gửi yêu cầu với nickname = null (không có thay đổi)
        UpdateProfileRequest updateData = new UpdateProfileRequest(null, null);
        Request request = new Request(RequestType.UPDATE_PROFILE, updateData);

        Response response = userProfileService.updateProfile(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("No profile changes were provided", response.getMessage());
    }

    /**
     * Ca kiểm thử: Cập nhật nickname thất bại khi nickname quá ngắn (dưới 3 ký tự).
     */
    @Test
    void updateProfileFailsWhenNicknameTooShort() {
        String username = uniqueUsername("short-nick");
        User user = createUser(username, "MyNickname", "pass123");

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Nickname "ab" chỉ có 2 ký tự
        UpdateProfileRequest updateData = new UpdateProfileRequest("ab", null);
        Request request = new Request(RequestType.UPDATE_PROFILE, updateData);

        Response response = userProfileService.updateProfile(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Nickname must be between 3 and 20 characters", response.getMessage());
    }

    /**
     * Ca kiểm thử: Cập nhật nickname thất bại khi nickname quá dài (trên 20 ký tự).
     */
    @Test
    void updateProfileFailsWhenNicknameTooLong() {
        String username = uniqueUsername("long-nick");
        User user = createUser(username, "MyNickname", "pass123");

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Nickname 21 ký tự
        UpdateProfileRequest updateData = new UpdateProfileRequest("abcdefghijklmnopqrstu", null);
        Request request = new Request(RequestType.UPDATE_PROFILE, updateData);

        Response response = userProfileService.updateProfile(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Nickname must be between 3 and 20 characters", response.getMessage());
    }

    /**
     * Ca kiểm thử: Đổi mật khẩu thành công.
     * Mật khẩu cũ khớp và mật khẩu mới hợp lệ.
     */
    @Test
    void updatePasswordSuccessfully() {
        String username = uniqueUsername("pw-ok");
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword123";
        User user = createUser(username, "Nickname", oldPassword);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Tạo yêu cầu đổi mật khẩu
        UpdatePasswordRequest pwData = new UpdatePasswordRequest(oldPassword, newPassword);
        Request request = new Request(RequestType.UPDATE_PASSWORD, pwData);

        Response response = userProfileService.updatePassword(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Password updated successfully", response.getMessage());

        // Kiểm tra xem mật khẩu mới đã được băm (hash) và lưu vào SQLite chưa
        User updatedUser = userDao.findByUsername(username);
        assertNotNull(updatedUser);
        assertNotEquals(newPassword, updatedUser.getPassword());
        assertTrue(PasswordUtil.matches(newPassword, updatedUser.getPassword()));
    }

    /**
     * Ca kiểm thử: Đổi mật khẩu thất bại khi mật khẩu hiện tại (mật khẩu cũ) không đúng.
     */
    @Test
    void updatePasswordFailsWhenCurrentPasswordIncorrect() {
        String username = uniqueUsername("pw-wrong");
        User user = createUser(username, "Nickname", "correctPassword");

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Nhập mật khẩu hiện tại sai
        UpdatePasswordRequest pwData = new UpdatePasswordRequest("wrongPassword", "newPassword123");
        Request request = new Request(RequestType.UPDATE_PASSWORD, pwData);

        Response response = userProfileService.updatePassword(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Incorrect password", response.getMessage());
    }

    /**
     * Ca kiểm thử: Đổi mật khẩu thất bại khi mật khẩu mới trùng mật khẩu cũ.
     */
    @Test
    void updatePasswordFailsWhenNewPasswordSameAsCurrent() {
        String username = uniqueUsername("pw-same");
        String password = "samePassword123";
        User user = createUser(username, "Nickname", password);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Đổi sang mật khẩu mới giống hệt mật khẩu cũ
        UpdatePasswordRequest pwData = new UpdatePasswordRequest(password, password);
        Request request = new Request(RequestType.UPDATE_PASSWORD, pwData);

        Response response = userProfileService.updatePassword(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("New password must be different from current password", response.getMessage());
    }

    /**
     * Ca kiểm thử: Đổi mật khẩu thất bại khi mật khẩu mới quá ngắn (dưới 6 ký tự).
     */
    @Test
    void updatePasswordFailsWhenNewPasswordTooShort() {
        String username = uniqueUsername("pw-short");
        String oldPassword = "oldPassword123";
        User user = createUser(username, "Nickname", oldPassword);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Mật khẩu mới "12345" chỉ có 5 ký tự
        UpdatePasswordRequest pwData = new UpdatePasswordRequest(oldPassword, "12345");
        Request request = new Request(RequestType.UPDATE_PASSWORD, pwData);

        Response response = userProfileService.updatePassword(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Password must be at least 6 characters", response.getMessage());
    }

    /**
     * Ca kiểm thử: Đổi mật khẩu thất bại khi mật khẩu mới chứa khoảng trắng.
     */
    @Test
    void updatePasswordFailsWhenNewPasswordContainsSpaces() {
        String username = uniqueUsername("pw-space");
        String oldPassword = "oldPassword123";
        User user = createUser(username, "Nickname", oldPassword);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Mật khẩu mới chứa khoảng trắng
        UpdatePasswordRequest pwData = new UpdatePasswordRequest(oldPassword, "new pass 123");
        Request request = new Request(RequestType.UPDATE_PASSWORD, pwData);

        Response response = userProfileService.updatePassword(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Password cannot contains spaces", response.getMessage());
    }

    /**
     * Ca kiểm thử: Lấy thông tin public profile thành công.
     */
    @Test
    void getPublicProfileSuccessfully() {
        String username = uniqueUsername("pub-ok");
        User user = createUser(username, "PublicNickname", "pass123");
        user.setEmail("public@example.com");
        user.setPhoneNumber("0987654321");
        userDao.save(user, false);

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        PublicProfileRequest requestData = new PublicProfileRequest(username);
        Request request = new Request(RequestType.GET_PUBLIC_PROFILE, requestData);

        Response response = userProfileService.getPublicProfile(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Public profile loaded successfully", response.getMessage());
        assertNotNull(response.getData());

        PublicProfileDto dto = (PublicProfileDto) response.getData();
        assertEquals(username, dto.getUsername());
        assertEquals("PublicNickname", dto.getNickname());
        assertEquals("public@example.com", dto.getEmail());
        assertEquals("0987654321", dto.getPhoneNumber());
        assertNotNull(dto.getStats());
        assertEquals(0, dto.getStats().getTotalAuctions());
        assertEquals(0, dto.getStats().getTotalBids());
    }

    /**
     * Ca kiểm thử: Lấy thông tin public profile thất bại khi username trống.
     */
    @Test
    void getPublicProfileFailsWhenUsernameEmpty() {
        TestClientHandler client = new TestClientHandler();

        PublicProfileRequest requestData = new PublicProfileRequest("");
        Request request = new Request(RequestType.GET_PUBLIC_PROFILE, requestData);

        Response response = userProfileService.getPublicProfile(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Username cannot be empty", response.getMessage());
    }

    /**
     * Ca kiểm thử: Lấy thông tin public profile thất bại khi user không tồn tại.
     */
    @Test
    void getPublicProfileFailsWhenUserNotFound() {
        TestClientHandler client = new TestClientHandler();

        PublicProfileRequest requestData = new PublicProfileRequest("nonexistentuser");
        Request request = new Request(RequestType.GET_PUBLIC_PROFILE, requestData);

        Response response = userProfileService.getPublicProfile(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("User not found", response.getMessage());
    }

    @Test
    void updateProfileSuccessfullyUpdatesContactFields() {
        String username = uniqueUsername("contactok");
        User user = createUser(username, "OldNickname", "pass123");

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        UpdateProfileRequest updateData = new UpdateProfileRequest(
            "NewNickname",
            null,
            null,
            "new@example.com",
            "0912345678"
        );
        Request request = new Request(RequestType.UPDATE_PROFILE, updateData);

        Response response = userProfileService.updateProfile(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());

        UserDto dto = (UserDto) response.getData();
        assertEquals("new@example.com", dto.getEmail());
        assertEquals("0912345678", dto.getPhoneNumber());

        User updatedUser = userDao.findByUsername(username);
        assertNotNull(updatedUser);
        assertEquals("new@example.com", updatedUser.getEmail());
        assertEquals("0912345678", updatedUser.getPhoneNumber());
    }

    @Test
    void updateProfileFailsWhenEmailInvalid() {
        String username = uniqueUsername("bademail");
        User user = createUser(username, "MyNickname", "pass123");

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        UpdateProfileRequest updateData = new UpdateProfileRequest("MyNickname", null, null, "not-email", null);
        Request request = new Request(RequestType.UPDATE_PROFILE, updateData);

        Response response = userProfileService.updateProfile(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Invalid email format", response.getMessage());
    }

    @Test
    void updateProfileFailsWhenPhoneInvalid() {
        String username = uniqueUsername("badphone");
        User user = createUser(username, "MyNickname", "pass123");

        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        UpdateProfileRequest updateData = new UpdateProfileRequest("MyNickname", null, null, null, "abc");
        Request request = new Request(RequestType.UPDATE_PROFILE, updateData);

        Response response = userProfileService.updateProfile(client, request);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Phone number must contain 10 or 11 digits", response.getMessage());
    }

    /**
     * Hàm phụ trợ (Helper): Tạo một user và lưu vào SQLite để test.
     */
    private User createUser(String username, String nickname, String rawPassword) {
        User user = new User(username, nickname, PasswordUtil.hash(rawPassword));
        userDao.create(user);
        createdUsernames.add(username);
        return user;
    }

    /**
     * Hàm phụ trợ (Helper): Tạo một username độc nhất bằng UUID để tránh trùng lặp giữa các lần test.
     */
    private String uniqueUsername(String prefix) {
        String normalizedPrefix = prefix.replace("-", "");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        int maxPrefixLength = Math.max(4, 20 - suffix.length());
        if (normalizedPrefix.length() > maxPrefixLength) {
            normalizedPrefix = normalizedPrefix.substring(0, maxPrefixLength);
        }
        return normalizedPrefix + suffix;
    }

    /**
     * Lớp giả lập (Mock/Stub) ClientHandler dùng riêng cho kiểm thử.
     * Tránh phụ thuộc vào socket kết nối mạng thực tế.
     */
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
