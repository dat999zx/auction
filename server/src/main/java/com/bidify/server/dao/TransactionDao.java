package com.bidify.server.dao;

import com.bidify.common.enums.TransactionType;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDao {
    private static TransactionDao instance = new TransactionDao();

    private TransactionDao() {}

    public static TransactionDao getInstance() { return instance; }

    public void create(Transaction transaction) throws DatabaseException {
        String sql = "INSERT INTO Transactions(id, createdAt, username, type, amount, auctionId) VALUES (?, ?, ?, ?, ?, ?)";
        SQLiteHelper.update(sql,
                transaction.getId(),
                transaction.getCreatedAt().toString(),
                transaction.getUsername(),
                transaction.getType().toString(),
                transaction.getAmount(),
                transaction.getAuctionId()
        );
    }

    public List<Transaction> findByUsername(String username) throws DatabaseException {
        String sql = "SELECT * FROM Transactions WHERE username = ? ORDER BY createdAt DESC";
        return SQLiteHelper.query(sql, rs -> {
            List<Transaction> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Transaction(
                        rs.getString("id"),
                        LocalDateTime.parse(rs.getString("createdAt")),
                        rs.getString("username"),
                        TransactionType.valueOf(rs.getString("type")),
                        rs.getDouble("amount"),
                        rs.getString("auctionId")
                ));
            }
            return list;
        }, username);
    }

    public void deleteByUsername(String username) throws DatabaseException {
        SQLiteHelper.update("DELETE FROM Transactions WHERE username = ?", username);
    }

    public void deleteByAuctionId(String auctionId) throws DatabaseException {
        SQLiteHelper.update("DELETE FROM Transactions WHERE auctionId = ?", auctionId);
    }
}
