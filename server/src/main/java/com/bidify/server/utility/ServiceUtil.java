package com.bidify.server.utility;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.*;
import com.bidify.common.model.Response;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.WalletRequestDao;
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

    // dùng để tạo một đối tượng ServiceUtil
    private ServiceUtil() {}

    // dùng cái này đỡ phải try-catch
    // dùng để bọc các hành động xử lý request nhằm tự động bắt các lỗi và trả về Response thất bại
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
    // dùng để tải thông tin của người dùng từ bộ nhớ tạm hoặc cơ sở dữ liệu SQLite kèm tính toán lockedBalance
    public static User getOrLoadUser(String username) {
        ValidationUtil.validateUsername(username);

        User user = RealtimeDatabase.getActiveUser(username);
        if (user != null) return user;

        user = userDao.findByUsername(username);
        if (user == null) throw new AuthException("User not found: " + username);

        double lockedBalance = auctionDao.sumWinningBidsForUser(username) + WalletRequestDao.getInstance().sumPendingWithdrawsForUser(username);
        user.getWallet().setlockedBalance(lockedBalance);

        return user;
    }

    // kiểm tra session hoạt động
    // dùng để bắt buộc client phải đang ở trong phiên đăng nhập hợp lệ
    public static void requireSession(ClientHandler client) {
        if (client == null || !client.isInSession())
            throw new ValidationException("Invalid session");
    }

    // dùng để lấy thông tin người dùng hiện tại dựa trên ClientHandler
    public static User requireSessionUser(ClientHandler client) {
        // dùng để bắt buộc phải có phiên làm việc
        requireSession(client);
        return getOrLoadUser(client.getCurrentUsername());
    }

    // dùng để kiểm tra xem một đối tượng người dùng có quyền admin hay không
    public static boolean isAdmin(User user) {
        return user != null && user.isAdmin();
    }

    // dùng để kiểm tra xem tên đăng nhập chỉ định có phải là tài khoản admin hay không
    public static boolean isAdminUsername(String username) {
        return isAdmin(getOrLoadUser(username));
    }

    // dùng để bắt buộc client hiện tại phải có quyền admin
    public static void requireAdmin(ClientHandler client) {
        if (!isAdmin(requireSessionUser(client)))
            throw new AuthException("Admin permission required");
    }

    // dùng để bắt buộc client hiện tại phải là tài khoản admin khởi tạo (bootstrap admin)
    public static void requireBootstrapAdmin(ClientHandler client) {
        User user = requireSessionUser(client);
        if (!AuthService.isBootstrapAdminUsername(user.getUsername()))
            throw new AuthException("Bootstrap admin permission required");
    }

    // dùng để bắt buộc người dùng không được phép có quyền admin
    public static void requireNonAdmin(User user, String message) {
        if (isAdmin(user))
            throw new AuthException(message);
    }

    // dùng để bắt buộc người dùng phải là tài khoản thông thường (không phải admin)
    public static void requireUserRole(User user, String message) {
        if (isAdmin(user))
            throw new AuthException(message);
    }

    // dùng để kiểm tra xem người dùng hiện tại có phải là chủ sở hữu hoặc là admin hay không
    public static boolean isOwnerOrAdmin(String ownerUsername, String currentUsername) {
        ValidationUtil.validateUsername(ownerUsername);
        ValidationUtil.validateUsername(currentUsername);
        return ownerUsername.equals(currentUsername) || isAdminUsername(currentUsername);
    }

    // kiểm tra request data hợp lệ
    // dùng để kiểm tra dữ liệu yêu cầu gửi lên từ client có khác null hay không
    public static void validateRequestData(Object data) {
        if (data == null)
            throw new ValidationException("Invalid request");
    }
}
