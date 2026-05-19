package com.bidify.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.model.Event;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.WalletRequest;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;

/**
 * Lớp kiểm thử (Unit Test) dành cho TransactionService.
 * Chịu trách nhiệm kiểm tra các luồng liên quan đến tiền bạc như: Nạp tiền (Deposit), Rút tiền (Withdraw) và Lịch sử giao dịch.
 */
class TransactionServiceTest {
    // Khởi tạo các Service và DAO theo mẫu Singleton để thao tác với DB
    private final TransactionService transactionService = TransactionService.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    // Danh sách lưu trữ các username được tạo ra trong quá trình chạy test để dọn dẹp sau khi test xong
    private final List<String> createdUsernames = new ArrayList<>();

    // dùng để khởi tạo cơ sở dữ liệu
    @BeforeAll
    static void initDatabase() {
        // Đảm bảo cấu trúc bảng (schema) của SQLite đã được khởi tạo trước khi bất kỳ test nào chạy
        SQLiteHelper.init();
    }

    // dùng để thiết lập up
    @BeforeEach
    void setUp() {
        // Làm sạch bộ nhớ tạm (RAM) và danh sách dọn dẹp trước mỗi test để đảm bảo các test độc lập, không ảnh hưởng lẫn nhau
        RealtimeDatabase.clearAll();
        createdUsernames.clear();
    }

    // dùng để tear down
    @AfterEach
    void tearDown() {
        RealtimeDatabase.clearAll();
        
        // Dọn dẹp dữ liệu giao dịch và users để không làm rác DB thật
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Transactions WHERE username = ?", username);
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        }
    }

    // dùng để nạp tiền successfully increases số dư and creates giao dịch
    @Test
    void depositSuccessfullyIncreasesBalanceAndCreatesTransaction() {
        // 1. Chuẩn bị dữ liệu (Arrange)
        String username = uniqueUsername("user");
        User user = createTestUser(username, "pass123");
        
        // Giả lập một client đang đăng nhập vào hệ thống
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Tạo payload yêu cầu nạp 500.0
        Request request = new Request(RequestType.DEPOSIT, new WalletRequest(500.0));
        
        // 2. Thực thi (Act)
        Response response = transactionService.deposit(client, request);

        // 3. Kiểm tra kết quả (Assert)
        // Đảm bảo request được xử lý thành công
        assertEquals(RequestStatus.SUCCESS, response.getStatus());

        // Đọc lại thông tin user từ DB để xác nhận số dư đã tăng đúng 500
        User updatedUser = userDao.findByUsername(username);
        assertEquals(500.0, updatedUser.getWallet().getBalance());

        // Đảm bảo một bản ghi lịch sử giao dịch (Transaction) đã được lưu xuống DB
        List<Transaction> transactions = transactionDao.findByUsername(username);
        assertEquals(1, transactions.size());
        assertEquals(TransactionType.DEPOSIT, transactions.get(0).getType());
        assertEquals(500.0, transactions.get(0).getAmount());
    }

    // dùng để nạp tiền fails when số tiền kiểm tra xem negative
    @Test
    void depositFailsWhenAmountIsNegative() {
        // 1. Chuẩn bị dữ liệu (Arrange)
        String username = uniqueUsername("user");
        User user = createTestUser(username, "pass123");
        
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Cố tình gửi số tiền âm (-100.0)
        Request request = new Request(RequestType.DEPOSIT, new WalletRequest(-100.0));
        
        // 2. Thực thi (Act)
        Response response = transactionService.deposit(client, request);

        // 3. Kiểm tra (Assert) - Giao dịch phải bị từ chối
        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertTrue(response.getMessage().toLowerCase().contains("deposit"));
        
        // Đảm bảo KHÔNG CÓ lịch sử giao dịch nào được tạo ra do request bị lỗi
        List<Transaction> transactions = transactionDao.findByUsername(username);
        assertTrue(transactions.isEmpty());
    }

    // dùng để rút tiền successfully decreases số dư and creates giao dịch
    @Test
    void withdrawSuccessfullyDecreasesBalanceAndCreatesTransaction() {
        // 1. Chuẩn bị (Arrange)
        String username = uniqueUsername("user");
        User user = createTestUser(username, "pass123");
        user.getWallet().deposit(1000.0); // Nạp trước 1000 để có tiền rút
        userDao.save(user, false);
        
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Yêu cầu rút 400.0
        Request request = new Request(RequestType.WITHDRAW, new WalletRequest(400.0));
        
        // 2. Thực thi (Act)
        Response response = transactionService.withdraw(client, request);

        // 3. Kiểm tra (Assert)
        assertEquals(RequestStatus.SUCCESS, response.getStatus());

        // Kiểm tra xem số dư có bị trừ chính xác không (1000 - 400 = 600)
        User updatedUser = userDao.findByUsername(username);
        assertEquals(600.0, updatedUser.getWallet().getBalance());

        // Kiểm tra lịch sử giao dịch Rút tiền đã được tạo
        List<Transaction> transactions = transactionDao.findByUsername(username);
        assertEquals(1, transactions.size());
        assertEquals(TransactionType.WITHDRAW, transactions.get(0).getType());
        assertEquals(400.0, transactions.get(0).getAmount());
    }

    // dùng để rút tiền fails when insufficient số dư
    @Test
    void withdrawFailsWhenInsufficientBalance() {
        // 1. Chuẩn bị dữ liệu
        String username = uniqueUsername("user");
        User user = createTestUser(username, "pass123");
        user.getWallet().deposit(100.0); // Trong ví chỉ có 100
        userDao.save(user, false);
        
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Yêu cầu rút 500.0 (nhiều hơn số dư)
        Request request = new Request(RequestType.WITHDRAW, new WalletRequest(500.0));
        
        // 2. Thực thi
        Response response = transactionService.withdraw(client, request);

        // 3. Kiểm tra - Yêu cầu rút tiền phải bị từ chối
        assertEquals(RequestStatus.FAILED, response.getStatus());
        assertEquals("Insufficient available balance", response.getMessage());

        // Xác nhận số tiền trong ví không bị thay đổi sau giao dịch thất bại
        User updatedUser = userDao.findByUsername(username);
        assertEquals(100.0, updatedUser.getWallet().getBalance()); 
    }

    // dùng để lấy người dùng danh sách giao dịch returns lịch sử
    @Test
    void getUserTransactionsReturnsHistory() {
        // 1. Chuẩn bị dữ liệu
        String username = uniqueUsername("user");
        User user = createTestUser(username, "pass123");
        
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveUser(client, user);

        // Bơm trực tiếp 2 giao dịch giả vào cơ sở dữ liệu để test luồng đọc
        transactionDao.create(new Transaction(username, TransactionType.DEPOSIT, 1000.0));
        transactionDao.create(new Transaction(username, TransactionType.WITHDRAW, 200.0));

        // 2. Thực thi
        Response response = transactionService.getUserTransactions(client);

        // 3. Kiểm tra kết quả
        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        List<?> history = (List<?>) response.getData();
        assertEquals(2, history.size()); // Phải trả về chính xác 2 giao dịch vừa lưu
    }

    // --- Helper Methods ---
    
    /**
     * Phương thức tiện ích để tạo User trực tiếp xuống Database.
     */
    // dùng để tạo test người dùng
    private User createTestUser(String username, String rawPassword) {
        User user = new User(username, username, PasswordUtil.hash(rawPassword));
        userDao.create(user);
        createdUsernames.add(username);
        return user;
    }

    /**
     * Trả về chuỗi username ngẫu nhiên để tránh xung đột (Conflict) giữa các Test.
     */
    // dùng để unique username
    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    // --- Mock Classes ---
    
    /**
     * Giả lập (Mock) kết nối từ Socket của Client.
     * Cho phép test logic Server mà không cần mở Port mạng thật.
     */
    private static class TestClientHandler extends ClientHandler {
        // dùng để test client trình xử lý
        TestClientHandler() {
            // dùng để super
            super(null);
        }

        // dùng để kiểm tra xem trong phiên làm việc
        @Override
        public boolean isInSession() {
            return getCurrentUsername() != null;
        }

        // dùng để gửi sự kiện
        @Override
        public void sendEvent(Event event) {
            // Ghi đè lại hàm này để vô hiệu hoá việc bắn Event qua Socket thật, tránh lỗi NullPointerException
        }
    }
}
