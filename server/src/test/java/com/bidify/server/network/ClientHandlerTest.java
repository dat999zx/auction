package com.bidify.server.network;

import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.Event;
import com.bidify.common.model.LogoutRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.dispatcher.RequestHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử cho ClientHandler.
 * Sử dụng MockSSLSocket để giả lập luồng dữ liệu mạng qua TCP socket và kiểm thử toàn bộ vòng đời kết nối.
 */
public class ClientHandlerTest {

    private RequestDispatcher dispatcher;

    @BeforeEach
    public void setUp() {
        dispatcher = RequestDispatcher.getInstance();
        dispatcher.clear();
    }

    @AfterEach
    public void tearDown() {
        dispatcher.clear();
    }

    @Test
    public void testSessionStateHandling() {
        MockSSLSocket mockSocket = new MockSSLSocket("".getBytes());
        ClientHandler handler = new ClientHandler(mockSocket);

        // Ban đầu chưa có username, chưa có session hợp lệ
        assertNull(handler.getCurrentUsername(), "Username mặc định phải là null");
        assertFalse(handler.isInSession(), "Phải không ở trong session khi chưa gán username");

        // Gán username và kiểm tra
        handler.setCurrentUsername("user_test");
        assertEquals("user_test", handler.getCurrentUsername(), "Username trả về phải khớp với giá trị đã gán");
        assertTrue(handler.isInSession(), "Phải ở trong session khi socket không null và username không null");

        // Gán username về null
        handler.setCurrentUsername(null);
        assertFalse(handler.isInSession(), "Phải rời khỏi session khi username là null");
    }

    @Test
    public void testSendResponseSuccessfully() throws IOException {
        MockSSLSocket mockSocket = new MockSSLSocket("".getBytes());
        ClientHandler handler = new ClientHandler(mockSocket);

        // Khởi tạo input và output stream của ClientHandler
        ByteArrayOutputStream outStream = mockSocket.getOutputStreamSpy();
        
        // Gọi run ở luồng chính nhưng truyền socket trống để kết thúc nhanh loop đọc dòng
        handler.run();

        // Gửi thử một response
        Response testResponse = new Response(RequestStatus.SUCCESS, "Hello Client");
        testResponse.setId("req-123");
        handler.sendResponse(testResponse);

        // Đọc dữ liệu ghi vào output stream
        String writtenData = outStream.toString().trim();
        assertFalse(writtenData.isEmpty(), "Dữ liệu phản hồi gửi cho client không được rỗng");
        
        Response receivedResponse = JsonUtil.fromJson(writtenData, Response.class);
        assertEquals(RequestStatus.SUCCESS, receivedResponse.getStatus());
        assertEquals("Hello Client", receivedResponse.getMessage());
        assertEquals("req-123", receivedResponse.getId());
    }

    @Test
    public void testSendEventAndOnEventSuccessfully() throws IOException {
        MockSSLSocket mockSocket = new MockSSLSocket("".getBytes());
        ClientHandler handler = new ClientHandler(mockSocket);
        ByteArrayOutputStream outStream = mockSocket.getOutputStreamSpy();
        handler.run();

        // Kiểm tra sendEvent
        Event event1 = new Event(EventType.AUCTION_CREATED, "Phiên đấu giá bắt đầu");
        handler.sendEvent(event1);
        String output1 = outStream.toString().trim();
        assertTrue(output1.contains("AUCTION_CREATED"));
        assertTrue(output1.contains("Phiên đấu giá bắt đầu"));

        // Xóa sạch dữ liệu trong stream để test onEvent
        outStream.reset();

        // Kiểm tra onEvent (kế thừa từ Observer)
        Event event2 = new Event(EventType.BID_PLACED, "Mức giá mới được đặt");
        handler.onEvent(event2);
        String output2 = outStream.toString().trim();
        assertTrue(output2.contains("BID_PLACED"));
        assertTrue(output2.contains("Mức giá mới được đặt"));
    }

    @Test
    public void testRunLoopReadsAndDispatchesSuccessfully() throws IOException {
        // Đăng ký một route test
        RequestType testType = RequestType.LOGIN;
        dispatcher.register(testType, (client, req) -> {
            // Giả lập lưu session
            client.setCurrentUsername("john_doe");
            return new Response(RequestStatus.SUCCESS, "Login success for john");
        });

        // Tạo dữ liệu request JSON kết thúc bằng dấu xuống dòng \n
        Request req = new Request(testType, null);
        String reqId = req.getId();
        String reqJson = JsonUtil.toJson(req) + "\n";

        MockSSLSocket mockSocket = new MockSSLSocket(reqJson.getBytes());
        ClientHandler handler = new ClientHandler(mockSocket);
        
        // Chạy client handler (sẽ đọc dòng tin, dispatch, và ghi phản hồi)
        handler.run();

        // Đọc phản hồi từ socket output
        String responseJson = mockSocket.getOutputStreamSpy().toString().trim();
        
        // Trích dòng phản hồi đầu tiên (do khi kết thúc loop, handleDisconnect cũng có thể ghi LOGOUT response nếu có đăng ký route LOGOUT)
        String firstLine = responseJson.split("\n")[0].trim();
        
        Response res = JsonUtil.fromJson(firstLine, Response.class);
        assertNotNull(res, "Phản hồi nhận được phải khác null");
        assertEquals(reqId, res.getId(), "Request ID phải được bảo toàn trong Response");
        assertEquals(RequestStatus.SUCCESS, res.getStatus());
        assertEquals("Login success for john", res.getMessage());

        // Kiểm tra session được giữ sau khi login
        assertEquals("john_doe", handler.getCurrentUsername());
    }

    @Test
    public void testDisconnectTriggersLogoutRequest() throws IOException {
        final List<Request> receivedRequests = new ArrayList<>();
        // Đăng ký route LOGOUT để hứng request khi ngắt kết nối
        dispatcher.register(RequestType.LOGOUT, (client, req) -> {
            receivedRequests.add(req);
            return new Response(RequestStatus.SUCCESS, "Logged out");
        });

        // Socket trống
        MockSSLSocket mockSocket = new MockSSLSocket("".getBytes());
        ClientHandler handler = new ClientHandler(mockSocket);
        
        // Gán username để giả lập client đã login trước khi ngắt kết nối
        handler.setCurrentUsername("active_user");

        // Gọi run() -> loop đọc dòng kết thúc ngay vì stream trống -> nhảy vào finally: handleDisconnect()
        handler.run();

        // Xác minh handler đã tự động gửi RequestType.LOGOUT
        assertEquals(1, receivedRequests.size(), "Hệ thống phải dispatch đúng 1 request khi ngắt kết nối");
        Request logoutReq = receivedRequests.get(0);
        assertEquals(RequestType.LOGOUT, logoutReq.getType(), "Loại request phải là LOGOUT");
        assertNotNull(logoutReq.getData(), "Dữ liệu request không được null");
        
        // Đọc logout data
        LogoutRequest data = JsonUtil.fromMap(logoutReq.getData(), LogoutRequest.class);
        assertNotNull(data, "Dữ liệu LogoutRequest phải parse thành công");
    }

    @Test
    public void testCloseConnectionClosesSocket() {
        MockSSLSocket mockSocket = new MockSSLSocket("".getBytes());
        ClientHandler handler = new ClientHandler(mockSocket);

        assertFalse(mockSocket.isClosed(), "Socket phải đang mở ban đầu");
        
        handler.closeConnection();
        
        assertTrue(mockSocket.isClosed(), "Socket phải đóng sau khi gọi closeConnection()");
    }

    /**
     * Lớp giả lập SSLSocket để phục vụ kiểm thử đơn vị.
     */
    private static class MockSSLSocket extends SSLSocket {
        private final ByteArrayInputStream inputStream;
        private final ByteArrayOutputStream outputStream;
        private boolean closed = false;

        MockSSLSocket(byte[] inputBytes) {
            this.inputStream = new ByteArrayInputStream(inputBytes);
            this.outputStream = new ByteArrayOutputStream();
        }

        ByteArrayOutputStream getOutputStreamSpy() {
            return outputStream;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public InetAddress getInetAddress() {
            try {
                return InetAddress.getByName("127.0.0.1");
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            closed = true;
        }

        // --- Các phương thức abstract bắt buộc phải implement của SSLSocket ---
        @Override public String[] getSupportedCipherSuites() { return new String[0]; }
        @Override public String[] getEnabledCipherSuites() { return new String[0]; }
        @Override public void setEnabledCipherSuites(String[] suites) {}
        @Override public String[] getSupportedProtocols() { return new String[0]; }
        @Override public String[] getEnabledProtocols() { return new String[0]; }
        @Override public void setEnabledProtocols(String[] protocols) {}
        @Override public SSLSession getSession() { return null; }
        @Override public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {}
        @Override public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {}
        @Override public void startHandshake() {}
        @Override public void setUseClientMode(boolean mode) {}
        @Override public boolean getUseClientMode() { return false; }
        @Override public void setNeedClientAuth(boolean need) {}
        @Override public boolean getNeedClientAuth() { return false; }
        @Override public void setWantClientAuth(boolean want) {}
        @Override public boolean getWantClientAuth() { return false; }
        @Override public void setEnableSessionCreation(boolean flag) {}
        @Override public boolean getEnableSessionCreation() { return false; }
    }
}
