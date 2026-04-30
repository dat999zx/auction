package com.bidify.service;

import java.io.IOException;

import com.bidify.common.dto.UserDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdateProfileRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.network.SocketClient;
import com.bidify.model.ClientSession;
import com.bidify.common.enums.RequestType;

public class UserProfileClientService {
    private final ClientSession clientSession = ClientSession.getInstance();
    private final SocketClient client = SocketClient.getClient();

    public UserDto getCurrentProfile() throws IOException {
        if (clientSession.getCurrentUsername() == null || clientSession.getCurrentUsername().isBlank()) {
            return new UserDto("Guest", "Guest", 0);
        }

        Response response = client.send(new Request(RequestType.GET_PROFILE, null));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new ValidationException(response.getMessage() == null ? "Cannot load profile." : response.getMessage());
        }

        UserDto profile = JsonUtil.fromMap(response.getData(), UserDto.class);
        if (profile == null) {
            throw new ValidationException("Profile came back in an unexpected format.");
        }
        clientSession.setCurrentUser(profile);
        return profile;
    }

    public UserDto getCachedProfile() {
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

    public UserDto updateProfile(String nickname) throws IOException {
        ValidationUtil.validateNickname(nickname);

        Response response = client.send(new Request(RequestType.UPDATE_PROFILE, new UpdateProfileRequest(nickname.trim(), null)));
        return consumeProfileResponse(response, "Cannot update profile.");
    }

    public UserDto addWalletBalance(double amount) throws IOException {
        ValidationUtil.validatePositiveAmount(amount, "Top up amount");
        UserDto currentUser = getCurrentProfile();
        double nextWallet = currentUser.getWallet() + amount;

        Response response = client.send(new Request(RequestType.UPDATE_PROFILE, new UpdateProfileRequest(null, nextWallet)));
        return consumeProfileResponse(response, "Cannot update wallet.");
    }

    public UserDto withdrawWalletBalance(double amount) throws IOException {
        ValidationUtil.validatePositiveAmount(amount, "Withdraw amount");
        UserDto currentUser = getCurrentProfile();
        if (amount > currentUser.getWallet()) {
            throw new ValidationException("Withdraw amount cannot exceed your current wallet balance");
        }

        double nextWallet = currentUser.getWallet() - amount;
        Response response = client.send(new Request(RequestType.UPDATE_PROFILE, new UpdateProfileRequest(null, nextWallet)));
        return consumeProfileResponse(response, "Cannot update wallet.");
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

    private UserDto consumeProfileResponse(Response response, String fallbackMessage) {
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new ValidationException(response.getMessage() == null ? fallbackMessage : response.getMessage());
        }

        UserDto updatedUser = JsonUtil.fromMap(response.getData(), UserDto.class);
        if (updatedUser == null) {
            throw new ValidationException("Profile update came back in an unexpected format.");
        }

        clientSession.setCurrentUser(updatedUser);
        return updatedUser;
    }
}
