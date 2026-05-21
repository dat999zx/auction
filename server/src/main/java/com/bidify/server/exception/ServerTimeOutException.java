package com.bidify.server.exception;

public class ServerTimeOutException extends RuntimeException {
    // dùng để tạo một đối tượng ServerTimeOutException
    public ServerTimeOutException(String message) { super(message); }
    // dùng để tạo một đối tượng ServerTimeOutException
    public ServerTimeOutException() { super("Server timed out"); }
}
