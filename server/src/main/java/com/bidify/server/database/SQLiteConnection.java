package com.bidify.server.database;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

public class SQLiteConnection {
    private static final String URL = "jdbc:sqlite:" + resolveDbPath();

    private SQLiteConnection(){}

    public static Connection connect() { // tạo kết nối với data.db
        try {
            return DriverManager.getConnection(URL);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String resolveDbPath() { // lấy đường dẫn tới data.db
        Path curPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

        if (curPath.getFileName() != null && "server".equalsIgnoreCase(curPath.getFileName().toString()))
            return curPath.resolve("data.db").toString();
        return curPath.resolve("server").resolve("data.db").toString();
    }
}
