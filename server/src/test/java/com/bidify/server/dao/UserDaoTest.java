package com.bidify.server.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.enums.UserRole;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Admin;
import com.bidify.server.model.User;
import com.bidify.server.utility.PasswordUtil;

class UserDaoTest {
    private final UserDao userDao = UserDao.getInstance();
    private final List<String> createdUsernames = new ArrayList<>();

    @BeforeAll
    static void initDatabase() {
        SQLiteHelper.init();
    }

    @BeforeEach
    void setUp() {
        createdUsernames.clear();
    }

    @AfterEach
    void tearDown() {
        for (String username : createdUsernames)
            userDao.deleteByUsername(username);
    }

    @Test
    void findByUsernameReturnsAdminSubclassForAdminRole() {
        String username = uniqueUsername("adminpoly");
        User admin = new User(username, "Admin Poly", PasswordUtil.hash("secret123"));
        admin.setRole(UserRole.ADMIN);
        userDao.create(admin);
        createdUsernames.add(username);

        User loaded = userDao.findByUsername(username);

        assertInstanceOf(Admin.class, loaded);
        assertEquals(UserRole.ADMIN, loaded.getRole());
        assertEquals(username, loaded.getUsername());
    }

    @Test
    void findByUsernameReturnsUserForUserRole() {
        String username = uniqueUsername("userpoly");
        User user = new User(username, "User Poly", PasswordUtil.hash("secret123"));
        userDao.create(user);
        createdUsernames.add(username);

        User loaded = userDao.findByUsername(username);

        assertFalse(loaded instanceof Admin);
        assertEquals(UserRole.USER, loaded.getRole());
        assertEquals(username, loaded.getUsername());
    }

    @Test
    void promotedUserReloadsAsAdminSubclass() {
        String username = uniqueUsername("promopoly");
        User user = new User(username, "Promoted Poly", PasswordUtil.hash("secret123"));
        userDao.create(user);
        createdUsernames.add(username);

        User savedUser = userDao.findByUsername(username);
        savedUser.setRole(UserRole.ADMIN);
        userDao.save(savedUser, false);

        User reloaded = userDao.findByUsername(username);

        assertInstanceOf(Admin.class, reloaded);
        assertEquals(UserRole.ADMIN, reloaded.getRole());
    }

    private String uniqueUsername(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
