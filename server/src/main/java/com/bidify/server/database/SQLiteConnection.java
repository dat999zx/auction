package com.bidify.server.database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.bidify.server.exception.DatabaseException;

public class SQLiteConnection {
    private static final String URL = "jdbc:sqlite:" + resolveDbPath();

    private SQLiteConnection(){}

    public static Connection connect() throws DatabaseException { // tạo kết nối với data.db
        try {
            return DriverManager.getConnection(URL);
        }
        catch (SQLException e) {
            throw new DatabaseException("Failed to connect to database", e);
        }
    }

    private static String resolveDbPath() { // lấy đường dẫn tới data.db
        Path curPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

        if (curPath.getFileName() != null && "server".equalsIgnoreCase(curPath.getFileName().toString()))
            return curPath.resolve("data.db").toString();
        return curPath.resolve("server").resolve("data.db").toString();
    }
}
