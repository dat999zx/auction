package com.bidify.server.utility;

import com.bidify.common.dto.WalletDto;
import com.bidify.server.model.Wallet;

public class WalletMapper {
    // dùng để tạo một đối tượng WalletMapper
    private WalletMapper() {}

    // dùng để chuyển thành đối tượng truyền tải dữ liệu (DTO)
    public static WalletDto toDto(Wallet wallet) {
        if (wallet == null) return null;
        return new WalletDto(
            wallet.getAvailableBalance(),
            wallet.getLockedBalance()
        );
    }
}
