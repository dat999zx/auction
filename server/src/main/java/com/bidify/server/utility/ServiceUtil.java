package com.bidify.server.utility;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.UserRole;
import com.bidify.common.exception.*;
import com.bidify.common.model.Response;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.exception.InsufficientBalanceException;
import com.bidify.server.exception.ServerTimeOutException;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.service.AuthService;

import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

public class ServiceUtil {
    private static final UserDao userDao = UserDao.getInstance();
    private static final AuctionDao auctionDao = AuctionDao.getInstance();

    private ServiceUtil() {}

    // dùng cái này đỡ phải try-catch
    public static Response handleRequest(Supplier<Response> action) {
        try {
            return action.get();
        }
        catch (DateTimeParseException e) {
            return new Response(RequestStatus.FAILED, "Invalid date time format");
        }
        catch (ValidationException | AuthException | AuctionException | BidException |
               InsufficientBalanceException | ServerTimeOutException | DatabaseException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }
        catch (Exception e) {
            return new Response(RequestStatus.FAILED, "Internal server error: " + e.getMessage());
        }
    }

    // load user từ RealtimeDatabase hoặc SQL
    public static User getOrLoadUser(String username) {
        ValidationUtil.validateUsername(username);

        if (AuthService.isBootstrapAdminUsername(username))
            return AuthService.createBootstrapAdminUser();
        
        User user = RealtimeDatabase.getActiveUser(username);
        if (user != null) return user;

        user = userDao.findByUsername(username);
        if (user == null) throw new AuthException("User not found: " + username);

        double lockedBalance = auctionDao.sumWinningBidsForUser(username);
        user.getWallet().setlockedBalance(lockedBalance);

        return user;
    }

    // kiểm tra session hoạt động
    public static void requireSession(ClientHandler client) {
        if (client == null || !client.isInSession())
            throw new ValidationException("Invalid session");
    }

    public static User requireSessionUser(ClientHandler client) {
        requireSession(client);
        return getOrLoadUser(client.getCurrentUsername());
    }

    public static boolean isAdmin(User user) {
        return user != null && user.getRole() == UserRole.ADMIN;
    }

    public static boolean isAdminUsername(String username) {
        return isAdmin(getOrLoadUser(username));
    }

    public static void requireAdmin(ClientHandler client) {
        if (!isAdmin(requireSessionUser(client)))
            throw new AuthException("Admin permission required");
    }

    public static void requireBootstrapAdmin(ClientHandler client) {
        User user = requireSessionUser(client);
        if (!AuthService.isBootstrapAdminUsername(user.getUsername()))
            throw new AuthException("Bootstrap admin permission required");
    }

    public static void requireNonAdmin(User user, String message) {
        if (isAdmin(user))
            throw new AuthException(message);
    }

    public static void requireUserRole(User user, String message) {
        if (isAdmin(user))
            throw new AuthException(message);
    }

    public static boolean isOwnerOrAdmin(String ownerUsername, String currentUsername) {
        ValidationUtil.validateUsername(ownerUsername);
        ValidationUtil.validateUsername(currentUsername);
        return ownerUsername.equals(currentUsername) || isAdminUsername(currentUsername);
    }

    // kiểm tra request data hợp lệ
    public static void validateRequestData(Object data) {
        if (data == null)
            throw new ValidationException("Invalid request");
    }
}
