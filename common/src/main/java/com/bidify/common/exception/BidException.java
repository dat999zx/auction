package com.bidify.common.exception;

public class BidException extends RuntimeException {
    // dùng để tạo một đối tượng BidException
    public BidException(String message) {
        // dùng để super
        super(message);
    }
}
