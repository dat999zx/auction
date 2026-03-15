package com.bidify.common.exception;
/*
xử lý các lỗi về định dạng của các attributes
*/
public class ValidationException extends RuntimeException {
    public ValidationException(String message){ super(message); }
}
