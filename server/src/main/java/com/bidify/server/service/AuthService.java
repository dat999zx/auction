package com.bidify.server.service;

import java.time.LocalDateTime;

import javax.security.auth.login.LoginContext;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.UserStatus;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.LogoutRequest;
import com.bidify.common.model.LoginRequest;
import com.bidify.common.model.RegisterRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.util.JsonUtil;
import com.bidify.common.util.ValidationUtil;
import com.bidify.server.utility.PasswordUtil;
import com.bidify.server.exception.DatabaseException;

import com.bidify.server.model.User;
import com.bidify.server.repository.UserRepository;

// xử lí phần bề mặt của thông tin người dùng (định dạng, ...) đưa cho UserRepository xử lí với database
public class AuthService {
    private final UserRepository userRepository = new UserRepository();

    // đăng kí
    public Response register(Request request) {
        RegisterRequest data = JsonUtil.fromMap(request.getData(), RegisterRequest.class);

        String username = data.getUsername();
        String nickname = data.getNickname();
        String email = data.getEmail();
        String password = data.getPassword();

        try{
            ValidationUtil.validateUsername(username);
            if (userRepository.existsByUsername(username)) throw new ValidationException("Username already exists");
            ValidationUtil.validateNickname(nickname);
            ValidationUtil.validateEmail(email);
            ValidationUtil.validatePassword(password);
        }
        catch (ValidationException e){
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        try{
            User user = new User(nickname, username, PasswordUtil.hash(password), email);;
            if (!userRepository.save(user)) throw new DatabaseException("Failed to save User");
        }
        catch (DatabaseException e){
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        return new Response(RequestStatus.SUCCESS, "Register successfully");
    }

    // đăng nhập
    public Response login(Request request){
        LoginRequest data = JsonUtil.fromMap(request.getData(), LoginRequest.class);

        String username = data.getUsername();
        String password = data.getPassword();

        if (!userRepository.existsByUsername(username))
            return new Response(RequestStatus.FAILED, "Username or password is incorrect");

        User user = userRepository.findByUsername(username);
        if (user == null)
            return new Response(RequestStatus.FAILED, "Failed to get user data");

        if (!PasswordUtil.matches(password, user.getPassword()))
            return new Response(RequestStatus.FAILED, "Username or password is incorrect");
        
        if (user.getStatus() == UserStatus.BANNED)
            return new Response(RequestStatus.FAILED, "You have been banned");

        if (user.isInSession())
            return new Response(RequestStatus.FAILED, "Another session is active");

        userRepository.updateInSession(username, true);
        user.setInSession(true);
        
        return new Response(RequestStatus.SUCCESS, "Login successfully", user);
    }

    // đăng kí
    public Response logout(Request request){
        LogoutRequest data = JsonUtil.fromMap(request.getData(), LogoutRequest.class);
        String username = data.getUsername();

        if (!userRepository.existsByUsername(username)){
            return new Response(RequestStatus.FAILED, "User is not found");
        }
 
        userRepository.updateInSession(username, false);
        userRepository.updateLastLogin(username, LocalDateTime.now().toString());

        return new Response(RequestStatus.SUCCESS, "Logout successfully");
    }

}
