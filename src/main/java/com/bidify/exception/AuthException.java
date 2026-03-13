package com.bidify.exception;
// lỗi xác thực, tài khoản / mật khẩu sai
public class AuthException extends RuntimeException {
    public AuthException(String message){ super(message); }
}
