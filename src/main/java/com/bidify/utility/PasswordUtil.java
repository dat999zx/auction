package com.bidify.utility;
// mật khẩu người dùng nếu lưu đúng định dạng -> yếu và dễ bị đánh cắp
// -> cần hash mật khẩu bằng thuật toán SHA-256 (chỉ dùng được với byte -> convert String thành Bytes)

import java.nio.charset.StandardCharsets; 
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {
    
    public static String hash(String iniPassword){
        try {
            MessageDigest mess = MessageDigest.getInstance("SHA-256");
            byte[] hashPass = mess.digest(iniPassword.getBytes(StandardCharsets.UTF_8));

            StringBuilder newPass = new StringBuilder();
            for (byte b : hashPass){
                newPass.append(String.format("%02x", b)); // %x chuyển sang hex với 02 ký tự
            }

            return newPass.toString();
        }
        // phòng trường hợp ko có thuật toán SHA-256, cho vào cho an toàn=))
        catch (NoSuchAlgorithmException ecp) {
            throw new RuntimeException("Can not hash by using algorithm", ecp);
        }
    }
    public static void main(String[] args) {
        String pass = "12345";
        System.out.println("password after being hashed: " + hash(pass));
    }
}
