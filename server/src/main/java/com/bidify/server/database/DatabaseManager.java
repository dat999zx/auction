package com.bidify.server.database;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseManager {
    // tạo bảng nếu chưa tồn tại
    public static void init() { // chỉ chạy 1 lần khi server start
        try (Connection connection = SQLiteConnection.connect()) {
            if (connection == null)
                throw new IllegalStateException("Cannot connect to database");

            try (Statement statement = connection.createStatement()) {
            InputStream in = DatabaseManager.class.getResourceAsStream("/schema.sql");
            if (in == null)
                throw new IllegalStateException("Missing resource: /schema.sql");

            String sqlScript = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String sql : sqlScript.split(";")) {
                String trimmed = sql.trim();
                if (trimmed.isEmpty()) continue;
                statement.execute(trimmed);
            }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // INSERT / UPDATE / DELETE
    public static boolean update(String sql, Object... params) {
        try (
            Connection connection = SQLiteConnection.connect();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            setParams(statement, params);
            return statement.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // SELECT
    public static <T> T query(String sql, ResultHandler<T> handler, Object... params) {
        try (
            Connection connection = SQLiteConnection.connect();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            setParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                return handler.handle(rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void setParams(PreparedStatement statement, Object... params) throws Exception {
        for (int i = 0; i < params.length; i++) {
            statement.setObject(i + 1, params[i]);
        }
    }
}
