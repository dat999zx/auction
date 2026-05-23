package com.bidify.common.exception;
/*
Xử lý nếu như login/ pass sai; user đã bị banned
*/
public class AuthException extends RuntimeException{
    public AuthException(String message){ super(message); }
}
