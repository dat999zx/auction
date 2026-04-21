package com.bidify.service;

import com.bidify.common.dto.UserDto;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.model.ClientSession;

public class UserProfileClientService {
    private final ClientSession clientSession = ClientSession.getInstance();

    public UserDto getCurrentProfile() {
        UserDto currentUser = clientSession.getCurrentUser();
        if (currentUser != null) {
            return currentUser;
        }

        String username = clientSession.getCurrentUsername();
        if (username == null || username.isBlank()) {
            return new UserDto("Guest", "Guest", 0);
        }

        return new UserDto(username, username, 0);
    }

    public UserDto updateProfilePreview(String nickname) {
        ValidationUtil.validateNickname(nickname);
        UserDto currentUser = getCurrentProfile();
        UserDto updatedUser = new UserDto(currentUser.getUsername(), nickname.trim(), currentUser.getWallet());
        clientSession.setCurrentUser(updatedUser);
        return updatedUser;
    }

    public UserDto addWalletBalancePreview(double amount) {
        ValidationUtil.validatePositiveAmount(amount, "Top up amount");
        UserDto currentUser = getCurrentProfile();
        UserDto updatedUser = new UserDto(
            currentUser.getUsername(),
            currentUser.getNickname(),
            currentUser.getWallet() + amount
        );
        clientSession.setCurrentUser(updatedUser);
        return updatedUser;
    }

    public UserDto withdrawWalletBalancePreview(double amount) {
        ValidationUtil.validatePositiveAmount(amount, "Withdraw amount");
        UserDto currentUser = getCurrentProfile();
        if (amount > currentUser.getWallet()) {
            throw new ValidationException("Withdraw amount cannot exceed your current wallet balance");
        }

        UserDto updatedUser = new UserDto(
            currentUser.getUsername(),
            currentUser.getNickname(),
            currentUser.getWallet() - amount
        );
        clientSession.setCurrentUser(updatedUser);
        return updatedUser;
    }

    public void changePasswordPreview(String currentPassword, String newPassword, String confirmPassword) {
        ValidationUtil.validatePassword(currentPassword);
        ValidationUtil.validatePassword(newPassword);
        ValidationUtil.validatePassword(confirmPassword);

        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("Confirm password must match the new password");
        }

        if (currentPassword.equals(newPassword)) {
            throw new ValidationException("New password must be different from the current password");
        }
    }
}
