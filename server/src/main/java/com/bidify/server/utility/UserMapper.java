package com.bidify.server.utility;

import com.bidify.common.dto.UserDto;
import com.bidify.server.model.User;

public class UserMapper {
    private UserMapper() {}

    public static UserDto toDto(User user) {
        if (user == null) return null;
        return new UserDto(
            user.getUsername(),
            user.getNickname(),
            WalletMapper.toDto(user.getWallet())
        );
    }
}
