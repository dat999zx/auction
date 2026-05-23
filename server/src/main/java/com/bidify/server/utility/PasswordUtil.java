package com.bidify.server.utility;

import java.nio.charset.StandardCharsets; 
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// PasswordUtil hỗ trợ mã hóa mật khẩu sử dụng thuật toán băm SHA-256.
public class PasswordUtil {
    private PasswordUtil(){}

    // Băm mật khẩu sử dụng thuật toán SHA-256.
    public static String hash(String iniPassword){
        if (iniPassword == null) throw new IllegalArgumentException("Password cannot be null");
        try {
            MessageDigest mess = MessageDigest.getInstance("SHA-256");
            byte[] hashPass = mess.digest(iniPassword.getBytes(StandardCharsets.UTF_8));

            StringBuilder newPass = new StringBuilder();
            for (byte b : hashPass){
                newPass.append(String.format("%02x", b));
            }

            return newPass.toString();
        }
        catch (NoSuchAlgorithmException ecp) {
            throw new RuntimeException("Can not hash by using algorithm", ecp);
        }
    }

    // So sánh mật khẩu thô với mật khẩu đã băm.
    public static boolean matches(String rawPass, String hashedPass){
        if (rawPass == null || hashedPass == null) return false;
        return hash(rawPass).equals(hashedPass);
    }
}
