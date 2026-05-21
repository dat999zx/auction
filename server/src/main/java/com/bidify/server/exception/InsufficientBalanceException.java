package com.bidify.server.exception;

public class InsufficientBalanceException extends RuntimeException {
    // dùng để tạo một đối tượng InsufficientBalanceException
    public InsufficientBalanceException(String message) {
        // dùng để super
        super(message);
    }
    
    // dùng để tạo một đối tượng InsufficientBalanceException
    public InsufficientBalanceException() {
        // dùng để super
        super("Insufficient balance");
    }

    // dùng để tạo một đối tượng InsufficientBalanceException
    public InsufficientBalanceException(String message, double available, double required) {
        super(message + String.format(". Missing %.2f$", required - available));
    }
}
