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

    // dùng để tạo một đối tượng SQLiteConnection
    private SQLiteConnection(){}

    // dùng để kết nối
    public static Connection connect() throws DatabaseException { // tạo kết nối với data.db
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.setJournalMode(SQLiteConfig.JournalMode.WAL); 
            config.setBusyTimeout(5000);
            // thêm thời gian chờ mở file .db tối đa 5s thay vì crash luôn
            // nếu ko có cái này mà 2 người cùng sửa file cùng 1 lúc thì bị exception "Database is locked"
            // quá 5s thì bị exception "Database is locked" -> khả năng cao là lỗi mở file quên đóng, mở vô hạn
            return DriverManager.getConnection(URL, config.toProperties());
        }
        catch (SQLException e) {
            throw new DatabaseException("Failed to connect to database", e);
        }
    }

    // dùng để giải quyết cơ sở dữ liệu path
    private static String resolveDbPath() { // lấy đường dẫn tới data.db
        String customPath = System.getProperty("db.path");
        // có thể override đường dẫn tới data.db bằng cách set system property "db.path" khi chạy ứng dụng, vd: java -Ddb.path="C:\custom\path\data.db" -jar server.jar
        if (customPath != null) return customPath;

        Path curPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path serverDir = curPath.resolve("server"); // thử tìm folder server

        // nếu như đang dùng IDE, thì đọc data.db trong folder server
        if (Files.exists(serverDir) && Files.isDirectory(serverDir))
            return serverDir.resolve("data.db").toString();

        // còn nếu dùng file jar thì tìm data.db trong folder đang chứa jar đó
        return curPath.resolve("data.db").toString();
    }
}
