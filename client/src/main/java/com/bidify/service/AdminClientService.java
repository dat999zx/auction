package com.bidify.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.AdminUserDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UserTargetRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.network.SocketClient;

public class AdminClientService {
    private final SocketClient client = SocketClient.getClient();

    public List<AdminUserDto> getUsers() throws IOException {
        Response response = client.send(new Request(RequestType.GET_ADMIN_USERS, null));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null)
            throw new ValidationException(response.getMessage() == null ? "Cannot load users." : response.getMessage());

        List<?> rawUsers = JsonUtil.fromMap(response.getData(), List.class);
        List<AdminUserDto> users = new ArrayList<>();
        if (rawUsers == null)
            return users;

        for (Object rawUser : rawUsers) {
            AdminUserDto user = JsonUtil.fromMap(rawUser, AdminUserDto.class);
            if (user != null)
                users.add(user);
        }

        return users;
    }

    public void banUser(String username) throws IOException {
        executeUserAction(RequestType.BAN_USER, username, "Cannot ban user.");
    }

    public void promoteAdmin(String username) throws IOException {
        executeUserAction(RequestType.PROMOTE_ADMIN, username, "Cannot promote user.");
    }

    public void demoteAdmin(String username) throws IOException {
        executeUserAction(RequestType.DEMOTE_ADMIN, username, "Cannot remove admin.");
    }

    public void unbanUser(String username) throws IOException {
        executeUserAction(RequestType.UNBAN_USER, username, "Cannot unban user.");
    }

    public void deleteUser(String username) throws IOException {
        executeUserAction(RequestType.DELETE_USER, username, "Cannot delete user.");
    }

    private void executeUserAction(RequestType requestType, String username, String fallbackMessage) throws IOException {
        ValidationUtil.validateUsername(username);

        Response response = client.send(new Request(requestType, new UserTargetRequest(username)));
        if (response.getStatus() != RequestStatus.SUCCESS)
            throw new ValidationException(response.getMessage() == null ? fallbackMessage : response.getMessage());
    }
}
