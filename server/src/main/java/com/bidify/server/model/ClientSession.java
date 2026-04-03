package com.bidify.server.model;

import com.bidify.server.network.ClientHandler;

public class ClientSession {
    private final ClientHandler clientHandler;
    private final User user;

    public ClientSession(ClientHandler clientHandler, User user) {
        this.clientHandler = clientHandler;
        this.user = user;
    }

    public ClientHandler getClientHandler() { return clientHandler; }
    public User getUser() { return user; }
}
