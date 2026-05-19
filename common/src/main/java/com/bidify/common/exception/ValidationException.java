package com.bidify.common.exception;
/*
xử lý các lỗi về định dạng của các attributes
*/
public class ValidationException extends RuntimeException {
    // dùng để tạo một đối tượng ValidationException
    public ValidationException(String message){ super(message); }
}
