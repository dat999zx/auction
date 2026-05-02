package com.bidify.server.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
    
    public InsufficientBalanceException() {
        super("Insufficient balance to perform this action");
    }

    public InsufficientBalanceException(String message, double available, double required) {
        super(message + String.format(". Missing %.2f$", required - available));
    }
}
