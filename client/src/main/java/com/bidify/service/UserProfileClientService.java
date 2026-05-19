package com.bidify.service;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.UserDto;
import com.bidify.common.dto.WalletDto;
import com.bidify.common.dto.WalletRequestDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.UserRole;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdatePasswordRequest;
import com.bidify.common.model.UpdateProfileRequest;
import com.bidify.common.model.WalletRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.model.ClientSession;
import com.bidify.network.SocketClient;

public class UserProfileClientService {
    private final ClientSession clientSession = ClientSession.getInstance();
    private final SocketClient client = SocketClient.getClient();

    // dùng để lấy current thông tin tài khoản
    public UserDto getCurrentProfile() throws IOException {
        if (clientSession.getCurrentUsername() == null || clientSession.getCurrentUsername().isBlank()) {
            return new UserDto("Guest", "Guest", new WalletDto(0, 0), UserRole.USER);
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

    // dùng để lấy cached thông tin tài khoản
    public UserDto getCachedProfile() {
        UserDto currentUser = clientSession.getCurrentUser();
        if (currentUser != null) {
            return currentUser;
        }

        String username = clientSession.getCurrentUsername();
        if (username == null || username.isBlank()) {
            return new UserDto("Guest", "Guest", new WalletDto(0, 0), UserRole.USER);
        }

        return new UserDto(username, username, new WalletDto(0, 0), UserRole.USER);
    }

    // dùng để cập nhật thông tin tài khoản
    public UserDto updateProfile(String nickname) throws IOException {
        ValidationUtil.validateNickname(nickname);

        Response response = client.send(new Request(RequestType.UPDATE_PROFILE, new UpdateProfileRequest(nickname.trim(), null)));
        // dùng để xử lý kết quả thông tin tài khoản kết quả trả về (Response)
        return consumeProfileResponse(response, "Cannot update profile.");
    }

    // dùng để gửi yêu cầu nạp tiền lên server thông qua socket connection
    public void addWalletBalance(double amount) throws IOException {
        ValidationUtil.validatePositiveAmount(amount, "Deposit amount");
        Response response = client.send(new Request(RequestType.DEPOSIT, new WalletRequest(amount)));
        if (response.getStatus() != RequestStatus.SUCCESS)
            throw new ValidationException(response.getMessage() == null ? "Cannot submit deposit request." : response.getMessage());
    }

    // dùng để gửi yêu cầu rút tiền lên server thông qua socket connection
    public void withdrawWalletBalance(double amount) throws IOException {
        ValidationUtil.validatePositiveAmount(amount, "Withdraw amount");
        Response response = client.send(new Request(RequestType.WITHDRAW, new WalletRequest(amount)));
        if (response.getStatus() != RequestStatus.SUCCESS)
            throw new ValidationException(response.getMessage() == null ? "Cannot submit withdraw request." : response.getMessage());
    }

    // dùng để lấy toàn bộ danh sách lịch sử nạp/rút tiền của user này từ server
    public List<WalletRequestDto> getUserWalletRequests() throws IOException {
        Response response = client.send(new Request(RequestType.GET_WALLET_REQUEST_HISTORY, null));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null)
            throw new ValidationException(response.getMessage() == null ? "Cannot load wallet requests." : response.getMessage());

        List<?> rawRequests = JsonUtil.fromMap(response.getData(), List.class);
        List<WalletRequestDto> requests = new ArrayList<>();
        if (rawRequests == null)
            return requests;

        for (Object rawReq : rawRequests) {
            WalletRequestDto req = JsonUtil.fromMap(rawReq, WalletRequestDto.class);
            if (req != null)
                requests.add(req);
        }

        return requests;
    }

    // dùng để change mật khẩu
    public void changePassword(String currentPassword, String newPassword, String confirmPassword) throws IOException {
        ValidationUtil.validatePassword(currentPassword);
        ValidationUtil.validatePassword(newPassword);
        ValidationUtil.validatePassword(confirmPassword);

        if (!newPassword.equals(confirmPassword))
            throw new ValidationException("Confirm password must match the new password");

        if (currentPassword.equals(newPassword))
            throw new ValidationException("New password must be different from the current password");

        Response response = client.send(new Request(RequestType.UPDATE_PASSWORD, new UpdatePasswordRequest(currentPassword, newPassword)));
        if (response.getStatus() != RequestStatus.SUCCESS)
            throw new ValidationException(response.getMessage() == null ? "Cannot update password." : response.getMessage());
    }

    // dùng để change mật khẩu preview
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

    // dùng để xử lý kết quả thông tin tài khoản kết quả trả về (Response)
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
