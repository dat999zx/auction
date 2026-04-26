package com.bidify.common.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.bidify.common.enums.RequestStatus;

public class ResponseTest {
    
    @Test
    void defaultConstructorSetsNotFound() {
        // Tạo Response mà không truyền tham số nào
        Response response = new Response();
        
        // Kiểm tra default values
        // - Status phải là NOT_FOUND (default value từ constructor)
        // - Message và Data phải null (vì không set)
        assertEquals(RequestStatus.NOT_FOUND, response.getStatus());
        assertNull(response.getMessage());
        assertNull(response.getData());
    }
    
    @Test
    void constructorWithStatusAndMessage() {
        // Tạo Response với status và message (dùng cho login/register response)
        Response response = new Response(RequestStatus.SUCCESS, "Login successful");
        
        // Kiểm tra các field được set đúng
        // - Status phải là SUCCESS (từ tham số constructor)
        // - Message phải là "Login successful" (từ tham số constructor)
        // - Data phải null (không được truyền)
        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("Login successful", response.getMessage());
        assertNull(response.getData());
    }
    
    @Test
    void constructorWithStatusMessageAndData() {
        // Chuẩn bị một object data (giả sử là user info sau khi đăng ký)
        Object userData = new Object();
        
        // Tạo Response với đầy đủ 3 tham số (status, message, data)
        Response response = new Response(RequestStatus.SUCCESS, "User registered", userData);
        
        // Kiểm tra tất cả fields được lưu trữ đúng
        // - Status phải SUCCESS
        // - Message phải khớp
        // - Data phải là object userData (dùng assertSame để kiểm tra reference)
        assertEquals(RequestStatus.SUCCESS, response.getStatus());
        assertEquals("User registered", response.getMessage());
        assertSame(userData, response.getData());
    }
    
    @Test
    void setAndGetId() {
        // Tạo một Response bình thường
        Response response = new Response(RequestStatus.SUCCESS, "Test");
        String testId = "resp-12345";
        
        // Set ID cho response (để track request-response pairs)
        response.setId(testId);
        
        // Kiểm tra ID được lưu trữ và retrieval đúng
        // Điều này quan trọng để match response với request gốc (pair by ID)
        assertEquals(testId, response.getId());
    }
    
    @Test
    void responseWithErrorStatus() {
        // Tạo error response (khi validation fail hoặc exception xảy ra)
        Response errorResponse = new Response(RequestStatus.ERROR, "Invalid input");
        
        // Kiểm tra error status và message được truyền đúng
        // Client sẽ dùng để hiển thị lỗi cho user
        assertEquals(RequestStatus.ERROR, errorResponse.getStatus());
        assertEquals("Invalid input", errorResponse.getMessage());
    }
    
    @Test
    void responseWithComplexData() {
        // Tạo một HashMap phức tạp (giả sử là user info, auction details, ...)
        var data = new java.util.HashMap<String, String>();
        data.put("key", "value");
        
        // Lưu map này vào Response data field
        Response response = new Response(RequestStatus.SUCCESS, "OK", data);
        
        // Kiểm tra complex object data được lưu trữ đúng (not corrupted)
        // Đảm bảo Response có thể handle mọi loại data object
        assertEquals(data, response.getData());
    }
}
