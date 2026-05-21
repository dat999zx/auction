package com.bidify.server.database;

import java.sql.ResultSet;
import java.sql.SQLException;

// đổi các hàng từ ResultSet thành object
@FunctionalInterface
public interface ResultHandler<T> {
    // dùng để xử lý
    T handle(ResultSet rs) throws SQLException;
}
