package com.bidify.server.service;

import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.TransactionDto;
import com.bidify.common.dto.UserDto;
import com.bidify.common.dto.WalletRequestDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.enums.UserRole;
import com.bidify.common.model.Event;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.dao.WalletRequestDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.exception.InsufficientBalanceException;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.model.Wallet;
import com.bidify.server.model.WalletRequest;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;
import com.bidify.server.utility.UserMapper;

public class TransactionService {
    private static TransactionService instance = new TransactionService();
    private final TransactionDao transactionDao = TransactionDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    private final WalletRequestDao walletRequestDao = WalletRequestDao.getInstance();

    private TransactionService() {}

    public static TransactionService getInstance() { return instance; }

    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.GET_TRANSACTION_HISTORY, (client, req) -> getUserTransactions(client));
        router.register(RequestType.GET_WALLET_REQUEST_HISTORY, (client, req) -> getUserWalletRequests(client));
        router.register(RequestType.DEPOSIT, this::deposit);
        router.register(RequestType.WITHDRAW, this::withdraw);
    }

    // Yêu cầu nạp tiền vào ví.
    public Response deposit(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            com.bidify.common.model.WalletRequest data = JsonUtil.fromMap(request.getData(), com.bidify.common.model.WalletRequest.class);
            ServiceUtil.validateRequestData(data);
            User user = ServiceUtil.requireSessionUser(client);
            ServiceUtil.requireUserRole(user, "Admin accounts cannot deposit");

            double amount = data.getAmount();
            ValidationUtil.validatePositiveAmount(amount, "Deposit amount");

            WalletRequest walletRequest = new WalletRequest(user.getUsername(), TransactionType.DEPOSIT, amount);
            walletRequestDao.create(walletRequest);

            UserDto userDto = UserMapper.toDto(user);
            publishWalletRequestsChanged(client);

            return new Response(RequestStatus.SUCCESS, "Deposit request created successfully", userDto);
        });
    }

    // Yêu cầu rút tiền khỏi ví. Số tiền rút tạm thời bị giữ (lock) chờ duyệt.
    public Response withdraw(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User user = ServiceUtil.requireSessionUser(client);
            ServiceUtil.requireUserRole(user, "Admin accounts cannot withdraw");
            com.bidify.common.model.WalletRequest data = JsonUtil.fromMap(request.getData(), com.bidify.common.model.WalletRequest.class);
            ServiceUtil.validateRequestData(data);

            double amount = data.getAmount();
            ValidationUtil.validatePositiveAmount(amount, "Withdraw amount");

            user.lock();
            try {
                Wallet wallet = user.getWallet();

                if (wallet.getAvailableBalance() < amount)
                    throw new InsufficientBalanceException("Insufficient available balance");

                wallet.lockBalance(amount);
                userDao.save(user, false);

                WalletRequest walletRequest = new WalletRequest(user.getUsername(), TransactionType.WITHDRAW, amount);
                walletRequestDao.create(walletRequest);
            } finally {
                user.unlock();
            }

            UserDto userDto = UserMapper.toDto(user);
            publishWalletChanged(client, userDto);
            publishWalletRequestsChanged(client);

            return new Response(RequestStatus.SUCCESS, "Withdraw request created successfully", userDto);
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

    public Response getUserWalletRequests(ClientHandler client) {
        return ServiceUtil.handleRequest(() -> {
            String username = client.getCurrentUsername();
            ServiceUtil.requireSession(client);

            List<WalletRequest> requests = walletRequestDao.findByUsername(username);
            List<WalletRequestDto> dtos = new ArrayList<>();

            for (WalletRequest r : requests) {
                dtos.add(new WalletRequestDto(
                        r.getId(),
                        r.getCreatedAt().toString(),
                        r.getReviewedAt() != null ? r.getReviewedAt().toString() : null,
                        r.getUsername(),
                        r.getType(),
                        r.getAmount(),
                        r.getStatus(),
                        r.getReviewedBy()
                ));
            }

            return new Response(RequestStatus.SUCCESS, "Wallet request history loaded", dtos);
        });
    }

    private void publishWalletChanged(ClientHandler client, UserDto userDto) {
        if (client == null || userDto == null) return;
        client.sendEvent(new Event(EventType.WALLET_CHANGED, "Wallet changed", userDto));
    }

    // Gửi sự kiện cập nhật danh sách yêu cầu ví đến toàn bộ admin trực tuyến.
    private void publishWalletRequestsChanged(ClientHandler client) {
        if (client == null) return;
        // Only notify admin clients — they manage the approval queue.
        Event event = new Event(EventType.WALLET_REQUESTS_CHANGED, "Wallet requests changed", null);
        for (ClientHandler c : RealtimeDatabase.getAllUserClients()) {
            User u = RealtimeDatabase.getActiveUser(c.getCurrentUsername());
            if (u != null && u.getRole() == UserRole.ADMIN) {
                c.sendEvent(event);
            }
        }
    }
}
