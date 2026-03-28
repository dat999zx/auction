package com.bidify.server.contract;

public interface CanManageUser {
    void banUser(String username);
    void unbanUser(String username);
}
