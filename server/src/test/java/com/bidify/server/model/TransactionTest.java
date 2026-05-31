package com.bidify.server.model;

import com.bidify.common.enums.TransactionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn giản cho Transaction.
 */
public class TransactionTest {

    @Test
    public void testTransactionWalletOperationConstructor() {
        String username = "user_tx_1";
        double amount = 500.0;

        // Giao dịch nạp/rút thông thường (không liên kết đấu giá)
        Transaction tx = new Transaction(username, TransactionType.DEPOSIT, amount);

        // Xác thực
        assertNotNull(tx.getId());
        assertNotNull(tx.getCreatedAt());
        assertEquals(username, tx.getUsername());
        assertEquals(TransactionType.DEPOSIT, tx.getType());
        assertEquals(amount, tx.getAmount());
        assertNull(tx.getAuctionId(), "Auction ID phải là null đối với giao dịch nạp/rút thông thường");
    }

    @Test
    public void testTransactionAuctionOperationConstructor() {
        String username = "user_tx_2";
        double amount = 1200.0;
        String auctionId = "auc-tx-123";

        // Giao dịch thanh toán đấu giá
        Transaction tx = new Transaction(username, TransactionType.AUCTION_PAY, amount, auctionId);

        // Xác thực
        assertEquals(username, tx.getUsername());
        assertEquals(TransactionType.AUCTION_PAY, tx.getType());
        assertEquals(amount, tx.getAmount());
        assertEquals(auctionId, tx.getAuctionId());
    }

    @Test
    public void testTransactionDatabaseConstructor() {
        String id = "tx-db-000";
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 29, 2, 0, 0);
        String username = "user_tx_db";
        double amount = 800.0;
        String auctionId = "auc-tx-db";

        // Giao dịch nạp từ database
        Transaction tx = new Transaction(id, createdAt, username, TransactionType.AUCTION_PROFIT, amount, auctionId);

        // Xác thực bảo toàn
        assertEquals(id, tx.getId());
        assertEquals(createdAt, tx.getCreatedAt());
        assertEquals(username, tx.getUsername());
        assertEquals(TransactionType.AUCTION_PROFIT, tx.getType());
        assertEquals(amount, tx.getAmount());
        assertEquals(auctionId, tx.getAuctionId());
    }
}
