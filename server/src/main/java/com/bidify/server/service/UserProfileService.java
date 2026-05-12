package com.bidify.server.service;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdatePasswordRequest;
import com.bidify.common.model.UpdateProfileRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.UserDao;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;
import com.bidify.server.utility.ServiceUtil;
import com.bidify.server.utility.UserMapper;

public class UserProfileService {
    private static UserProfileService instance = new UserProfileService();
    private final UserDao userDao = UserDao.getInstance();

    private UserProfileService() {}

    public static UserProfileService getInstance() { return instance; }

    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.GET_PROFILE, (client, req) -> getProfile(client));
        router.register(RequestType.UPDATE_PROFILE, this::updateProfile);
        router.register(RequestType.UPDATE_PASSWORD, this::updatePassword);
    }

    public Response getProfile(ClientHandler client) {
        return ServiceUtil.handleRequest(() -> {
            User user = ServiceUtil.getOrLoadUser(client.getCurrentUsername());
            return new Response(RequestStatus.SUCCESS, "Profile loaded successfully", UserMapper.toDto(user));
        });
    }

    public Response updateProfile(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            UpdateProfileRequest data = JsonUtil.fromMap(request.getData(), UpdateProfileRequest.class);
            ServiceUtil.validateRequestData(data);

            User user = ServiceUtil.getOrLoadUser(client.getCurrentUsername());

            boolean hasChange = false;

            String nickname = data.getNickname();
            if (nickname != null) {
                ValidationUtil.validateNickname(nickname);
                user.setNickname(nickname.trim());
                hasChange = true;
            }

            if (!hasChange)
                throw new ValidationException("No profile changes were provided");

            userDao.save(user, false);
            return new Response(RequestStatus.SUCCESS, "Profile updated successfully", UserMapper.toDto(user));
        });
    }

    public Response updatePassword(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            UpdatePasswordRequest data = JsonUtil.fromMap(request.getData(), UpdatePasswordRequest.class);
            ServiceUtil.validateRequestData(data);
            ServiceUtil.requireSession(client);

            User user = ServiceUtil.getOrLoadUser(client.getCurrentUsername());

            String currentPassword = data.getCurrentPassword();
            String newPassword = data.getNewPassword();

            ValidationUtil.validatePassword(currentPassword);
            ValidationUtil.validatePassword(newPassword);

            if (!PasswordUtil.matches(currentPassword, user.getPassword()))
                throw new ValidationException("Incorrect password");

            if (currentPassword.equals(newPassword))
                throw new ValidationException("New password must be different from current password");

            user.setPassword(PasswordUtil.hash(newPassword));
            userDao.save(user, false);

            return new Response(RequestStatus.SUCCESS, "Password updated successfully");
        });
    }
}
