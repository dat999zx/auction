package com.bidify.common.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.bidify.common.enums.RequestType;

public class RequestTest {
    
    @Test
    void defaultConstructorGeneratesId() {
        // Tạo Request mà không truyền tham số nào
        Request request = new Request();
        
        // Kiểm tra ID được generate tự động
        // - ID phải không null
        // - ID phải là string khác rỗng (từ IdGenerator)
        assertNotNull(request.getId());
        assertFalse(request.getId().isEmpty());
    }
    
    @Test
    void multipleRequestsHaveDifferentIds() {
        // Tạo 2 Request khác nhau
        Request req1 = new Request();
        Request req2 = new Request();
        
        // Kiểm tra mỗi Request có ID độc nhất
        // Quan trọng để tracking request-response pairs
        assertNotEquals(req1.getId(), req2.getId());
    }
    
    @Test
    void constructorWithTypeAndData() {
        // Chuẩn bị data cho login request
        var loginData = new Object();
        
        // Tạo Request với type LOGIN và data
        Request request = new Request(RequestType.LOGIN, loginData);
        
        // Kiểm tra tất cả fields được set đúng
        // - ID phải được generate
        // - Type phải là LOGIN
        // - Data phải là object được truyền vào
        assertNotNull(request.getId());
        assertEquals(RequestType.LOGIN, request.getType());
        assertSame(loginData, request.getData());
    }
    
    @Test
    void createRegisterRequest() {
        // Chuẩn bị data register
        var regData = new Object();
        
        // Tạo REGISTER request
        Request request = new Request(RequestType.REGISTER, regData);
        
        // Kiểm tra request được tạo đúng cho register flow
        assertEquals(RequestType.REGISTER, request.getType());
        assertEquals(regData, request.getData());
    }
    
    @Test
    void createAuctionListRequest() {
        // Tạo request lấy danh sách auction (không cần data)
        Request request = new Request(RequestType.GET_LIVE_AUCTIONS, null);
        
        // Kiểm tra request được tạo đúng
        // - Type phải GET_LIVE_AUCTIONS
        // - Data phải null (vì ko cần truyền tham số gì)
        assertEquals(RequestType.GET_LIVE_AUCTIONS, request.getType());
        assertNull(request.getData());
    }
    
    @Test
    void createBidRequest() {
        // Chuẩn bị data bid (thường là JSON object chứa auctionId, amount)
        var bidData = new Object();
        
        // Tạo PLACE_BID request
        Request request = new Request(RequestType.PLACE_BID, bidData);
        
        // Kiểm tra bid request được tạo đúng
        assertEquals(RequestType.PLACE_BID, request.getType());
        assertEquals(bidData, request.getData());
    }
    
    @Test
    void requestIdNeverNull() {
        // Tạo 2 requests khác nhau: 1 cái với default constructor, 1 cái có tham số
        Request request1 = new Request();
        Request request2 = new Request(RequestType.LOGIN, null);
        
        // Kiểm tra cả 2 đều có ID (không bao giờ null)
        // Điều này đảm bảo client có thể track request/response pairs
        assertNotNull(request1.getId());
        assertNotNull(request2.getId());
    }
}
