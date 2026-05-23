package com.bidify.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;
import com.bidify.common.model.LoginRequest;
import com.bidify.common.model.RegisterRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Admin;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;

class AuthServiceTest {
    private final AuthService authService = AuthService.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    // Lưu lại username do test tạo ra để dọn sạch sau mỗi test, tránh làm bẩn DB thật của project.
    private final List<String> createdUsernames = new ArrayList<>();

    @BeforeAll
    static void initDatabase() {
        // Đảm bảo schema SQLite đã tồn tại trước khi chạy test.
        // Nếu chưa có bảng Users thì các test register/login sẽ fail ngay từ đầu.
        SQLiteHelper.init();
        resetBootstrapAdmin();
    }

    @Test
    void schemaInitCreatesBootstrapAdminInDatabase() {
        SQLiteHelper.update("DELETE FROM Users WHERE username = ?", AuthService.BOOTSTRAP_ADMIN_USERNAME);

        SQLiteHelper.init();

        User admin = userDao.findByUsername(AuthService.BOOTSTRAP_ADMIN_USERNAME);
        assertNotNull(admin);
        assertEquals(AuthService.BOOTSTRAP_ADMIN_USERNAME, admin.getUsername());
        assertEquals(AuthService.BOOTSTRAP_ADMIN_NICKNAME, admin.getNickname());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals(UserStatus.ACTIVE, admin.getStatus());
        assertTrue(PasswordUtil.matches(AuthService.BOOTSTRAP_ADMIN_PASSWORD, admin.getPassword()));
    }

    @Test
    void schemaInitDoesNotOverwriteExistingBootstrapAdmin() {
        SQLiteHelper.update("DELETE FROM Users WHERE username = ?", AuthService.BOOTSTRAP_ADMIN_USERNAME);

        User admin = new User(
            AuthService.BOOTSTRAP_ADMIN_USERNAME,
            "Custom Admin",
            PasswordUtil.hash("custom123")
        );
        admin.setRole(UserRole.ADMIN);
        userDao.create(admin);

        SQLiteHelper.init();

        User savedAdmin = userDao.findByUsername(AuthService.BOOTSTRAP_ADMIN_USERNAME);
        assertNotNull(savedAdmin);
        assertEquals("Custom Admin", savedAdmin.getNickname());
        assertTrue(PasswordUtil.matches("custom123", savedAdmin.getPassword()));
        assertEquals(UserRole.ADMIN, savedAdmin.getRole());
    }

    @BeforeEach
    void setUp() {
        // Xóa toàn bộ trạng thái runtime trong RAM trước mỗi test.
        // Việc này giúp test này không ảnh hưởng sang test khác qua RealtimeDatabase.
        RealtimeDatabase.clearAll();
        // Reset danh sách user được tạo trong test hiện tại.
        createdUsernames.clear();
    }

    @AfterEach
    void tearDown() {
        // Sau mỗi test, xóa sạch session/active user trong bộ nhớ.
        RealtimeDatabase.clearAll();
        // Xóa user test khỏi SQLite để repo không bị tích lũy dữ liệu giả sau nhiều lần chạy test.
        for (String username : createdUsernames)
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        resetBootstrapAdmin();
    }

    private static void resetBootstrapAdmin() {
        SQLiteHelper.update("DELETE FROM Users WHERE username = ?", AuthService.BOOTSTRAP_ADMIN_USERNAME);
        SQLiteHelper.init();
    }

    @Test
    void registerSuccessfullyCreatesUserWithHashedPassword() {
        // Tạo username riêng để tránh đụng dữ liệu cũ trong DB.
        String username = uniqueUsername("register-ok");
        String password = "secret123"; // password gốc user nhập vào
        // User này sẽ được tạo bởi register(), nên cần ghi nhớ để xóa ở tearDown().
        createdUsernames.add(username);

        // Gọi đúng luồng đăng ký như server đang xử lý thật.
        Response response = authService.register(new Request(
            RequestType.REGISTER,
            new RegisterRequest(username, password)
        ));

        // Đăng ký hợp lệ phải trả về SUCCESS.
        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Register successfully", response.getMessage());

        // Đọc lại từ DB để xác nhận user đã được lưu thật, không chỉ trả response thành công.
        User savedUser = userDao.findByUsername(username);
        assertNotNull(savedUser);
        // Theo thiết kế hiện tại, nickname đang tự gán bằng username.
        assertEquals(username, savedUser.getNickname());
        // Không được lưu password thô xuống DB.
        assertNotEquals(password, savedUser.getPassword());
        // Hash lưu trong DB phải khớp với password gốc khi kiểm tra lại bằng PasswordUtil.
        assertTrue(PasswordUtil.matches(password, savedUser.getPassword()));
    }

    @Test
    void registerFailsForBootstrapAdminUsername() {
        Response response = authService.register(new Request(
            RequestType.REGISTER,
            new RegisterRequest(AuthService.BOOTSTRAP_ADMIN_USERNAME, "secret123")
        ));

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Username already exists", response.getMessage());
    }

    @Test
    void registerFailsWhenUsernameAlreadyExists() {
        String username = uniqueUsername("duplicate-user");
        // Seed sẵn 1 user trong DB để giả lập trường hợp username bị trùng.
        createUser(username, "firstNick", "secret123");

        Response response = authService.register(new Request(
            RequestType.REGISTER,
            new RegisterRequest(username, "secret456")
        ));

        // Khi username đã tồn tại, service phải chặn lại và trả FAILED.
        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Username already exists", response.getMessage());
    }

    @Test
    void registerFailsWhenUsernameFormatIsInvalid() {
        // "bad user" chứa khoảng trắng nên sẽ fail validateUsername().
        Response response = authService.register(new Request(
            RequestType.REGISTER,
            new RegisterRequest("bad user", "secret123")
        ));

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Username cannot contains spaces", response.getMessage());
    }

    @Test
    void registerFailsWhenPasswordFormatIsInvalid() {
        // Password quá ngắn nên sẽ fail validatePassword().
        Response response = authService.register(new Request(
            RequestType.REGISTER,
            new RegisterRequest(uniqueUsername("bad-pass"), "123")
        ));

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Password must be at least 6 characters", response.getMessage());
    }

    @Test
    void loginFailsWhenPasswordIsIncorrect() {
        String username = uniqueUsername("wrong-pass");
        // Tạo user thật trong DB với password đúng là "secret123".
        createUser(username, "loginNick", "secret123");
        // Client giả lập, không cần socket thật nhưng vẫn đủ cho AuthService thao tác session.
        TestClientHandler client = new TestClientHandler();

        Response response = authService.login(
            client,
            new Request(RequestType.LOGIN, new LoginRequest(username, "wrongpass"))
        );

        // Sai password phải bị từ chối đăng nhập.
        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Username or password is incorrect", response.getMessage());
        // Client không được gắn username nếu login thất bại.
        assertNull(client.getCurrentUsername());
        // RealtimeDatabase cũng không được đánh dấu user này đang online.
        assertFalse(RealtimeDatabase.isUserOnline(username));
    }

    @Test
    void loginUsesPersistedBootstrapAdminFromDatabase() {
        SQLiteHelper.update("DELETE FROM Users WHERE username = ?", AuthService.BOOTSTRAP_ADMIN_USERNAME);

        User admin = new User(
            AuthService.BOOTSTRAP_ADMIN_USERNAME,
            "Persisted Admin",
            PasswordUtil.hash("persisted123")
        );
        admin.setRole(UserRole.ADMIN);
        userDao.create(admin);

        TestClientHandler client = new TestClientHandler();
        Response response = authService.login(
            client,
            new Request(RequestType.LOGIN, new LoginRequest(AuthService.BOOTSTRAP_ADMIN_USERNAME, "persisted123"))
        );

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals(AuthService.BOOTSTRAP_ADMIN_USERNAME, client.getCurrentUsername());
        assertTrue(RealtimeDatabase.isUserOnline(AuthService.BOOTSTRAP_ADMIN_USERNAME));
    }

    @Test
    void loginStoresBootstrapAdminAsAdminRuntimeType() {
        SQLiteHelper.update("DELETE FROM Users WHERE username = ?", AuthService.BOOTSTRAP_ADMIN_USERNAME);
        SQLiteHelper.init();

        TestClientHandler client = new TestClientHandler();
        Response response = authService.login(
            client,
            new Request(
                RequestType.LOGIN,
                new LoginRequest(AuthService.BOOTSTRAP_ADMIN_USERNAME, AuthService.BOOTSTRAP_ADMIN_PASSWORD)
            )
        );

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        User activeUser = RealtimeDatabase.getActiveUser(AuthService.BOOTSTRAP_ADMIN_USERNAME);
        assertInstanceOf(Admin.class, activeUser);
        assertEquals(UserRole.ADMIN, activeUser.getRole());
    }

    @Test
    void loginFailsWhenUserIsBanned() {
        String username = uniqueUsername("banned-user");
        User user = createUser(username, "bannedNick", "secret123");
        // Đổi trạng thái sang BANNED rồi lưu ngược lại DB để test đúng nhánh chặn tài khoản.
        user.setStatus(UserStatus.BANNED);
        userDao.save(user, false);

        Response response = authService.login(
            new TestClientHandler(),
            new Request(RequestType.LOGIN, new LoginRequest(username, "secret123"))
        );

        // Dù password đúng nhưng tài khoản bị khóa thì vẫn phải fail.
        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("You have been banned", response.getMessage());
        assertFalse(RealtimeDatabase.isUserOnline(username));
    }

    @Test
    void loginFailsWhenAnotherSessionIsAlreadyActive() {
        String username = uniqueUsername("active-session");
        User user = createUser(username, "sessionNick", "secret123");

        // Client đầu tiên đã login trước đó và đang được giữ trong RealtimeDatabase.
        TestClientHandler activeClient = new TestClientHandler();
        activeClient.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(activeClient, user);

        // Client thứ hai cố login cùng tài khoản => phải bị từ chối vì đã có session khác.
        Response response = authService.login(
            new TestClientHandler(),
            new Request(RequestType.LOGIN, new LoginRequest(username, "secret123"))
        );

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Another session is already active", response.getMessage());
    }

    @Test
    void logoutFailsWhenClientHasNotLoggedIn() {
        // Client chưa có currentUsername => không có session hợp lệ để logout.
        Response response = authService.logout(new TestClientHandler());

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Invalid session", response.getMessage());
    }

    @Test
    void logoutFailsWhenSessionIsMissingFromRealtimeDatabase() {
        String username = uniqueUsername("inactive-session");
        TestClientHandler client = new TestClientHandler();
        // Client nghĩ rằng mình đã login...
        client.setCurrentUsername(username);
        // ...nhưng RealtimeDatabase không có session tương ứng.
        // Đây là case lệch trạng thái giữa client và runtime database.

        Response response = authService.logout(client);

        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Session is inactive", response.getMessage());
    }

    private User createUser(String username, String nickname, String rawPassword) {
        // Helper tạo user thật trong DB với password đã hash,
        // để các test login có dữ liệu đầu vào giống hệ thống thật.
        User user = new User(username, nickname, PasswordUtil.hash(rawPassword));
        userDao.create(user);
        createdUsernames.add(username);
        return user;
    }

    private String uniqueUsername(String prefix) {
        // Tạo username ngắn gọn nhưng đủ unique để không bị trùng giữa các lần chạy test.
        String normalizedPrefix = prefix.replace("-", "");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        int maxPrefixLength = Math.max(4, 20 - suffix.length());
        if (normalizedPrefix.length() > maxPrefixLength)
            normalizedPrefix = normalizedPrefix.substring(0, maxPrefixLength);
        return normalizedPrefix + suffix;
    }

    private static class TestClientHandler extends ClientHandler {
        TestClientHandler() {
            // Không cần socket thật cho unit test này, nên truyền null.
            super(null);
        }

        @Override
        public boolean isInSession() {
            // Trong test, chỉ cần currentUsername khác null là coi như đang có session.
            // Việc override chỗ này giúp tránh phụ thuộc vào socket thật của ClientHandler.
            return getCurrentUsername() != null;
        }
    }
}
