package com.bidify.server.service;

import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.TransactionDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.WalletRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.exception.InsufficientBalanceException;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.model.Wallet;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;
import com.bidify.server.utility.UserMapper;

public class TransactionService {
    private static TransactionService instance = new TransactionService();
    private final TransactionDao transactionDao = TransactionDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();

    private TransactionService() {}

    public static TransactionService getInstance() { return instance; }

    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.GET_TRANSACTION_HISTORY, (client, req) -> getUserTransactions(client));
        router.register(RequestType.DEPOSIT, this::deposit);
        router.register(RequestType.WITHDRAW, this::withdraw);
    }

    public Response deposit(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            WalletRequest data = JsonUtil.fromMap(request.getData(), WalletRequest.class);
            ServiceUtil.validateRequestData(data);
            ServiceUtil.requireSession(client);

            User user = ServiceUtil.getOrLoadUser(client.getCurrentUsername());

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
            User user = ServiceUtil.getOrLoadUser(client.getCurrentUsername());
            WalletRequest data = JsonUtil.fromMap(request.getData(), WalletRequest.class);
            ServiceUtil.validateRequestData(data);

            double amount = data.getAmount();
            ValidationUtil.validatePositiveAmount(amount, "Withdraw amount");

            Wallet wallet = user.getWallet();

            if (wallet.getAvailableBalance() < amount)
                throw new InsufficientBalanceException("Insufficient available balance");

            wallet.withdraw(amount);
            userDao.save(user, false);

            transactionDao.create(new Transaction(user.getUsername(), TransactionType.WITHDRAW, amount));

            return new Response(RequestStatus.SUCCESS, "Withdraw successful", UserMapper.toDto(user));
        });
    }

    public Response getUserTransactions(ClientHandler client) {
        return ServiceUtil.handleRequest(() -> {
            String username = client.getCurrentUsername();
            ServiceUtil.requireSession(client);

            List<Transaction> transactions = transactionDao.findByUsername(username);
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
}
