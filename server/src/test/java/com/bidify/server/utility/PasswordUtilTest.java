package com.bidify.server.utility;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {
    @Test
    void testHashPassword(){ // password hash xong thì ko được giống ban đầu
        String password = "testing";
        String hashedPassword = PasswordUtil.hash(password);
        assertNotEquals(password, hashedPassword);
    }

    @Test
    void testVerifyPassword(){ // test kiểm tra 1 password hashed và chưa hash có giống nhau
        String password = "testing";
        String hashedPassword = PasswordUtil.hash(password);
        assertTrue(PasswordUtil.matches(password, hashedPassword));
    }

    @Test
    void test2HashedPasswordNotEqual(){ // test 2 password hash khác nhau
        String password1 = "testing1";
        String password2 = "testing2";
        String hashedPassword1 = PasswordUtil.hash(password1);
        String hashedPassword2 = PasswordUtil.hash(password2);
        assertNotEquals(hashedPassword1, hashedPassword2);
        assertTrue(PasswordUtil.matches(password1, hashedPassword1));
        assertTrue(PasswordUtil.matches(password2, hashedPassword2));
    }
}
