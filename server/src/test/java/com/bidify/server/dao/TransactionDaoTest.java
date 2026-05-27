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

import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.utility.IdGenerator;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Item;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.utility.PasswordUtil;

/**
 * Lớp kiểm thử (Unit Test) cho {@link TransactionDao}.
 * Kiểm tra các tính năng:
 * - Tạo giao dịch ví mới (Transaction) và tìm lại theo tên đăng nhập (username).
 * - Kiểm tra danh sách giao dịch trả về được sắp xếp giảm dần theo thời gian tạo (createdAt DESC).
 * - Xóa lịch sử giao dịch theo tên đăng nhập (deleteByUsername).
 * - Xóa lịch sử giao dịch theo ID cuộc đấu giá (deleteByAuctionId).
 * Có chú thích chi tiết bằng tiếng Việt.
 */
class TransactionDaoTest {

    private final TransactionDao transactionDao = TransactionDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();

    private final List<String> createdUsernames = new ArrayList<>();
    private final List<String> createdItemIds = new ArrayList<>();
    private final List<String> createdAuctionIds = new ArrayList<>();

    @BeforeAll
    static void initDatabase() {
        SQLiteHelper.init();
    }

    @BeforeEach
    void setUp() {
        createdUsernames.clear();
        createdItemIds.clear();
        createdAuctionIds.clear();
    }

    @AfterEach
    void tearDown() {
        // Xóa Transactions trước tiên
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Transactions WHERE username = ?", username);
        }

        for (String auctionId : createdAuctionIds) {
            SQLiteHelper.update("DELETE FROM Transactions WHERE auctionId = ?", auctionId);
            SQLiteHelper.update("DELETE FROM Bids WHERE auctionId = ?", auctionId);
            SQLiteHelper.update("DELETE FROM Auctions WHERE id = ?", auctionId);
        }

        // Xóa các sản phẩm
        for (String itemId : createdItemIds) {
            SQLiteHelper.update("DELETE FROM ItemImageLinks WHERE itemId = ?", itemId);
            SQLiteHelper.update("DELETE FROM Items WHERE id = ?", itemId);
        }

        // Xóa các user
        for (String username : createdUsernames) {
            SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
        }
    }

    /**
     * Ca kiểm thử: Thêm mới giao dịch thành công và truy vấn lại theo username.
     */
    @Test
    void createAndFindTransactionSuccessfully() {
        String username = createTestUser("user");
        Transaction tx = new Transaction(username, TransactionType.DEPOSIT, 500.0);

        transactionDao.create(tx);

        List<Transaction> list = transactionDao.findByUsername(username);

        assertNotNull(list);
        assertEquals(1, list.size());

        Transaction found = list.get(0);
        assertEquals(tx.getId(), found.getId());
        assertEquals(username, found.getUsername());
        assertEquals(TransactionType.DEPOSIT, found.getType());
        assertEquals(500.0, found.getAmount());
        assertNull(found.getAuctionId());
        assertNotNull(found.getCreatedAt());
    }

    /**
     * Ca kiểm thử: Truy vấn danh sách giao dịch sắp xếp giảm dần theo thời gian tạo (createdAt DESC).
     */
    @Test
    void findByUsernameReturnsSortedList() {
        String username = createTestUser("user");

        LocalDateTime now = TimeUtil.nowInVietnam();
        // Tạo 3 giao dịch với các mốc thời gian rõ ràng để kiểm tra sắp xếp giảm dần (DESC)
        Transaction tx1 = new Transaction(IdGenerator.genTransactionId(), now.minusMinutes(10), username, TransactionType.DEPOSIT, 100.0, null);
        Transaction tx2 = new Transaction(IdGenerator.genTransactionId(), now.minusMinutes(5), username, TransactionType.WITHDRAW, 200.0, null);
        Transaction tx3 = new Transaction(IdGenerator.genTransactionId(), now, username, TransactionType.AUCTION_PAY, 300.0, null);

        // Lưu vào DB
        transactionDao.create(tx1);
        transactionDao.create(tx2);
        transactionDao.create(tx3);

        List<Transaction> list = transactionDao.findByUsername(username);

        assertNotNull(list);
        assertEquals(3, list.size());

        // Giao dịch mới nhất (tx3) phải nằm ở vị trí đầu tiên, cũ nhất (tx1) ở cuối cùng
        assertEquals(tx3.getId(), list.get(0).getId());
        assertEquals(tx2.getId(), list.get(1).getId());
        assertEquals(tx1.getId(), list.get(2).getId());
    }

    /**
     * Ca kiểm thử: Xóa giao dịch ví của người dùng theo username thành công.
     */
    @Test
    void deleteByUsernameSuccessfully() {
        String userA = createTestUser("userA");
        String userB = createTestUser("userB");

        Transaction txA = new Transaction(userA, TransactionType.DEPOSIT, 100.0);
        Transaction txB = new Transaction(userB, TransactionType.DEPOSIT, 200.0);

        transactionDao.create(txA);
        transactionDao.create(txB);

        // Kiểm tra xem cả 2 giao dịch đã có trong DB
        assertEquals(1, transactionDao.findByUsername(userA).size());
        assertEquals(1, transactionDao.findByUsername(userB).size());

        // Xóa giao dịch của userA
        transactionDao.deleteByUsername(userA);

        // userA không còn giao dịch nào, userB vẫn còn nguyên
        assertTrue(transactionDao.findByUsername(userA).isEmpty());
        assertEquals(1, transactionDao.findByUsername(userB).size());
    }

    /**
     * Ca kiểm thử: Xóa giao dịch ví liên kết theo ID cuộc đấu giá thành công.
     */
    @Test
    void deleteByAuctionIdSuccessfully() {
        String bidder = createTestUser("bidder");
        String seller = createTestUser("seller");
        
        // Tạo cuộc đấu giá
        Item item = new Item(seller, "Sản phẩm đấu giá", "Mô tả", "Điện tử", "Khác");
        itemDao.create(item);
        createdItemIds.add(item.getId());

        Auction auction = new Auction(
            seller, 
            item.getId(), 
            1000.0, 
            TimeUtil.nowInVietnam().minusHours(1),
            TimeUtil.nowInVietnam().plusHours(1)
        );
        auction.setAuctionName("Đấu giá test");
        auction.setDescription("Mô tả đấu giá test");
        auctionDao.create(auction);
        createdAuctionIds.add(auction.getId());

        // Giao dịch 1: Nạp tiền (không liên kết đấu giá)
        Transaction txNormal = new Transaction(bidder, TransactionType.DEPOSIT, 1000.0);
        // Giao dịch 2: Thanh toán thắng đấu giá (có liên kết auctionId)
        Transaction txAuction = new Transaction(bidder, TransactionType.AUCTION_PAY, 1000.0, auction.getId());

        transactionDao.create(txNormal);
        transactionDao.create(txAuction);

        // Tổng cộng bidder có 2 giao dịch
        assertEquals(2, transactionDao.findByUsername(bidder).size());

        // Xóa các giao dịch liên quan tới cuộc đấu giá
        transactionDao.deleteByAuctionId(auction.getId());

        // Bidder chỉ còn lại giao dịch nạp tiền thông thường
        List<Transaction> remaining = transactionDao.findByUsername(bidder);
        assertEquals(1, remaining.size());
        assertEquals(txNormal.getId(), remaining.get(0).getId());
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
