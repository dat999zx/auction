package com.bidify.server.utility;

import com.bidify.common.dto.UserDto;
import com.bidify.server.model.User;

public class UserMapper {
    // dùng để tạo một đối tượng UserMapper
    private UserMapper() {}

    // dùng để chuyển thành đối tượng truyền tải dữ liệu (DTO)
    public static UserDto toDto(User user) {
        if (user == null) return null;
        return new UserDto(
            user.getUsername(),
            user.getNickname(),
            WalletMapper.toDto(user.getWallet()),
            user.getRole()
        );
    }
}
