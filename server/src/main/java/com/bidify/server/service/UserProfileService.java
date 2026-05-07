package com.bidify.server.service;

import com.bidify.common.dto.TransactionDto;
import com.bidify.common.dto.UserDto;
import com.bidify.common.enums.RequestStatus;
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
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.model.Wallet;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.UserMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class UserProfileService {
    private static UserProfileService instance = new UserProfileService();
    private final UserDao userDao = UserDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();

    private UserProfileService() {}

    public static UserProfileService getInstance() { return instance; }

    public Response getProfile(ClientHandler client) {
        return handleProfileRequest(() -> {
            User user = requireActiveUser(client);
            return new Response(RequestStatus.SUCCESS, "Profile loaded successfully", UserMapper.toDto(user));
        });
    }

    public Response updateProfile(ClientHandler client, Request request) {
        UpdateProfileRequest data = JsonUtil.fromMap(request.getData(), UpdateProfileRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");

        return handleProfileRequest(() -> {
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
        WalletRequest data = JsonUtil.fromMap(request.getData(), WalletRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");

        return handleProfileRequest(() -> {
            User user = requireActiveUser(client);
            double amount = data.getAmount();
            ValidationUtil.validatePositiveAmount(amount, "Deposit amount");

            user.getWallet().deposit(amount);
            userDao.save(user, false);

            transactionDao.create(new Transaction(user.getUsername(), TransactionType.DEPOSIT, amount));

            return new Response(RequestStatus.SUCCESS, "Deposit successful", UserMapper.toDto(user));
        });
    }

    public Response withdraw(ClientHandler client, Request request) {
        WalletRequest data = JsonUtil.fromMap(request.getData(), WalletRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");

        return handleProfileRequest(() -> {
            User user = requireActiveUser(client);
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

    public Response getTransactions(ClientHandler client) {
        return handleProfileRequest(() -> {
            User user = requireActiveUser(client);
            List<Transaction> transactions = transactionDao.findByUsername(user.getUsername());
            List<TransactionDto> dtos = new ArrayList<>();

            for (Transaction t : transactions) {
                dtos.add(new TransactionDto(
                        t.getId(),
                        t.getCreatedAt().toString(),
                        t.getUsername(),
                        t.getType(),
                        t.getAmount(),
                        t.getAuctionId()
                ));
            }

            return new Response(RequestStatus.SUCCESS, "Transaction history loaded", dtos);
        });
    }

    private Response handleProfileRequest(Supplier<Response> action) {
        try {
            return action.get();
        } catch (ValidationException | DatabaseException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }
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
