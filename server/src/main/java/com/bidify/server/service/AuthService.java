package com.bidify.server.service;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.model.RegisterRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.util.JsonUtil;
import com.bidify.server.model.User;
import com.bidify.server.repository.UserRepository;

// xử lí phần bề mặt của thông tin người dùng (định dạng, ...) đưa cho UserRepository xử lí với database
public class AuthService {
    private final UserRepository userRepository = new UserRepository();

    public Response register(Request request) {
        RegisterRequest data = JsonUtil.fromMap(request.getData(), RegisterRequest.class);

        if (data.getUsername() == null || data.getUsername().isBlank())
            return new Response(RequestStatus.FAILED, "Username is required");

        if (userRepository.existsByUsername(data.getUsername()))
            return new Response(RequestStatus.FAILED, "Username already exists");

        User user = new User("1234", "test", "bru", "testing", "haha@", "912312");
        userRepository.save(user);

        return new Response(RequestStatus.SUCCESS, "Register successful");
    }
}
