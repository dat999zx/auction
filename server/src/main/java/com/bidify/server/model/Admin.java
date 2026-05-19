package com.bidify.server.model;

import com.bidify.common.model.Event;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.model.runtime.GlobalChannel;
import com.bidify.server.network.ClientHandler;

import java.time.LocalDateTime;

import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;

public class Admin extends User {
    // dùng để tạo một đối tượng Admin
    public Admin(String username, String nickname, String password) {
        // dùng để super
        super(username, nickname, password);
        // dùng để thiết lập vai trò
        setRole(UserRole.ADMIN);
    }

    // dùng để tạo một đối tượng Admin
    public Admin(String username, String nickname, String password, String email, String phone, UserStatus status, LocalDateTime createdAt, LocalDateTime lastLogin, double wallet) {
        // dùng để super
        super(username, nickname, password, email, phone, status, UserRole.ADMIN, createdAt, lastLogin, wallet);
    }

    // dùng để cấm người dùng
    public void banUser(String username){ // ban user
        // TODO: ban user
    }

    // dùng để gỡ cấm người dùng
    public void unbanUser(String username){ // unban user
        // TODO: unban user
    }

    // dùng để đóng đấu giá
    public void closeAuction(String auctionId){ // đóng auction
        // TODO: close auction
    }

    // dùng để xóa đấu giá
    public void deleteAuction(String auctionId){ // xóa auction
        // TODO: delete auction
    }

    // dùng để gửi sự kiện
    public void sendEvent(Event event){ // gửi event đến tất cả user
        GlobalChannel globalChannel = RealtimeDatabase.getGlobalChannel();
        if (globalChannel != null) globalChannel.publish(event);
    }

    // dùng để gửi sự kiện
    public void sendEvent(String username, Event event){ // gửi event đến user cụ thể
        ClientHandler client = RealtimeDatabase.getUserClient(username);
        if (client != null) client.sendEvent(event);
    }
}
