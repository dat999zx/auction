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
    // dùng để tạo một đối tượng Transaction
    public Transaction(String username, TransactionType type, double amount) {
        // dùng để this
        this(username, type, amount, null);
    }

    // thanh toán / nhận tiền trong auction
    // dùng để tạo một đối tượng Transaction
    public Transaction(String username, TransactionType type, double amount, String auctionId) {
        super(IdGenerator.genTransactionId(), LocalDateTime.now());
        this.username = username;
        this.type = type;
        this.amount = amount;
        this.auctionId = auctionId;
    }

    // load từ db
    // dùng để tạo một đối tượng Transaction
    public Transaction(String id, LocalDateTime createdAt, String username, TransactionType type, double amount, String auctionId) {
        // dùng để super
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
