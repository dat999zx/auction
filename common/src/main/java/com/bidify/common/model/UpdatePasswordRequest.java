package com.bidify.common.model;

public class UpdatePasswordRequest {
    private String currentPassword;
    private String newPassword;

    // dùng để tạo một đối tượng UpdatePasswordRequest
    public UpdatePasswordRequest() {}

    // dùng để tạo một đối tượng UpdatePasswordRequest
    public UpdatePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() { return currentPassword; }
    public String getNewPassword() { return newPassword; }
}
