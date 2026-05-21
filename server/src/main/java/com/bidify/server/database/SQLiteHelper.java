package com.bidify.server.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.bidify.server.exception.DatabaseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLiteHelper {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteHelper.class);
    private static final String SCHEMA_PATH = "/database/schema.sql";

    // dùng để tạo một đối tượng SQLiteHelper
    private SQLiteHelper() {}

    // tạo bảng nếu chưa tồn tại
    // dùng để khởi tạo
    public static void init() throws DatabaseException { // chỉ chạy 1 lần khi server start
        try (
            Connection connection = SQLiteConnection.connect();
            InputStream in = SQLiteHelper.class.getResourceAsStream(SCHEMA_PATH);
        ) {
            if (in == null)
                throw new DatabaseException("Missing resource: " + SCHEMA_PATH);

            String sqlScript = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement statement = connection.createStatement()) {
                for (String sql : sqlScript.split(";")) {
                    String trimmed = sql.trim();
                    if (trimmed.isEmpty()) continue;
                    statement.execute(trimmed);
                }
            }
        }
        catch (SQLException e) {
            throw new DatabaseException("Failed to initialize database", e);
        }
        catch (IOException e) {
            throw new DatabaseException("Failed to read schema file", e);
        }
    }

    // INSERT / UPDATE / DELETE
    // dùng để cập nhật
    public static void update(String sql, Object... params) throws DatabaseException {
        try (
            Connection connection = SQLiteConnection.connect();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            // dùng để thiết lập params
            setParams(statement, params);
            statement.executeUpdate();
        }
        catch (SQLException e) {
            logger.error("Exception occurred", e);
            throw new DatabaseException("Failed to execute update");
        }
    }

    // SELECT
    // dùng để truy vấn
    public static <T> T query(String sql, ResultHandler<T> handler, Object... params) throws DatabaseException {
        try (
            Connection connection = SQLiteConnection.connect();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            // dùng để thiết lập params
            setParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                return handler.handle(rs);
            }
        }
        catch (SQLException e) {
            logger.error("Exception occurred", e);
            throw new DatabaseException("Failed to execute query");
        }
    }

    // dùng để thiết lập params
    private static void setParams(PreparedStatement statement, Object... params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++)
            statement.setObject(i + 1, params[i]);
    }
}
