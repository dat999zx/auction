package com.bidify.server.utility;

import com.bidify.common.enums.RequestStatus;
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

import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

public class ServiceUtil {
    private static final UserDao userDao = UserDao.getInstance();
    private static final AuctionDao auctionDao = AuctionDao.getInstance();

    private ServiceUtil() {}

    // gọi cái này để đỡ phải try-catch
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

    // load user từ RAM nếu có, ko thì load từ database và tính lockedBalance
    public static User getOrLoadUser(String username) {
        ValidationUtil.validateUsername(username);
        
        User user = RealtimeDatabase.getActiveUser(username);
        if (user != null) return user;

        user = userDao.findByUsername(username);
        if (user == null) throw new AuthException("User not found: " + username);

        double lockedBalance = auctionDao.sumWinningBidsForUser(username);
        user.getWallet().setlockedBalance(lockedBalance);

        return user;
    }

    // kiểm tra session hợp lệ
    public static void requireSession(ClientHandler client) {
        if (client == null || !client.isInSession())
            throw new ValidationException("Invalid session");
    }

    // kiểm tra request data
    public static void validateRequestData(Object data) {
        if (data == null)
            throw new ValidationException("Invalid request");
    }
}
