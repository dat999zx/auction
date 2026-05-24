package com.bidify.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.bidify.common.dto.UserDto;
import com.bidify.common.dto.WalletDto;
import com.bidify.common.enums.UserRole;

class ClientSessionTest {
    private final ClientSession session = ClientSession.getInstance();

    @AfterEach
    void tearDown() {
        session.clear();
    }

    @Test
    void setCurrentUserAlsoSetsCurrentUsername() {
        UserDto user = new UserDto("alice", "Alice", new WalletDto(0, 0), UserRole.USER);

        session.setCurrentUser(user);

        assertEquals("alice", session.getCurrentUsername());
        assertEquals(user, session.getCurrentUser());
        assertTrue(session.isLoggedIn());
        assertFalse(session.isAdmin());
    }

    @Test
    void detectsAdminFromCachedUser() {
        UserDto admin = new UserDto("admin", "Admin", new WalletDto(0, 0), UserRole.ADMIN);

        session.setCurrentUser(admin);

        assertTrue(session.isAdmin());
    }

    @Test
    void clearRemovesUserAndUsername() {
        session.setCurrentUsername("alice");

        session.clear();

        assertNull(session.getCurrentUsername());
        assertNull(session.getCurrentUser());
        assertFalse(session.isLoggedIn());
        assertFalse(session.isAdmin());
    }
}
