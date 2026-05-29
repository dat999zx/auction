package com.bidify.server.model.runtime;

import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn giản cho ClientSession.
 */
public class ClientSessionTest {

    @Test
    public void testClientSessionInitialization() {
        // Khởi tạo các đối tượng giả lập
        ClientHandler clientHandler = new ClientHandler(null);
        User user = new User("test_username", "Test Nick", "password");

        // Khởi tạo ClientSession
        ClientSession session = new ClientSession(clientHandler, user);

        // Xác thực
        assertNotNull(session, "ClientSession không được null sau khi khởi tạo");
        assertSame(clientHandler, session.getClientHandler(), "ClientHandler trả về phải khớp với đối tượng truyền vào");
        assertSame(user, session.getUser(), "User trả về phải khớp với đối tượng truyền vào");
    }
}
