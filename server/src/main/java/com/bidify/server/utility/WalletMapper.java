package com.bidify.server.utility;

import com.bidify.common.dto.WalletDto;
import com.bidify.server.model.Wallet;

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
