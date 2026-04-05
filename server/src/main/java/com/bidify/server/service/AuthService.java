package com.bidify.server.service;

import com.bidify.common.dto.UserDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.UserStatus;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.LoginRequest;
import com.bidify.common.model.RegisterRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.utility.PasswordUtil;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.exception.DatabaseException;

import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

// xử lí phần bề mặt của thông tin người dùng (định dạng, xác thực, ...) đưa cho UserDao xử lí với database
public class AuthService {
    private final UserDao userDao = new UserDao();

    // đăng kí
    public Response register(Request request) {
        RegisterRequest data = JsonUtil.fromMap(request.getData(), RegisterRequest.class);

        String username = data.getUsername();
        String nickname = data.getNickname();
        String password = data.getPassword();

        try{
            ValidationUtil.validateUsername(username);
            if (userDao.existsByUsername(username)) throw new ValidationException("Username already exists");
            ValidationUtil.validateNickname(nickname);
            ValidationUtil.validatePassword(password);
        }
        catch (ValidationException e){
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        try{
            User user = new User(username, nickname, PasswordUtil.hash(password));
            if (!userDao.create(user)) throw new DatabaseException("Failed to save User");
        }
        catch (DatabaseException e){
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        return new Response(RequestStatus.SUCCESS, "Register successfully");
    }

    // đăng nhập
    public Response login(ClientHandler client, Request request){
        LoginRequest data = JsonUtil.fromMap(request.getData(), LoginRequest.class);

        String username = data.getUsername();
        String password = data.getPassword();

        if (client.isValidClient())
            return new Response(RequestStatus.FAILED, "Your are already logged in");

        if (!userDao.existsByUsername(username))
            return new Response(RequestStatus.FAILED, "Username or password is incorrect");

        User user = userDao.findByUsername(username);
        if (user == null)
            return new Response(RequestStatus.FAILED, "Failed to get user data");

        if (!PasswordUtil.matches(password, user.getPassword()))
            return new Response(RequestStatus.FAILED, "Username or password is incorrect");
        
        if (user.getStatus() == UserStatus.BANNED)
            return new Response(RequestStatus.FAILED, "You have been banned");
        
        if (RealtimeDatabase.getActiveClient(username) != null)
            return new Response(RequestStatus.FAILED, "Another session is already active");

        client.setCurrentUsername(username);
        RealtimeDatabase.addActiveClient(client, user);

        UserDto userDto = new UserDto(user.getUsername(), user.getNickname(), user.getWallet());
        
        return new Response(RequestStatus.SUCCESS, "Login successfully", userDto);
    }

    // đăng kí
    public Response logout(ClientHandler client, Request request){
        String username = client.getCurrentUsername();

        if (!client.isValidClient())
            return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");

        User user = RealtimeDatabase.getActiveUser(username);
        if (!userDao.existsByUsername(username) || user == null)
            return new Response(RequestStatus.FAILED, "User is not found or not active");

        if (RealtimeDatabase.getActiveClient(username) == null)
            return new Response(RequestStatus.FAILED, "Session is inactive");
        
        userDao.save(user);
        client.setCurrentUsername(null);
        RealtimeDatabase.removeActiveClient(username);

        System.out.println(username + " logged out");

        return new Response(RequestStatus.SUCCESS, "Logout successfully");
    }

    public void saveAllClients(){ // lưu tất cả client data mặc định cập nhật last login
        saveAllClients(true);
    }

    public void saveAllClients(boolean saveLastLogin){ // lưu tất cả client data
        for (User user : RealtimeDatabase.getAllActiveUsers())
            userDao.save(user, saveLastLogin);
    }
}
