package com.bidify.server.service;

import java.util.List;

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
import com.bidify.server.dao.WalletRequestDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.PasswordUtil;
import com.bidify.server.utility.ServiceUtil;
import com.bidify.server.utility.UserMapper;

public class AuthService {
    public static final String BOOTSTRAP_ADMIN_USERNAME = "admin";
    public static final String BOOTSTRAP_ADMIN_PASSWORD = "admin123";
    public static final String BOOTSTRAP_ADMIN_NICKNAME = "Administrator";
    private static AuthService instance = new AuthService();
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserDao userDao = UserDao.getInstance();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();

    // dùng để tạo một đối tượng AuthService
    private AuthService() {}

    // dùng để lấy đối tượng Singleton AuthService
    public static AuthService getInstance() { return instance; }

    // dùng để đăng ký các API routes xác thực với router hệ thống
    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.REGISTER, (client, req) -> register(req));
        router.register(RequestType.LOGIN, this::login);
        router.register(RequestType.LOGOUT, (client, req) -> logout(client));
    }

    // dùng để kiểm tra xem tên đăng nhập có phải là admin khởi tạo hay không
    public static boolean isBootstrapAdminUsername(String username) {
        return BOOTSTRAP_ADMIN_USERNAME.equals(username);
    }

    // đăng kí
    // dùng để xử lý yêu cầu đăng ký tài khoản mới của người dùng
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

            if (userDao.existsByUsername(username) || isBootstrapAdminUsername(username))
                throw new AuthException("Username already exists");

            User user = new User(username, nickname, PasswordUtil.hash(password));
            userDao.create(user);

            return new Response(RequestStatus.SUCCESS, "Register successfully");
        });
    }

    // đăng nhập
    // dùng để xử lý yêu cầu đăng nhập tài khoản từ client
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

            User user = userDao.findByUsername(username);
            if (user == null)
                throw new AuthException("Username or password is incorrect");
            
            if (!PasswordUtil.matches(password, user.getPassword()))
                throw new AuthException("Username or password is incorrect");

            if (user.getStatus() == UserStatus.BANNED)
                throw new AuthException("You have been banned");

            if (RealtimeDatabase.isUserOnline(username))
                throw new AuthException("Another session is already active");

            double lockedBalance = auctionDao.sumWinningBidsForUser(username) + WalletRequestDao.getInstance().sumPendingWithdrawsForUser(username);
            user.getWallet().setlockedBalance(lockedBalance);

            client.setCurrentUsername(username);
            RealtimeDatabase.addActiveUser(client, user);

            UserDto userDto = UserMapper.toDto(user);
            return new Response(RequestStatus.SUCCESS, "Login successfully", userDto);
        });
    }

    // đăng kí
    // dùng để xử lý yêu cầu đăng xuất và dọn dẹp session người dùng
    public Response logout(ClientHandler client){
        return ServiceUtil.handleRequest(() -> {
            String username = client.getCurrentUsername();

            ServiceUtil.requireSession(client);

            User user = RealtimeDatabase.getActiveUser(username);
            if (user == null)
                throw new AuthException("Session is inactive");

            userDao.save(user);
            client.setCurrentUsername(null);
            List<String> affectedAuctionIds = RealtimeDatabase.removeActiveUser(username);
            for (String auctionId : affectedAuctionIds)
                auctionService.publishLiveAudienceUpdate(auctionId);

            return new Response(RequestStatus.SUCCESS, "Logout successfully");
        });
    }

    // dùng để lưu all danh sách người dùng
    public void saveAllUsers(){ // lưu tất cả user data mặc định cập nhật last login
        // dùng để lưu all danh sách người dùng
        saveAllUsers(true);
    }

    // dùng để lưu all danh sách người dùng
    public void saveAllUsers(boolean saveLastLogin){ // lưu tất cả user data
        for (User user : RealtimeDatabase.getAllActiveUsers())
            userDao.save(user, saveLastLogin);
    }
}
