package com.bidify.common.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.bidify.common.enums.EventType;

public class EventTest {
    
    // dùng để default constructor
    @Test
    void defaultConstructor() {
        // Tạo Event mà không truyền tham số nào
        Event event = new Event();
        
        // Kiểm tra default values
        // - Type phải null (không set)
        // - Message phải null (không set)
        // - Data phải null (không set)
        assertNull(event.getType());
        assertNull(event.getMessage());
        assertNull(event.getData());
    }
    
    // dùng để constructor với type and tin nhắn
    @Test
    void constructorWithTypeAndMessage() {
        // Tạo Event với type và message (dùng cho server push notifications)
        Event event = new Event(EventType.BID_PLACED, "New bid placed");
        
        // Kiểm tra các field được set đúng
        // - Type phải là BID_PLACED (để client biết xử lý gì)
        // - Message phải là "New bid placed" (display cho user)
        // - Data phải null (không truyền payload)
        assertEquals(EventType.BID_PLACED, event.getType());
        assertEquals("New bid placed", event.getMessage());
        assertNull(event.getData());
    }
    
    // dùng để constructor với type tin nhắn and data
    @Test
    void constructorWithTypeMessageAndData() {
        // Chuẩn bị data event (giả sử là bid info)
        var bidData = new Object();
        
        // Tạo Event với đầy đủ 3 tham số (type, message, data)
        Event event = new Event(EventType.BID_PLACED, "Bid updated", bidData);
        
        // Kiểm tra tất cả fields được lưu trữ đúng
        // - Type phải BID_PLACED
        // - Message phải khớp
        // - Data phải là object bidData (chứa bid amount, bidder, etc)
        assertEquals(EventType.BID_PLACED, event.getType());
        assertEquals("Bid updated", event.getMessage());
        assertSame(bidData, event.getData());
    }
    
    // dùng để auction created sự kiện
    @Test
    void auctionCreatedEvent() {
        // Tạo event khi auction được tạo
        Event event = new Event(EventType.AUCTION_CREATED, "New auction created");
        
        // Kiểm tra AUCTION_CREATED event được tạo đúng
        assertEquals(EventType.AUCTION_CREATED, event.getType());
        assertEquals("New auction created", event.getMessage());
    }
    
    // dùng để auction ended sự kiện
    @Test
    void auctionEndedEvent() {
        // Tạo event khi auction kết thúc (có winner)
        Event event = new Event(EventType.AUCTION_ENDED, "Auction closed", "winner_user");
        
        // Kiểm tra AUCTION_ENDED event có data (winner username)
        assertEquals(EventType.AUCTION_ENDED, event.getType());
        assertEquals("winner_user", event.getData());
    }
    
    // dùng để server notice sự kiện
    @Test
    void serverNoticeEvent() {
        // Tạo event thông báo từ server (bảo trì, etc)
        Event event = new Event(EventType.SERVER_NOTICE, "Maintenance in 5 minutes");
        
        // Kiểm tra SERVER_NOTICE event
        assertEquals(EventType.SERVER_NOTICE, event.getType());
        assertEquals("Maintenance in 5 minutes", event.getMessage());
    }
    
    // dùng để event với complex data
    @Test
    void eventWithComplexData() {
        // Tạo một HashMap phức tạp (bid history, auction details, etc)
        var complexData = new java.util.HashMap<String, Integer>();
        complexData.put("bidCount", 5);
        complexData.put("currentPrice", 1500);
        
        // Lưu map này vào Event data field
        Event event = new Event(EventType.BID_PLACED, "Multiple bids", complexData);
        
        // Kiểm tra complex object data được lưu trữ đúng (not corrupted)
        // Đảm bảo Event có thể handle mọi loại data object
        assertEquals(complexData, event.getData());
    }
}
