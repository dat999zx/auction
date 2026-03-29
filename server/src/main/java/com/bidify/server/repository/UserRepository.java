package com.bidify.server.repository;

import java.time.LocalDateTime;

import com.bidify.common.enums.UserStatus;
import com.bidify.server.database.DatabaseManager;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

// giao tiếp với database về phần người dùng
public class UserRepository {
    public boolean existsByUsername(String username){ // xét tồn tại username trong database
        Boolean exists = DatabaseManager.query(
            "SELECT username FROM Users WHERE username = ?",
            rs -> rs != null && rs.next(),
            username
        );
        return exists != null && exists;
    }

    public User findByUsername(String username){ // lấy User từ database bằng username
        return DatabaseManager.query(
            "SELECT * FROM Users WHERE username = ?",
            rs -> {
                if (rs != null && rs.next()){
                    User user = new User(
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("phoneNumber"),
                        UserStatus.valueOf(rs.getString("status")),
                        rs.getString("createdAt"),
                        rs.getString("lastLogin"),
                        rs.getDouble("wallet")
                    );
                    return user;
                }
                return null;
            },
            username
        );
    }

    public boolean register(User user){ // đăng kí
        return DatabaseManager.update(
            "INSERT INTO Users(username, nickname, password) VALUES (?, ?, ?)",
            user.getUsername(), user.getNickname(), user.getPassword());
    }

    public void saveClient(ClientHandler client){
        saveClient(client, true);
    }

    public void saveClient(ClientHandler client, boolean saveLastLogin) { // lưu client data
        if (client == null || client.getCurrentUsername() == null) return;
        // TODO: save client
        if (saveLastLogin){
            DatabaseManager.update(
                "UPDATE Users SET lastLogin = ? WHERE username = ?",
                LocalDateTime.now().toString(),
                client.getCurrentUsername()
            );
        }
        System.out.println("saved client: " + client.getCurrentUsername());
    }

    public void saveAllClients(){
        saveAllClients(true);
    }

    public void saveAllClients(boolean saveLastLogin){ // lưu tất cả client data
        for (ClientHandler client : RealtimeDatabase.getAllActiveClients())
            saveClient(client, saveLastLogin);
    }
}
