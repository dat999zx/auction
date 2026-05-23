package com.bidify.service;

import java.io.IOException;

import com.bidify.common.dto.UserDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.AuthException;
import com.bidify.common.model.LoginRequest;
import com.bidify.common.model.LogoutRequest;
import com.bidify.common.model.RegisterRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.network.SocketClient;
import com.bidify.utility.SceneManager;

public class AuthClientService {
    private final SocketClient client = SocketClient.getClient();

    public Response login(String username, String password) throws IOException {
        ValidationUtil.validateUsername(username);
        ValidationUtil.validatePassword(password);

        Response response = client.send(new Request(RequestType.LOGIN, new LoginRequest(username, password)));
        if (response.getStatus() == RequestStatus.SUCCESS) {
            client.setCurrentUsername(username);
            if (response.getData() != null) {
                client.getClientSession().setCurrentUser(JsonUtil.fromMap(response.getData(), UserDto.class));
            }
            return response;
        }
        throw new AuthException(response.getMessage());
    }

    public Response register(String username, String password) throws IOException {
        ValidationUtil.validateUsername(username);
        ValidationUtil.validatePassword(password);

        Response response = client.send(new Request(RequestType.REGISTER, new RegisterRequest(username, password)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuthException(response.getMessage());
    }

    public Response logout() throws IOException {
        String currentUsername = client.getCurrentUsername();
        if (currentUsername == null || currentUsername.isBlank()) {
            client.setCurrentUsername(null);
            return new Response(RequestStatus.SUCCESS, "Logged out");
        }

        Response response = client.send(new Request(RequestType.LOGOUT, new LogoutRequest()));
        if (response.getStatus() == RequestStatus.SUCCESS) {
            client.getClientSession().clear();
            SceneManager.clearAllCache();
            return response;
        }
        throw new AuthException(response.getMessage());
    }
}
