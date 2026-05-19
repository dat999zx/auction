package com.bidify.common.exception;

public class UnauthorizedException extends RuntimeException {
    // dùng để tạo một đối tượng UnauthorizedException
    public UnauthorizedException(String message) { super(message); }
    // dùng để tạo một đối tượng UnauthorizedException
    public UnauthorizedException() { super("Unauthorized access"); }
}
