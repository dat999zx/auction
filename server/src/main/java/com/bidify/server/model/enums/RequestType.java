package com.bidify.server.model.enums;

public enum RequestType {
    REGISTER, // Đắng kí
    LOGIN, // Đăng nhập
    LOGOUT, // Đăng xuất

    GET_PROFILE, // Lấy thông tin cơ bản
    UPDATE_PROFILE, // Sửa profile: nickname, phoneNumber, Email, Pass...

    CREATE_AUCTION, // tạo đấu giá
    GET_AUCTIONS, // xem list các cuột đấu giá
    GET_AUCTION_DETAIL, // xem chi tiết cuộc đấu giá
    DELETE_AUCTION, // xóa cuộc đấu giá

    PLACE_BID, // đặt giá
    GET_BID_HISTORY, // xem lịch sử bid

    JOIN_AUCTION, // vào đấu giá
    LEAVE_AUCTION // rời đấu giá
}
