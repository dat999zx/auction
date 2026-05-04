package com.bidify.server.model;

import com.bidify.common.enums.TransactionType;
import com.bidify.common.utility.IdGenerator;
import java.time.LocalDateTime;

public class Transaction extends Entity {
    private String username;
    private TransactionType type;
    private double amount;
    private String auctionId;

    // nạp tiền / rút tiền
    public Transaction(String username, TransactionType type, double amount) {
        this(username, type, amount, null);
    }

    // thanh toán / nhận tiền trong auction
    public Transaction(String username, TransactionType type, double amount, String auctionId) {
        super(IdGenerator.genTransactionId(), LocalDateTime.now());
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.auctionId = auctionId;
    }

    // load từ db
    public Transaction(String id, LocalDateTime createdAt, String username, TransactionType type, double amount, String auctionId) {
        super(id, createdAt);
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.auctionId = auctionId;
    }

    public String getUsername() { return username; }
    public TransactionType getType() { return type; }
    public double getAmount() { return amount; }
    public String getAuctionId() { return auctionId; }
}
