package com.bidify.server.service.auction;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.GetAuctionDetailRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.SearchAuctionRequest;
import com.bidify.common.utility.TimeUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Admin;
import com.bidify.server.model.Auction;
import com.bidify.server.model.AutoBid;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

/**
 * Lớp kiểm thử (Unit Test) cho AuctionQueryService.
 * Sử dụng chuỗi ASCII để tránh lỗi mã hóa ký tự.
 * Bình luận mã nguồn bằng tiếng Việt.
 */
public class AuctionQueryServiceTest {

    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    private AuctionQueryService queryService;

    @BeforeAll
    public static void setUpClass() {
        SQLiteHelper.init();
    }

    @BeforeEach
    public void setUp() {
        queryService = new AuctionQueryService(auctionDao, new AuctionDtoAssembler());
        cleanDb();
        RealtimeDatabase.clearAll();
    }

    @AfterEach
    public void tearDown() {
        cleanDb();
        RealtimeDatabase.clearAll();
    }

    private void cleanDb() {
        SQLiteHelper.update("DELETE FROM Users");
        SQLiteHelper.update("DELETE FROM ItemImageLinks");
        SQLiteHelper.update("DELETE FROM Items");
        SQLiteHelper.update("DELETE FROM Auctions");
    }

    /**
     * Ca kiểm thử: Tìm kiếm đấu giá (Search) theo tên sản phẩm, danh mục, người bán, v.v.
     */
    @Test
    public void testSearchAuctions() {
        String sellerName = "search_seller";
        User seller = new User(sellerName, "Search Seller", "pass");
        userDao.create(seller);

        // Tạo các sản phẩm khác nhau
        Item item1 = new Item(sellerName, "Rolex Watch", "Authentic", "Antiques", "Watch");
        Item item2 = new Item(sellerName, "Adidas Jacket", "Black color", "Fashion", "Jacket");
        itemDao.create(item1);
        itemDao.create(item2);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction1 = new Auction(
            "auc-search-1", now, "Rolex", "Rolex desc", sellerName, item1.getId(),
            null, 1000.0, 100.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        Auction auction2 = new Auction(
            "auc-search-2", now, "Adidas", "Adidas desc", sellerName, item2.getId(),
            null, 200.0, 20.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        RealtimeDatabase.addRuntimeAuction(auction1);
        RealtimeDatabase.addRuntimeAuction(auction2);

        // 1. Tìm theo từ khóa "rolex" (tên sản phẩm)
        SearchAuctionRequest req1 = new SearchAuctionRequest("rolex");
        Response res1 = queryService.search(new Request(RequestType.SEARCH_AUCTIONS, req1));
        assertEquals(RequestStatus.SUCCESS, res1.getStatus());
        List<AuctionDto> data1 = (List<AuctionDto>) res1.getData();
        assertEquals(1, data1.size());
        assertEquals("auc-search-1", data1.get(0).getId());

        // 2. Tìm theo từ khóa "Fashion" (danh mục)
        SearchAuctionRequest req2 = new SearchAuctionRequest("Fashion");
        Response res2 = queryService.search(new Request(RequestType.SEARCH_AUCTIONS, req2));
        assertEquals(RequestStatus.SUCCESS, res2.getStatus());
        List<AuctionDto> data2 = (List<AuctionDto>) res2.getData();
        assertEquals(1, data2.size());
        assertEquals("auc-search-2", data2.get(0).getId());
    }

    /**
     * Ca kiểm thử: Lấy chi tiết đấu giá (getDetail) hỗ trợ fallback xuống database SQLite khi không có trong RAM.
     */
    @Test
    public void testGetDetailFallbackToDatabase() {
        String sellerName = "detail_seller";
        User seller = new User(sellerName, "Detail Seller", "pass");
        userDao.create(seller);

        Item item = new Item(sellerName, "Samsung Phone", "Mô tả", "Tech", "Phone");
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        // Đấu giá đã kết thúc, không nằm trong runtime RAM mà lưu dưới SQLite
        Auction dbAuction = new Auction(
            "auc-in-db", now, "Samsung Detail", "Samsung desc", sellerName, item.getId(),
            null, 500.0, 50.0, now.minusDays(3), now.minusDays(2), AuctionStatus.COMPLETED
        );
        auctionDao.create(dbAuction);

        GetAuctionDetailRequest detailReq = new GetAuctionDetailRequest(dbAuction.getId());
        Request request = new Request(RequestType.GET_ITEM_DETAIL, detailReq);

        // Giả lập client chưa login (để không check autobid)
        TestClientHandler client = new TestClientHandler();

        Response response = queryService.getDetail(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Get auction detail successfully", response.getMessage());
        AuctionDto dto = (AuctionDto) response.getData();
        assertNotNull(dto);
        assertEquals("Samsung Phone", dto.getAuctionName());
        assertEquals(AuctionStatus.COMPLETED.toString(), dto.getStatus());
    }

    /**
     * Ca kiểm thử: Lấy chi tiết đấu giá kèm trạng thái AutoBid của người dùng hiện tại đang login.
     */
    @Test
    public void testGetDetailWithCurrentUserAutoBid() {
        String sellerName = "detail_seller_2";
        String bidderName = "bidder_autobid";
        
        User seller = new User(sellerName, "Detail Seller 2", "pass");
        userDao.create(seller);

        User bidder = new User(bidderName, "Bidder", "pass");
        userDao.create(bidder);

        Item item = new Item(sellerName, "Sony Camera", "Mô tả", "Tech", "Camera");
        itemDao.create(item);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-autobid-detail", now, "Sony Camera", "Desc", sellerName, item.getId(),
            null, 800.0, 50.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        // Cấu hình AutoBid cho bidder này
        AutoBid autoBid = new AutoBid(auction.getId(), bidderName, 1500.0);
        auction.upsertAutoBid(autoBid);
        RealtimeDatabase.addRuntimeAuction(auction);

        // Giả lập client của bidder đang online
        TestClientHandler client = new TestClientHandler();
        client.setCurrentUsername(bidderName);
        RealtimeDatabase.addActiveUser(client, bidder);

        GetAuctionDetailRequest detailReq = new GetAuctionDetailRequest(auction.getId());
        Request request = new Request(RequestType.GET_ITEM_DETAIL, detailReq);

        Response response = queryService.getDetail(client, request);

        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        AuctionDto dto = (AuctionDto) response.getData();
        assertNotNull(dto);
        assertTrue(dto.isCurrentUserAutoBidActive(), "AutoBid của người xem phải được đánh dấu active");
        assertEquals(1500.0, dto.getCurrentUserAutoBidMax(), 0.001);
    }

    /**
     * Ca kiểm thử: Lấy danh sách đấu giá cho Admin (getAdminAuctions).
     * Phải kiểm tra phân quyền Admin: Cho phép admin và chặn người dùng thường.
     */
    @Test
    public void testGetAdminAuctionsAuthorization() {
        String sellerName = "seller_admin_test";
        User seller = new User(sellerName, "Seller Nick", "pass");
        userDao.create(seller);

        Admin admin = new Admin("admin_user", "Admin Nick", "pass");
        userDao.create(admin);

        LocalDateTime now = TimeUtil.nowInVietnam();
        Auction auction = new Auction(
            "auc-admin-1", now, "Rolex", "Desc", sellerName, "item-9",
            null, 100.0, 10.0, now.plusDays(1), now.plusDays(2), AuctionStatus.ACTIVE
        );
        auctionDao.create(auction);

        Request request = new Request(RequestType.GET_ADMIN_AUCTIONS, null);

        // 1. Client là người dùng thường -> Báo lỗi phân quyền
        TestClientHandler normalClient = new TestClientHandler();
        normalClient.setCurrentUsername(sellerName);
        RealtimeDatabase.addActiveUser(normalClient, seller);

        Response response1 = queryService.getAdminAuctions(normalClient, request);
        assertEquals(RequestStatus.FAILED, response1.getStatus());
        assertTrue(response1.getMessage().contains("Admin permission required"));

        // 2. Client là Admin -> Thành công và trả về danh sách đấu giá
        TestClientHandler adminClient = new TestClientHandler();
        adminClient.setCurrentUsername(admin.getUsername());
        RealtimeDatabase.addActiveUser(adminClient, admin);

        Response response2 = queryService.getAdminAuctions(adminClient, request);
        assertEquals(RequestStatus.SUCCESS, response2.getStatus());
        List<AuctionDto> data = (List<AuctionDto>) response2.getData();
        assertEquals(1, data.size());
        assertEquals("auc-admin-1", data.get(0).getId());
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
