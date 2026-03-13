package com.bidify.exception;
// lỗi đăng kí: username trống, password ngắn hoặc username đã tồn tại
public class ValidationException extends RuntimeException {
    public ValidationException(String message) { super(message); }
}
