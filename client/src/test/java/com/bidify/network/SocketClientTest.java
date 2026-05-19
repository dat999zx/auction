package com.bidify.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.bidify.common.enums.RequestType;
import com.bidify.common.model.Request;

public class SocketClientTest {
    // dùng để lấy singleton đối tượng Singleton
    @Test
    void getSingletonInstance() { // test singleton pattern
        SocketClient client1 = SocketClient.getClient();
        SocketClient client2 = SocketClient.getClient();

        // dùng để assert not null
        assertNotNull(client1);
        // dùng để assert same
        assertSame(client1, client2);
    }

    // dùng để thiết lập current username
    @Test
    void setCurrentUsername() { // test set username
        SocketClient client = SocketClient.getClient();

        client.setCurrentUsername("test");

        assertEquals("test", client.getCurrentUsername());
    }

    // dùng để gửi yêu cầu
    @Test
    void sendRequest() { // test gửi request
        SocketClient client = SocketClient.getClient();
        Request request = new Request(RequestType.GET_LIVE_AUCTIONS, null);

        assertThrows(IOException.class, () -> client.send(request));
    }
}
