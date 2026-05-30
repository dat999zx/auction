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
import com.bidify.navigation.SceneManager;

import com.bidify.model.ClientSession;

public class AuthClientService {
    private final SocketClient client = SocketClient.getClient();
    private final ClientSession clientSession = ClientSession.getInstance();

    public Response login(String username, String password) throws IOException {
        ValidationUtil.validateUsername(username);
        ValidationUtil.validatePassword(password);

        Response response = client.send(new Request(RequestType.LOGIN, new LoginRequest(username, password)));
        if (response.getStatus() == RequestStatus.SUCCESS) {
            if (response.getData() != null) {
                clientSession.setCurrentUser(JsonUtil.fromMap(response.getData(), UserDto.class));
            }
            else {
                clientSession.setCurrentUsername(username);
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
        String currentUsername = clientSession.getCurrentUsername();
        if (currentUsername == null || currentUsername.isBlank()) {
            clientSession.clear();
            SceneManager.clearAllCache();
            SceneManager.resetMissionBar();
            SceneManager.preloadAuthScenes();
            return new Response(RequestStatus.SUCCESS, "Logged out");
        }

        Response response = client.send(new Request(RequestType.LOGOUT, new LogoutRequest()));
        if (response.getStatus() == RequestStatus.SUCCESS) {
            clientSession.clear();
            SceneManager.clearAllCache();
            SceneManager.resetMissionBar();
            SceneManager.preloadAuthScenes();
            return response;
        }
        throw new AuthException(response.getMessage());
    }
}
