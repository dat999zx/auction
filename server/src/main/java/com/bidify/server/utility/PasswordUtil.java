package com.bidify.server.utility;
// mật khẩu người dùng nếu lưu đúng định dạng -> yếu và dễ bị đánh cắp
// -> cần hash mật khẩu bằng thuật toán SHA-256 (chỉ dùng được với byte -> convert String thành Bytes)

import java.nio.charset.StandardCharsets; 
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {
    // dùng để tạo một đối tượng PasswordUtil
    private PasswordUtil(){}

    // dùng để băm
    public static String hash(String iniPassword){
        if (iniPassword == null) throw new IllegalArgumentException("Password cannot be null");
        try {
            MessageDigest mess = MessageDigest.getInstance("SHA-256");
            byte[] hashPass = mess.digest(iniPassword.getBytes(StandardCharsets.UTF_8));

            StringBuilder newPass = new StringBuilder();
            for (byte b : hashPass){
                newPass.append(String.format("%02x", b)); // %x chuyển sang hex với 02 ký tự
            }

            return newPass.toString();
        }
        catch (NoSuchAlgorithmException ecp) {
            // phòng trường hợp ko có thuật toán SHA-256, cho vào cho an toàn=))
            throw new RuntimeException("Can not hash by using algorithm", ecp);
        }
    }

    // dùng để so khớp
    public static boolean matches(String rawPass, String hashedPass){
        if (rawPass == null || hashedPass == null) return false;
        return hash(rawPass).equals(hashedPass);
    }
}
