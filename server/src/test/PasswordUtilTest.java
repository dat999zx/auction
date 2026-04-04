package com.bidify.server.utility;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {
    @Test
    void testHashPassword(){
        String password = "testing";
        String hashedPassword = PasswordUtil.hashPassword(password);
        assert(PasswordUtil.matches(password, hashedPassword));
    }
}
