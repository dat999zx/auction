package com.bidify.common.model;

import java.time.LocalDateTime;

/**
 * Admin - System administrator with special privileges
 */
public class Admin extends User {
    private static final long serialVersionUID = 1L;
    
    private String adminLevel;
    private boolean canModerateAuctions; // có thể quản lý đấu giá
    private boolean canManageUsers;
    private boolean canViewReports; // có Thể Xem Báo Cáo
    private LocalDateTime lastActionTime; // thời gian hành động cuối
    private String lastAction; // nội dung hành động cuối
    
    public Admin() {
        super();
        this.adminLevel = "STANDARD"; // Tiêu chuẩn
        this.canModerateAuctions = false;
        this.canManageUsers = false;
        this.canViewReports = false;
        this.lastActionTime = null;
        this.lastAction = "";
    }
    
    public Admin(String username, String email, String password, String fullName) {
        super(username, email, password, fullName);
        this.adminLevel = "STANDARD";
        this.canModerateAuctions = false;
        this.canManageUsers = false;
        this.canViewReports = false;
        this.lastActionTime = null;
        this.lastAction = "";
    }
    
    @Override
    public String getUserType() {
        return "ADMIN";
    }
    
    public String getAdminLevel() {
        return adminLevel;
    }
    
    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }
    
    public boolean canModerateAuctions() {
        return canModerateAuctions;
    }
    
    public void setCanModerateAuctions(boolean canModerateAuctions) {
        this.canModerateAuctions = canModerateAuctions;
    }
    
    public boolean canManageUsers() {
        return canManageUsers;
    }
    
    public void setCanManageUsers(boolean canManageUsers) {
        this.canManageUsers = canManageUsers;
    }
    
    public boolean canViewReports() {
        return canViewReports;
    }
    
    public void setCanViewReports(boolean canViewReports) {
        this.canViewReports = canViewReports;
    }
    
    public LocalDateTime getLastActionTime() {
        return lastActionTime;
    }
    
    public void setLastActionTime(LocalDateTime lastActionTime) {
        this.lastActionTime = lastActionTime;
    }
    
    public String getLastAction() {
        return lastAction;
    }
    
    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }
    
    public void logAction(String action) {
        this.lastAction = action;
        this.lastActionTime = LocalDateTime.now();
    }
}
