package com.bidify.common.model;

// Dữ liệu gửi lên khi cập nhật thông tin cá nhân
public class UpdateProfileRequest {
    // Tên hiển thị mới
    private String nickname;
    // Số tiền nạp vào ví (null nếu không nạp)
    private Double wallet;
    // Ảnh đại diện mới (encode base64, null nếu không đổi)
    private String profileImageBase64;

    // Email mới
    private String email;
    // Số điện thoại mới
    private String phoneNumber;

    public UpdateProfileRequest(String nickname, Double wallet) {
        this(nickname, wallet, null, null, null);
    }

    public UpdateProfileRequest(String nickname, Double wallet, String profileImageBase64) {
        this(nickname, wallet, profileImageBase64, null, null);
    }

    public UpdateProfileRequest(String nickname, Double wallet, String profileImageBase64, String email, String phoneNumber) {
        this.nickname = nickname;
        this.wallet = wallet;
        this.profileImageBase64 = profileImageBase64;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getNickname() { return nickname; }
    public Double getWallet() { return wallet; }
    public String getProfileImageBase64() { return profileImageBase64; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
}
