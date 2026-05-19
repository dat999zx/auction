package com.bidify.server.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.User;

public class UserDao  {
    private static UserDao instance = new UserDao();
    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    // dùng để tạo một đối tượng UserDao
    private UserDao() {}

    // dùng để lấy đối tượng Singleton
    public static UserDao getInstance() { return instance; }

    // dùng để exists bởi username
    public boolean existsByUsername(String username) throws DatabaseException { // xÃ©t tá»“n táº¡i username trong database
        Boolean exists = SQLiteHelper.query(
            "SELECT username FROM Users WHERE username = ?",
            rs -> rs != null && rs.next(),
            username
        );
        return exists != null && exists;
    }

    // dùng để tìm kiếm bởi username
    public User findByUsername(String username) throws DatabaseException { // láº¥y User tá»« database báº±ng username
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
                        rs.getString("role") == null ? UserRole.USER : UserRole.valueOf(rs.getString("role")),
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

    // dùng để tìm kiếm all
    public List<User> findAll() throws DatabaseException {
        return SQLiteHelper.query(
            "SELECT * FROM Users ORDER BY createdAt DESC",
            rs -> {
                List<User> users = new ArrayList<>();
                while (rs != null && rs.next()){
                    String createdAt = rs.getString("createdAt");
                    String lastLogin = rs.getString("lastLogin");
                    users.add(new User(
                        rs.getString("username"),
                        rs.getString("nickname"),
                        rs.getString("password"),
                        rs.getString("email"),
                        rs.getString("phoneNumber"),
                        UserStatus.valueOf(rs.getString("status")),
                        rs.getString("role") == null ? UserRole.USER : UserRole.valueOf(rs.getString("role")),
                        createdAt == null || createdAt.isBlank() ? null : LocalDateTime.parse(createdAt),
                        lastLogin == null || lastLogin.isBlank() ? null : LocalDateTime.parse(lastLogin),
                        rs.getDouble("balance")
                    ));
                }
                return users;
            }
        );
    }

    // dùng để tạo
    public void create(User user) throws DatabaseException { // Ä‘Äƒng kÃ­
        SQLiteHelper.update(
            "INSERT INTO Users(username, nickname, password, role, createdAt, lastLogin, balance) VALUES (?, ?, ?, ?, ?, ?, ?)",
            user.getUsername(),
            user.getNickname(),
            user.getPassword(),
            user.getRole().toString(),
            user.getCreatedAt() == null ? null : user.getCreatedAt().toString(),
            user.getLastLogin() == null ? null : user.getLastLogin().toString(),
            user.getWallet().getBalance()
        );
    }

    // dùng để lưu
    public void save(User user) throws DatabaseException { // lÆ°u user data máº·c Ä‘á»‹nh cáº­p nháº­t last login
        // dùng để lưu
        save(user, true);
    }

    // dùng để lưu
    public void save(User user, boolean saveLastLogin) throws DatabaseException { // lÆ°u user data
        SQLiteHelper.update(
            """
            UPDATE Users SET 
                nickname = ?, 
                password = ?, 
                email = ?, 
                phoneNumber = ?, 
                status = ?, 
                role = ?,
                createdAt = ?, 
                balance = ? 
            WHERE username = ?
            """,
            user.getNickname(),
            user.getPassword(),
            user.getEmail(),
            user.getPhoneNumber(),
            user.getStatus().toString(),
            user.getRole().toString(),
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

    // dùng để xóa bởi username
    public void deleteByUsername(String username) throws DatabaseException {
        SQLiteHelper.update("DELETE FROM Users WHERE username = ?", username);
    }
}
