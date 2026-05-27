package com.bidify.common.model;

// Dữ liệu gửi lên khi đổi mật khẩu
public class UpdatePasswordRequest {
    // Mật khẩu hiện tại (để xác minh)
    private String currentPassword;
    // Mật khẩu mới muốn đặt
    private String newPassword;

    public UpdatePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() { return currentPassword; }
    public String getNewPassword() { return newPassword; }
}
