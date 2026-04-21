package com.bidify.server.dao;

import java.time.LocalDateTime;

import com.bidify.common.enums.UserStatus;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.contract.ImplementUserDao;
import com.bidify.server.model.User;

// giao tiếp với SQLite database về bảng Users
public class UserDao implements ImplementUserDao {
    public boolean existsByUsername(String username) throws DatabaseException { // xét tồn tại username trong database
        Boolean exists = SQLiteHelper.query(
            "SELECT username FROM Users WHERE username = ?",
            rs -> rs != null && rs.next(),
            username
        );
        return exists != null && exists;
    }

    public User findByUsername(String username) throws DatabaseException { // lấy User từ database bằng username
        return SQLiteHelper.query(
            "SELECT * FROM Users WHERE username = ?",
            rs -> {
                if (rs != null && rs.next()){
                    String createdAt = rs.getString("createdAt");
                    String lastLogin = rs.getString("lastLogin");
                    User user = new User(
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("phoneNumber"),
                        UserStatus.valueOf(rs.getString("status")),
                        createdAt == null || createdAt.isBlank() ? null : LocalDateTime.parse(createdAt),
                        lastLogin == null || lastLogin.isBlank() ? null : LocalDateTime.parse(lastLogin),
                        rs.getDouble("wallet")
                    );
                    return user;
                }
                return null;
            },
            username
        );
    }

    public void create(User user) throws DatabaseException { // đăng kí
        SQLiteHelper.update(
            "INSERT INTO Users(username, nickname, password, createdAt, lastLogin, wallet) VALUES (?, ?, ?, ?, ?, ?)",
            user.getUsername(),
            user.getNickname(),
            user.getPassword(),
            user.getCreatedAt() == null ? null : user.getCreatedAt().toString(),
            user.getLastLogin() == null ? null : user.getLastLogin().toString(),
            user.getWallet()
        );
    }

    public void save(User user) throws DatabaseException { // lưu user data mặc định cập nhật last login
        save(user, true);
    }

    public void save(User user, boolean saveLastLogin) throws DatabaseException { // lưu user data
        SQLiteHelper.update(
            """
            UPDATE Users SET 
                nickname = ?, 
                password = ?, 
                email = ?, 
                phoneNumber = ?, 
                status = ?, 
                createdAt = ?, 
                wallet = ? 
            WHERE username = ?
            """,
            user.getNickname(),
            user.getPassword(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getStatus().toString(),
            user.getCreatedAt().toString(),
            user.getWallet(),
            user.getUsername()
        );
        
        if (saveLastLogin){
            SQLiteHelper.update(
                "UPDATE Users SET lastLogin = ? WHERE username = ?",
                LocalDateTime.now().toString(),
                user.getUsername()
            );
        }
        System.out.println("saved user: " + user.getUsername());
    }
}
