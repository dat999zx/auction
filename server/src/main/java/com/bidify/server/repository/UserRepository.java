package com.bidify.server.repository;

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
                        rs.getString("nickname"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("email")
                    );
                    return user;
                }
                return null;
            },
            username
        );
    }

    public boolean save(User user){ // lưu user vào database
        return (!DatabaseManager.update(
            "INSERT INTO Users(username, nickname, password) VALUES (?, ?, ?)",
            user.getUsername(), user.getNickname(), user.getPassword()));
    }

    // cập nhật lần login gần nhất
    public boolean updateLastLogin(String username, String lastLogin){
        return DatabaseManager.update(
            "UPDATE Users SET lastLogin = ? WHERE username = ?",
            lastLogin,
            username
        );
    }
}
