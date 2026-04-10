package com.bidify.model;

public final class ClientSession {
    private static final ClientSession instance = new ClientSession();

    private String currentUsername;

    private ClientSession() {}

    public static ClientSession getInstance() {
        return instance;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public void clear() {
        currentUsername = null;
    }
}
