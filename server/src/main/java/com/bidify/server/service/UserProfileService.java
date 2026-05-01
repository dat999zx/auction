package com.bidify.server.service;

import java.util.function.Supplier;

import com.bidify.common.dto.UserDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdateProfileRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.UserMapper;

public class UserProfileService {
    private static UserProfileService instance = new UserProfileService();
    private final UserDao userDao = UserDao.getInstance();

    private UserProfileService() {}

    public static UserProfileService getInstance() { return instance; }

    public Response getProfile(ClientHandler client) {
        return handleProfileRequest(() -> {
            User user = requireActiveUser(client);
            return new Response(RequestStatus.SUCCESS, "Profile loaded successfully", UserMapper.toDto(user));
        });
    }

    public Response updateProfile(ClientHandler client, Request request) {
        UpdateProfileRequest data = JsonUtil.fromMap(request.getData(), UpdateProfileRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");

        return handleProfileRequest(() -> {
            User user = requireActiveUser(client);

            boolean hasChange = false;

            String nickname = data.getNickname();
            if (nickname != null) {
                ValidationUtil.validateNickname(nickname);
                user.setNickname(nickname.trim());
                hasChange = true;
            }

            Double wallet = data.getWallet();
            if (wallet != null) {
                if (wallet < 0) {
                    throw new ValidationException("Wallet balance cannot be negative");
                }
                user.setWallet(wallet);
                hasChange = true;
            }

            if (!hasChange) {
                return new Response(RequestStatus.INVALID_REQUEST, "No profile changes were provided");
            }

            userDao.save(user, false);
            return new Response(RequestStatus.SUCCESS, "Profile updated successfully", UserMapper.toDto(user));
        });
    }

    private Response handleProfileRequest(Supplier<Response> action) {
        try {
            return action.get();
        } catch (ValidationException | DatabaseException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }
    }

    private User requireActiveUser(ClientHandler client) {
        if (client == null || !client.isInSession()) {
            throw new ValidationException("Invalid session");
        }

        String username = client.getCurrentUsername();
        User user = RealtimeDatabase.getActiveUser(username);
        if (user != null) {
            return user;
        }

        user = userDao.findByUsername(username);
        if (user == null) {
            throw new ValidationException("User not found");
        }
        return user;
    }
}
