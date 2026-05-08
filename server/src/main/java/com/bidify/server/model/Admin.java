package com.bidify.server.model;

import com.bidify.common.model.Event;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.model.runtime.GlobalChannel;
import com.bidify.server.contract.*;
import com.bidify.server.network.ClientHandler;

import java.time.LocalDateTime;

import com.bidify.common.enums.UserStatus;

public class Admin extends User {
    public Admin(String username, String nickname, String password) {
        super(username, nickname, password);
    }

    public Admin(String username, String nickname, String password, String email, String phone, UserStatus status, LocalDateTime createdAt, LocalDateTime lastLogin, double wallet) {
        super(username, nickname, password, email, phone, status, createdAt, lastLogin, wallet);
    }

    public void banUser(String username){ // ban user
        // TODO: ban user
    }

    public void unbanUser(String username){ // unban user
        // TODO: unban user
    }

    public void closeAuction(String auctionId){ // đóng auction
        // TODO: close auction
    }

    public void deleteAuction(String auctionId){ // xóa auction
        // TODO: delete auction
    }

    public void sendEvent(Event event){ // gửi event đến tất cả user
        GlobalChannel globalChannel = RealtimeDatabase.getGlobalChannel();
        if (globalChannel != null) globalChannel.publish(event);
    }

    public void sendEvent(String username, Event event){ // gửi event đến user cụ thể
        ClientHandler client = RealtimeDatabase.getUserClient(username);
        if (client != null) client.sendEvent(event);
    }
}
