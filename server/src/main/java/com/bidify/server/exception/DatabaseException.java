package com.bidify.server.exception;

// Ngoại lệ xảy ra khi có lỗi thao tác với cơ sở dữ liệu SQLite.
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message){ super(message); }
    public DatabaseException(String message, Throwable cause){ super(message, cause); }
}
