package com.bidify.server.service;

import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.AdminUserDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;
import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UserTargetRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.service.admin.AdminUserCleanupService;
import com.bidify.server.utility.ServiceUtil;

public class AdminService {
    private static final AdminService instance = new AdminService();

    private final UserDao userDao = UserDao.getInstance();
    private final AuctionDao auctionDao = AuctionDao.getInstance();
    private final ItemDao itemDao = ItemDao.getInstance();
    private final BidDao bidDao = BidDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();
    private final ImageDao imageDao = ImageDao.getInstance();
    private final ImageService imageService = ImageService.getInstance();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final AdminUserCleanupService userCleanupService =
            new AdminUserCleanupService(
                    userDao,
                    auctionDao,
                    itemDao,
                    bidDao,
                    transactionDao,
                    imageDao,
                    imageService,
                    auctionService);

    private AdminService() {}

    public static AdminService getInstance() { return instance; }

    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.GET_ADMIN_USERS, (client, req) -> getUsers(client));
        router.register(RequestType.PROMOTE_ADMIN, this::promoteAdmin);
        router.register(RequestType.DEMOTE_ADMIN, this::demoteAdmin);
        router.register(RequestType.BAN_USER, this::banUser);
        router.register(RequestType.UNBAN_USER, this::unbanUser);
        router.register(RequestType.DELETE_USER, this::deleteUser);
    }

    public Response getUsers(ClientHandler client) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);

            List<AdminUserDto> dtos = new ArrayList<>();
            for (User user : userDao.findAll())
                dtos.add(new AdminUserDto(user.getUsername(), user.getNickname(), user.getStatus(), user.getRole()));

            return new Response(RequestStatus.SUCCESS, "Users loaded successfully", dtos);
        });
    }

    // Cấm người dùng và buộc đăng xuất.
    public Response banUser(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);

            User target = requireManageableTarget(request);
            target.setStatus(UserStatus.BANNED);
            userDao.save(target, false);
            userCleanupService.forceLogoutUser(target.getUsername(), "Your account has been banned.");

            return new Response(RequestStatus.SUCCESS, "User banned successfully");
        });
    }

    // Gỡ cấm người dùng.
    public Response unbanUser(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);

            User target = requireManageableTarget(request);
            target.setStatus(UserStatus.ACTIVE);
            userDao.save(target, false);

            return new Response(RequestStatus.SUCCESS, "User unbanned successfully");
        });
    }

    // Xóa tài khoản người dùng và dọn dẹp toàn bộ dữ liệu liên quan (Cascade).
    public Response deleteUser(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);

            User target = requireManageableTarget(request);
            userCleanupService.deleteUser(target);

            return new Response(RequestStatus.SUCCESS, "User deleted successfully");
        });
    }

    // Thăng chức user thành admin.
    public Response promoteAdmin(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireBootstrapAdmin(client);

            User target = requireAdminRoleTarget(request);
            if (target.getRole() == UserRole.ADMIN)
                throw new ValidationException("User is already an admin");

            target.setRole(UserRole.ADMIN);
            userDao.save(target, false);
            userCleanupService.forceLogoutUser(target.getUsername(), "Your account role has changed. Please log in again.");

            return new Response(RequestStatus.SUCCESS, "User promoted to admin successfully");
        });
    }

    // Bãi nhiệm quyền admin của user.
    public Response demoteAdmin(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireBootstrapAdmin(client);

            User target = requireAdminRoleTarget(request);
            if (target.getRole() != UserRole.ADMIN)
                throw new ValidationException("User is not an admin");

            target.setRole(UserRole.USER);
            userDao.save(target, false);
            userCleanupService.forceLogoutUser(target.getUsername(), "Your account role has changed. Please log in again.");

            return new Response(RequestStatus.SUCCESS, "Admin removed successfully");
        });
    }

    private User requireManageableTarget(Request request) {
        User target = requireExistingTarget(request);
        ServiceUtil.requireNonAdmin(target, "Cannot manage admin accounts");
        return target;
    }

    private User requireAdminRoleTarget(Request request) {
        return requireExistingTarget(request);
    }

    private User requireExistingTarget(Request request) {
        UserTargetRequest data = JsonUtil.fromMap(request.getData(), UserTargetRequest.class);
        ServiceUtil.validateRequestData(data);

        String username = data.getUsername();
        ValidationUtil.validateUsername(username);

        if (AuthService.BOOTSTRAP_ADMIN_USERNAME.equals(username))
            throw new AuthException("Cannot manage the bootstrap admin account");
        return ServiceUtil.getOrLoadUser(username);
    }
}
