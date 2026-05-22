package com.bidify.common.dto;

public class PublicProfileDto {
    private String username;
    private String nickname;
    private String profileImageBase64;
    private String email;
    private String phoneNumber;
    private PublicProfileStatsDto stats;
    private AuctionDto[] auctions;

    // dùng để tạo một đối tượng PublicProfileDto
    public PublicProfileDto() {}

    // dùng để tạo một đối tượng PublicProfileDto với email và số điện thoại
    public PublicProfileDto(String username, String nickname, String profileImageBase64, String email, String phoneNumber, PublicProfileStatsDto stats, AuctionDto[] auctions) {
        this.username = username;
        this.nickname = nickname;
        this.profileImageBase64 = profileImageBase64;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.stats = stats;
        this.auctions = auctions;
    }

    public String getUsername() { return username; }
    public String getNickname() { return nickname; }
    public String getProfileImageBase64() { return profileImageBase64; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }
    public PublicProfileStatsDto getStats() { return stats; }
    public AuctionDto[] getAuctions() { return auctions; }
}
