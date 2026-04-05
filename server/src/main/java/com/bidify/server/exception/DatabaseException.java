package com.bidify.server.exception;

// các lỗi liên quan đến database (không truy cập được, không đọc được...)
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message){ super(message); }
    public DatabaseException(String message, Throwable cause){ super(message, cause); }
}
