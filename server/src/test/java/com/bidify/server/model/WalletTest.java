package com.bidify.server.model;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.bidify.server.exception.InsufficientBalanceException;
import com.bidify.common.exception.ValidationException;

class WalletTest {

    @Test
    void withdrawWithInsufficientAvailableBalanceThrowsException() {
        Wallet wallet = new Wallet(100.0);
        wallet.lockBalance(80.0);
        
        assertThrows(InsufficientBalanceException.class, () -> wallet.withdraw(50.0));
        assertEquals(100.0, wallet.getBalance());
        assertEquals(80.0, wallet.getLockedBalance());
    }

    @Test
    void withdrawSuccessfullyDecreasesBalance() {
        Wallet wallet = new Wallet(100.0);
        wallet.withdraw(50.0);
        assertEquals(50.0, wallet.getBalance());
    }

    @Test
    void withdrawWithNegativeAmountThrowsValidationException() {
        Wallet wallet = new Wallet(100.0);
        assertThrows(ValidationException.class, () -> wallet.withdraw(-10.0));
    }
}
