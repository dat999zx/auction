package com.bidify.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.UserDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.UserStatus;
import com.bidify.common.exception.AuthException;
import com.bidify.common.model.LoginRequest;
import com.bidify.common.model.RegisterRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;
import com.bidify.server.utility.ServiceUtil;
import com.bidify.server.utility.UserMapper;

public class AuthService {
    private static AuthService instance = new AuthService();
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDao userDao = UserDao.getInstance();
    private final AuctionDao auctionDao = AuctionDao.getInstance();

    private AuthService() {}

    public static AuthService getInstance() { return instance; }

    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.REGISTER, (client, req) -> register(req));
        router.register(RequestType.LOGIN, this::login);
        router.register(RequestType.LOGOUT, (client, req) -> logout(client));
    }

    // đăng kí
    public Response register(Request request) {
        return ServiceUtil.handleRequest(() -> {
            RegisterRequest data = JsonUtil.fromMap(request.getData(), RegisterRequest.class);
            ServiceUtil.validateRequestData(data);

            String username = data.getUsername();
            String nickname = data.getNickname();
            String password = data.getPassword();

            // Không in thẳng password ra
            logger.debug("Register attempt: username={}, nickname={}", username , nickname);


            ValidationUtil.validateUsername(username);
            ValidationUtil.validateNickname(nickname);
            ValidationUtil.validatePassword(password);

            if (userDao.existsByUsername(username))
                throw new AuthException("Username already exists");

            User user = new User(username, nickname, PasswordUtil.hash(password));
            userDao.create(user);

            return new Response(RequestStatus.SUCCESS, "Register successfully");
        });
    }

    // đăng nhập
    public Response login(ClientHandler client, Request request){
        return ServiceUtil.handleRequest(() -> {
            LoginRequest data = JsonUtil.fromMap(request.getData(), LoginRequest.class);
            ServiceUtil.validateRequestData(data);

            String username = data.getUsername();
            String password = data.getPassword();

            ValidationUtil.requiresNonBlank(username, "Username");
            ValidationUtil.requiresNonBlank(password, "Password");

            if (client.isInSession())
                throw new AuthException("You are already logged in");

            if (!userDao.existsByUsername(username))
                throw new AuthException("Username or password is incorrect");

            User user = userDao.findByUsername(username);
            if (user == null)
                throw new AuthException("Failed to get user data");
            
            if (!PasswordUtil.matches(password, user.getPassword()))
                throw new AuthException("Username or password is incorrect");

            if (user.getStatus() == UserStatus.BANNED)
                throw new AuthException("You have been banned");

            if (RealtimeDatabase.isUserOnline(username))
                throw new AuthException("Another session is already active");

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
        return ServiceUtil.handleRequest(() -> {
            String username = client.getCurrentUsername();

            ServiceUtil.requireSession(client);

            User user = RealtimeDatabase.getActiveUser(username);
            if (user == null)
                throw new AuthException("Session is inactive");

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
        for (User user : RealtimeDatabase.getAllActiveUsers())
            userDao.save(user, saveLastLogin);
    }
}
