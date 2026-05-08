package com.bidify.server.service;

import java.util.function.Supplier;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.UpdateProfileRequest;
import com.bidify.common.model.WalletRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.model.Wallet;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;
import com.bidify.server.utility.UserMapper;

public class UserProfileService {
    private static UserProfileService instance = new UserProfileService();
    private final UserDao userDao = UserDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();

    private UserProfileService() {}

    public static UserProfileService getInstance() { return instance; }

    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.GET_PROFILE, (client, req) -> getProfile(client));
        router.register(RequestType.UPDATE_PROFILE, this::updateProfile);
        router.register(RequestType.DEPOSIT, this::deposit);
        router.register(RequestType.WITHDRAW, this::withdraw);
    }

    public Response getProfile(ClientHandler client) {
        return ServiceUtil.handleRequest(() -> {
            User user = requireActiveUser(client);
            return new Response(RequestStatus.SUCCESS, "Profile loaded successfully", UserMapper.toDto(user));
        });
    }

    public Response updateProfile(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            UpdateProfileRequest data = JsonUtil.fromMap(request.getData(), UpdateProfileRequest.class);
            if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");

            User user = requireActiveUser(client);

            boolean hasChange = false;

            String nickname = data.getNickname();
            if (nickname != null) {
                ValidationUtil.validateNickname(nickname);
                user.setNickname(nickname.trim());
                hasChange = true;
            }

            if (!hasChange) {
                return new Response(RequestStatus.INVALID_REQUEST, "No profile changes were provided");
            }

            userDao.save(user, false);
            return new Response(RequestStatus.SUCCESS, "Profile updated successfully", UserMapper.toDto(user));
        });
    }

    public Response deposit(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User user = requireActiveUser(client);
            WalletRequest data = JsonUtil.fromMap(request.getData(), WalletRequest.class);
            if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");

            double amount = data.getAmount();
            ValidationUtil.validatePositiveAmount(amount, "Deposit amount");

            user.getWallet().deposit(amount);
            userDao.save(user, false);

            transactionDao.create(new Transaction(user.getUsername(), TransactionType.DEPOSIT, amount));

            return new Response(RequestStatus.SUCCESS, "Deposit successful", UserMapper.toDto(user));
        });
    }

    public Response withdraw(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User user = requireActiveUser(client);
            WalletRequest data = JsonUtil.fromMap(request.getData(), WalletRequest.class);
            if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");

            double amount = data.getAmount();
            ValidationUtil.validatePositiveAmount(amount, "Withdraw amount");

            Wallet wallet = user.getWallet();

            if (wallet.getAvailableBalance() < amount)
                throw new ValidationException("Insufficient available balance");

            wallet.withdraw(amount);
            userDao.save(user, false);

            transactionDao.create(new Transaction(user.getUsername(), TransactionType.WITHDRAW, amount));

            return new Response(RequestStatus.SUCCESS, "Withdraw successful", UserMapper.toDto(user));
        });
    }

    private User requireActiveUser(ClientHandler client) {
        if (client == null || !client.isInSession()) {
            throw new ValidationException("Invalid session");
        }

        String username = client.getCurrentUsername();
        User user = RealtimeDatabase.getActiveUser(username);
        if (user != null) {
            return user;
        }

        user = userDao.findByUsername(username);
        if (user == null) {
            throw new ValidationException("User not found");
        }
        return user;
    }
}
