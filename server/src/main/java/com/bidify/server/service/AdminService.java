package com.bidify.server.service;

import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.AdminUserDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.UserStatus;
import com.bidify.common.exception.AuthException;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Event;
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
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
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

    // dùng để tạo một đối tượng AdminService
    private AdminService() {}

    // dùng để lấy đối tượng Singleton
    public static AdminService getInstance() { return instance; }

    // dùng để khởi tạo
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

    // dùng để cấm người dùng
    public Response banUser(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);

            User target = requireManageableTarget(request);
            target.setStatus(UserStatus.BANNED);
            userDao.save(target, false);
            forceLogoutUser(target.getUsername(), "Your account has been banned.");

            return new Response(RequestStatus.SUCCESS, "User banned successfully");
        });
    }

    // dùng để gỡ cấm người dùng
    public Response unbanUser(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);

            User target = requireManageableTarget(request);
            target.setStatus(UserStatus.ACTIVE);
            userDao.save(target, false);

            return new Response(RequestStatus.SUCCESS, "User unbanned successfully");
        });
    }

    // dùng để xóa người dùng
    public Response deleteUser(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);

            User target = requireManageableTarget(request);
            String username = target.getUsername();

            // dùng để ngắt kết nối người dùng
            disconnectUser(username, "Your account has been deleted by an administrator.");

            for (Auction auction : auctionDao.findBySellerUsername(username))
                // dùng để xóa đấu giá cascade
                deleteAuctionCascade(auction);

            for (Item item : itemDao.findByOwnerUsername(username))
                // dùng để xóa sản phẩm cascade
                deleteItemCascade(item);

            bidDao.deleteByUsername(username);
            transactionDao.deleteByUsername(username);
            userDao.deleteByUsername(username);

            return new Response(RequestStatus.SUCCESS, "User deleted successfully");
        });
    }

    // dùng để thăng chức quản trị viên (admin)
    public Response promoteAdmin(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireBootstrapAdmin(client);

            User target = requireAdminRoleTarget(request);
            if (target.getRole() == UserRole.ADMIN)
                throw new ValidationException("User is already an admin");

            target.setRole(UserRole.ADMIN);
            userDao.save(target, false);
            forceLogoutUser(target.getUsername(), "Your account role has changed. Please log in again.");

            return new Response(RequestStatus.SUCCESS, "User promoted to admin successfully");
        });
    }

    // dùng để bãi nhiệm quản trị viên (admin)
    public Response demoteAdmin(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireBootstrapAdmin(client);

            User target = requireAdminRoleTarget(request);
            if (target.getRole() != UserRole.ADMIN)
                throw new ValidationException("User is not an admin");

            target.setRole(UserRole.USER);
            userDao.save(target, false);
            forceLogoutUser(target.getUsername(), "Your account role has changed. Please log in again.");

            return new Response(RequestStatus.SUCCESS, "Admin removed successfully");
        });
    }

    // dùng để bắt buộc phải có manageable target
    private User requireManageableTarget(Request request) {
        User target = requireExistingTarget(request);
        ServiceUtil.requireNonAdmin(target, "Cannot manage admin accounts");
        return target;
    }

    // dùng để bắt buộc phải có quản trị viên (admin) vai trò target
    private User requireAdminRoleTarget(Request request) {
        // dùng để bắt buộc phải có existing target
        return requireExistingTarget(request);
    }

    // dùng để bắt buộc phải có existing target
    private User requireExistingTarget(Request request) {
        UserTargetRequest data = JsonUtil.fromMap(request.getData(), UserTargetRequest.class);
        ServiceUtil.validateRequestData(data);

        String username = data.getUsername();
        ValidationUtil.validateUsername(username);

        if (AuthService.BOOTSTRAP_ADMIN_USERNAME.equals(username))
            throw new AuthException("Cannot manage the bootstrap admin account");
        return ServiceUtil.getOrLoadUser(username);
    }

    // dùng để ngắt kết nối người dùng
    private void disconnectUser(String username, String message) {
        ClientHandler clientHandler = RealtimeDatabase.getUserClient(username);
        // dùng để xóa sạch active phiên làm việc
        clearActiveSession(username);
        if (clientHandler != null) {
            clientHandler.sendEvent(new Event(EventType.SERVER_NOTICE, message));
            clientHandler.setCurrentUsername(null);
            clientHandler.closeConnection();
        }
    }

    // dùng để force đăng xuất người dùng
    private void forceLogoutUser(String username, String message) {
        ClientHandler clientHandler = RealtimeDatabase.getUserClient(username);
        // dùng để xóa sạch active phiên làm việc
        clearActiveSession(username);
        if (clientHandler != null) {
            clientHandler.sendEvent(new Event(EventType.FORCED_LOGOUT, message));
            clientHandler.setCurrentUsername(null);
        }
    }

    // dùng để xóa sạch active phiên làm việc
    private void clearActiveSession(String username) {
        User activeUser = RealtimeDatabase.getActiveUser(username);
        if (activeUser != null)
            userDao.save(activeUser, false);

        List<String> affectedAuctionIds = RealtimeDatabase.removeActiveUser(username);
        for (String auctionId : affectedAuctionIds)
            AuctionService.getInstance().publishLiveAudienceUpdate(auctionId);
    }

    // dùng để xóa đấu giá cascade
    private void deleteAuctionCascade(Auction auction) {
        if (auction == null)
            return;
        auctionService.deleteAuctionCascade(auction, false);
    }

    // dùng để xóa sản phẩm cascade
    private void deleteItemCascade(Item item) {
        if (item == null)
            return;

        for (String imageId : itemDao.findImageIdsByItemId(item.getId())) {
            Image image = imageDao.findById(imageId);
            if (image != null)
                imageService.deleteImageFile(image.getFilePath());
            imageDao.deleteById(imageId);
        }

        itemDao.deleteItemImageLinks(item.getId());
        itemDao.deleteById(item.getId());
    }
}
