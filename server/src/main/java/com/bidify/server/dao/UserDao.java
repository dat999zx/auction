package com.bidify.server.dao;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.enums.UserStatus;
import com.bidify.server.contract.ImplementUserDao;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.User;

public class UserDao implements ImplementUserDao {
    private static UserDao instance = new UserDao();
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    private UserDao() {}

    public static UserDao getInstance() { return instance; }

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
                    return new User(
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("phoneNumber"),
                        UserStatus.valueOf(rs.getString("status")),
                        createdAt == null || createdAt.isBlank() ? null : LocalDateTime.parse(createdAt),
                        lastLogin == null || lastLogin.isBlank() ? null : LocalDateTime.parse(lastLogin),
                        rs.getDouble("balance")
                    );
                }
                return null;
            },
            username
        );
    }

    public void create(User user) throws DatabaseException { // đăng kí
        SQLiteHelper.update(
            "INSERT INTO Users(username, nickname, password, createdAt, lastLogin, balance) VALUES (?, ?, ?, ?, ?, ?)",
            user.getUsername(),
            user.getNickname(),
            user.getPassword(),
            user.getCreatedAt() == null ? null : user.getCreatedAt().toString(),
            user.getLastLogin() == null ? null : user.getLastLogin().toString(),
            user.getWallet().getBalance()
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
                balance = ? 
            WHERE username = ?
            """,
            user.getNickname(),
            user.getPassword(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getStatus().toString(),
            user.getCreatedAt().toString(),
            user.getWallet().getBalance(),
            user.getUsername()
        );
        
        if (saveLastLogin){
            SQLiteHelper.update(
                "UPDATE Users SET lastLogin = ? WHERE username = ?",
                LocalDateTime.now().toString(),
                user.getUsername()
            );
        }
        logger.debug("saved user: {}", user.getUsername());
    }
}
