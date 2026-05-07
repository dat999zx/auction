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
import com.bidify.server.utility.UserMapper;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.exception.DatabaseException;

import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;

import java.util.function.Supplier;

// xử lí phần bề mặt của thông tin người dùng (định dạng, xác thực, ...) đưa cho UserDao xử lí với database
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthService {
    private static AuthService instance = new AuthService();
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDao userDao = UserDao.getInstance();
    private final AuctionDao auctionDao = AuctionDao.getInstance();

    private AuthService() {}

    public static AuthService getInstance() { return instance; }

    // đăng kí
    public Response register(Request request) {
        RegisterRequest data = JsonUtil.fromMap(request.getData(), RegisterRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");

        return handleAuthRequest(() -> {
            String username = data.getUsername();
            String nickname = data.getNickname();
            String password = data.getPassword();

            // Không in thẳng password ra
            logger.debug("Register attempt: username={}, nickname={}", username , nickname);


            ValidationUtil.validateUsername(username);
            ValidationUtil.validateNickname(nickname);
            ValidationUtil.validatePassword(password);

            if (userDao.existsByUsername(username))
                return new Response(RequestStatus.FAILED, "Username already exists");

            User user = new User(username, nickname, PasswordUtil.hash(password));
            userDao.create(user);

            return new Response(RequestStatus.SUCCESS, "Register successfully");
        });
    }

    // đăng nhập
    public Response login(ClientHandler client, Request request){
        LoginRequest data = JsonUtil.fromMap(request.getData(), LoginRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request");

        return handleAuthRequest(() -> {
            String username = data.getUsername();
            String password = data.getPassword();

            if (client.isInSession())
                return new Response(RequestStatus.FAILED, "You are already logged in");

            if (!userDao.existsByUsername(username))
                return new Response(RequestStatus.FAILED, "Username or password is incorrect");

            User user = userDao.findByUsername(username);
            if (user == null)
                return new Response(RequestStatus.FAILED, "Failed to get user data");
            
            if (!PasswordUtil.matches(password, user.getPassword()))
                return new Response(RequestStatus.FAILED, "Username or password is incorrect");

            if (user.getStatus() == UserStatus.BANNED)
                return new Response(RequestStatus.FAILED, "You have been banned");

            if (RealtimeDatabase.getUserClient(username) != null)
                return new Response(RequestStatus.FAILED, "Another session is already active");

            double lockedBalance = auctionDao.sumWinningBidsForUser(username);
            user.getWallet().setlockedBalance(lockedBalance);

            client.setCurrentUsername(username);
            RealtimeDatabase.addActiveUser(client, user);

            UserDto userDto = UserMapper.toDto(user);
            return new Response(RequestStatus.SUCCESS, "Login successfully", userDto);
        });
    }

    // đăng kí
    public Response logout(ClientHandler client){
        String username = client.getCurrentUsername();

        return handleAuthRequest(() -> {
            if (!client.isInSession())
                return new Response(RequestStatus.UNAUTHORIZED, "Invalid session");

            User user = RealtimeDatabase.getActiveUser(username);
            if (user == null)
                return new Response(RequestStatus.FAILED, "Session is inactive");

            userDao.save(user);
            client.setCurrentUsername(null);
            RealtimeDatabase.removeActiveUser(username);

            return new Response(RequestStatus.SUCCESS, "Logout successfully");
        });
    }

    public void saveAllUsers(){ // lưu tất cả user data mặc định cập nhật last login
        saveAllUsers(true);
    }

    public void saveAllUsers(boolean saveLastLogin){ // lưu tất cả user data
        for (User user : RealtimeDatabase.getAllActiveUsers()) {
            userDao.save(user, saveLastLogin);
        }
    }

    private Response handleAuthRequest(Supplier<Response> action) {
        try {
            return action.get();
        }
        catch (ValidationException | DatabaseException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }
    }
}
