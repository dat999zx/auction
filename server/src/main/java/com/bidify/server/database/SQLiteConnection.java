package com.bidify.server.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.sqlite.SQLiteConfig;

import com.bidify.server.exception.DatabaseException;

public class SQLiteConnection {
    private static final String URL = "jdbc:sqlite:" + resolveDbPath();

    private SQLiteConnection(){}

    public static Connection connect() throws DatabaseException {
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.setJournalMode(SQLiteConfig.JournalMode.WAL); 
            config.setBusyTimeout(5000);
            // Thiết lập chế độ WAL và Busy Timeout để tránh lỗi "Database is locked" khi ghi đồng thời.
            return DriverManager.getConnection(URL, config.toProperties());
        }
        catch (SQLException e) {
            throw new DatabaseException("Failed to connect to database", e);
        }
    }

    private static String resolveDbPath() {
        String customPath = System.getProperty("db.path");
        // Cho phép ghi đè đường dẫn data.db qua System property "db.path" khi chạy server.
        if (customPath != null) return customPath;

        Path curPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path serverDir = curPath.resolve("server");

        // Đọc file data.db ở thư mục server nếu chạy trong môi trường IDE, ngược lại đọc ở thư mục hiện hành.
        if (Files.exists(serverDir) && Files.isDirectory(serverDir))
            return serverDir.resolve("data.db").toString();

        return curPath.resolve("data.db").toString();
    }
}
