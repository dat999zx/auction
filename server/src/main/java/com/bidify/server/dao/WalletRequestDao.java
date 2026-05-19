package com.bidify.server.dao;

import com.bidify.common.enums.TransactionType;
import com.bidify.common.enums.WalletRequestStatus;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.WalletRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WalletRequestDao {
    private static WalletRequestDao instance = new WalletRequestDao();

    private WalletRequestDao() {}

    public static WalletRequestDao getInstance() { return instance; }

    public void create(WalletRequest request) throws DatabaseException {
        String sql = "INSERT INTO WalletRequests(id, createdAt, reviewedAt, username, type, amount, status, reviewedBy) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        SQLiteHelper.update(sql,
                request.getId(),
                request.getCreatedAt().toString(),
                request.getReviewedAt() != null ? request.getReviewedAt().toString() : null,
                request.getUsername(),
                request.getType().toString(),
                request.getAmount(),
                request.getStatus().toString(),
                request.getReviewedBy()
        );
    }

    public void update(WalletRequest request) throws DatabaseException {
        String sql = "UPDATE WalletRequests SET reviewedAt = ?, status = ?, reviewedBy = ? WHERE id = ?";
        SQLiteHelper.update(sql,
                request.getReviewedAt() != null ? request.getReviewedAt().toString() : null,
                request.getStatus().toString(),
                request.getReviewedBy(),
                request.getId()
        );
    }

    public WalletRequest findById(String id) throws DatabaseException {
        String sql = "SELECT * FROM WalletRequests WHERE id = ?";
        List<WalletRequest> list = SQLiteHelper.query(sql, this::mapResultSet, id);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<WalletRequest> findByUsername(String username) throws DatabaseException {
        String sql = "SELECT * FROM WalletRequests WHERE username = ? ORDER BY createdAt DESC";
        return SQLiteHelper.query(sql, this::mapResultSet, username);
    }

    public List<WalletRequest> findPending() throws DatabaseException {
        String sql = "SELECT * FROM WalletRequests WHERE status = 'PENDING' ORDER BY createdAt ASC";
        return SQLiteHelper.query(sql, this::mapResultSet);
    }

    public double sumPendingWithdrawsForUser(String username) throws DatabaseException {
        String sql = "SELECT SUM(amount) FROM WalletRequests WHERE username = ? AND type = 'WITHDRAW' AND status = 'PENDING'";
        return SQLiteHelper.query(sql, rs -> {
            if (!rs.next()) return 0.0;
            return rs.getDouble(1);
        }, username);
    }

    private List<WalletRequest> mapResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        List<WalletRequest> list = new ArrayList<>();
        while (rs.next()) {
            String reviewedAtStr = rs.getString("reviewedAt");
            LocalDateTime reviewedAt = reviewedAtStr != null ? LocalDateTime.parse(reviewedAtStr) : null;
            
            list.add(new WalletRequest(
                    rs.getString("id"),
                    LocalDateTime.parse(rs.getString("createdAt")),
                    reviewedAt,
                    rs.getString("username"),
                    TransactionType.valueOf(rs.getString("type")),
                    rs.getDouble("amount"),
                    WalletRequestStatus.valueOf(rs.getString("status")),
                    rs.getString("reviewedBy")
            ));
        }
        return list;
    }
}
