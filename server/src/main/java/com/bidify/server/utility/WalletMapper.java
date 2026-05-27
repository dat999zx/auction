package com.bidify.server.utility;

import com.bidify.common.dto.WalletDto;
import com.bidify.server.model.Wallet;

// Chuyển đổi object Wallet (server) → WalletDto (gửi cho client), gồm số dư khả dụng và số dư bị khóa
public class WalletMapper {
    private WalletMapper() {}

    public static WalletDto toDto(Wallet wallet) {
        if (wallet == null) return null;
        return new WalletDto(
            wallet.getAvailableBalance(),
            wallet.getLockedBalance()
        );
    }
}
