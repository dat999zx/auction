package com.bidify.server.exception;

public class ServerTimeOutException extends RuntimeException {
    public ServerTimeOutException(String message) { super(message); }
    public ServerTimeOutException() { super("Server timed out"); }
}
