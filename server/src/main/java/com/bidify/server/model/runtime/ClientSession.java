package com.bidify.server.model.runtime;

import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

public class ClientSession {
    private final ClientHandler clientHandler;
    private final User user;

    // dùng để tạo một đối tượng ClientSession
    public ClientSession(ClientHandler clientHandler, User user) {
        this.clientHandler = clientHandler;
        this.user = user;
    }

    public ClientHandler getClientHandler() { return clientHandler; }
    public User getUser() { return user; }
}
