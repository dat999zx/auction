package com.bidify.service;

import java.io.IOException;
import com.bidify.common.dto.PublicProfileDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.PublicProfileRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.network.SocketClient;

public class PublicProfileClientService {
    private final SocketClient client = SocketClient.getClient();

    public PublicProfileDto getPublicProfile(String username) throws IOException {
        if (username == null || username.isBlank()) {
            throw new ValidationException("Username cannot be empty");
        }

        Response response = client.send(new Request(RequestType.GET_PUBLIC_PROFILE, new PublicProfileRequest(username)));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new ValidationException(response.getMessage() == null ? "Cannot load public profile." : response.getMessage());
        }

        PublicProfileDto publicProfile = JsonUtil.fromMap(response.getData(), PublicProfileDto.class);
        if (publicProfile == null) {
            throw new ValidationException("Public profile came back in an unexpected format.");
        }
        return publicProfile;
    }
}
