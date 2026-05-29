package com.bidify.server.dispatcher;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.network.ClientHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử cho RequestDispatcher.
 * Đảm bảo các logic đăng ký route, điều phối request và xử lý lỗi hoạt động chính xác.
 */
public class RequestDispatcherTest {

    private RequestDispatcher dispatcher;

    @BeforeEach
    public void setUp() {
        dispatcher = RequestDispatcher.getInstance();
        // Dọn dẹp các route đã đăng ký trước đó để tránh xung đột giữa các test case
        dispatcher.clear();
    }

    @AfterEach
    public void tearDown() {
        // Dọn dẹp sau khi chạy xong mỗi test case
        dispatcher.clear();
    }

    @Test
    public void testGetInstance() {
        // Đảm bảo getInstance luôn trả về cùng một instance duy nhất (Singleton)
        RequestDispatcher instance1 = RequestDispatcher.getInstance();
        RequestDispatcher instance2 = RequestDispatcher.getInstance();
        assertSame(instance1, instance2, "RequestDispatcher phải là một Singleton");
    }

    @Test
    public void testRegisterAndDispatchSuccessfully() {
        // Đăng ký một route giả lập cho LOGIN
        RequestType type = RequestType.LOGIN;
        RequestHandler mockHandler = (client, req) -> new Response(RequestStatus.SUCCESS, "Success routing test");
        
        dispatcher.register(type, mockHandler);
        
        assertTrue(dispatcher.hasRoute(type), "Route LOGIN phải được đăng ký thành công");

        // Tạo request giả lập
        Request request = new Request(type, null);
        
        // Điều phối request
        Response response = dispatcher.dispatch(null, request);
        
        assertNotNull(response, "Response không được null");
        assertEquals(RequestStatus.SUCCESS, response.getStatus(), "Trạng thái phản hồi phải là SUCCESS");
        assertEquals("Success routing test", response.getMessage(), "Thông điệp phản hồi phải chính xác");
    }

    @Test
    public void testRegisterDuplicateRouteThrowsException() {
        RequestType type = RequestType.REGISTER;
        RequestHandler handler1 = (client, req) -> new Response(RequestStatus.SUCCESS, "Handler 1");
        RequestHandler handler2 = (client, req) -> new Response(RequestStatus.SUCCESS, "Handler 2");

        // Đăng ký lần đầu thành công
        dispatcher.register(type, handler1);

        // Đăng ký lần hai cho cùng một RequestType phải ném ra IllegalStateException
        assertThrows(IllegalStateException.class, () -> {
            dispatcher.register(type, handler2);
        }, "Đăng ký route trùng lặp phải ném ra IllegalStateException");
    }

    @Test
    public void testDispatchNullRequestReturnsFailedResponse() {
        // Gửi request null
        Response response = dispatcher.dispatch(null, null);

        assertNotNull(response, "Response không được null khi xử lý lỗi");
        assertEquals(RequestStatus.FAILED, response.getStatus(), "Trạng thái phản hồi phải là FAILED");
        assertEquals("Invalid request", response.getMessage(), "Phải báo lỗi Invalid request");
    }

    @Test
    public void testDispatchNullRequestTypeReturnsFailedResponse() {
        // Request có type là null
        Request request = new Request(null, null);
        Response response = dispatcher.dispatch(null, request);

        assertNotNull(response, "Response không được null khi xử lý lỗi");
        assertEquals(RequestStatus.FAILED, response.getStatus(), "Trạng thái phản hồi phải là FAILED");
        assertEquals("Invalid request", response.getMessage(), "Phải báo lỗi Invalid request");
    }

    @Test
    public void testDispatchUnsupportedRequestTypeReturnsFailedResponse() {
        // Gửi request có type chưa được đăng ký handler
        RequestType unregisteredType = RequestType.LOGOUT;
        Request request = new Request(unregisteredType, null);

        Response response = dispatcher.dispatch(null, request);

        assertNotNull(response, "Response không được null khi xử lý lỗi");
        assertEquals(RequestStatus.FAILED, response.getStatus(), "Trạng thái phản hồi phải là FAILED");
        assertTrue(response.getMessage().contains("Unsupported request type"), 
                "Thông điệp lỗi phải chứa cụm 'Unsupported request type'");
    }

    @Test
    public void testUnregisterRouteSuccessfully() {
        RequestType type = RequestType.GET_PROFILE;
        RequestHandler handler = (client, req) -> new Response(RequestStatus.SUCCESS, "Profile OK");

        dispatcher.register(type, handler);
        assertTrue(dispatcher.hasRoute(type), "Route phải tồn tại sau khi đăng ký");

        // Hủy đăng ký route
        dispatcher.unregister(type);
        assertFalse(dispatcher.hasRoute(type), "Route không được tồn tại sau khi hủy đăng ký");
    }
}
