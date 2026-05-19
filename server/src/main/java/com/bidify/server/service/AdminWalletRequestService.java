package com.bidify.server.service;

import com.bidify.common.dto.WalletRequestDto;
import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.enums.TransactionType;
import com.bidify.common.enums.UserRole;
import com.bidify.common.enums.WalletRequestStatus;
import com.bidify.common.model.Event;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.WalletReviewRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.dao.WalletRequestDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.model.Transaction;
import com.bidify.server.model.User;
import com.bidify.server.model.Wallet;
import com.bidify.server.model.WalletRequest;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;
import com.bidify.server.utility.UserMapper;

import java.util.ArrayList;
import java.util.List;

public class AdminWalletRequestService {
    private static final AdminWalletRequestService instance = new AdminWalletRequestService();

    private final WalletRequestDao walletRequestDao = WalletRequestDao.getInstance();
    private final UserDao userDao = UserDao.getInstance();
    private final TransactionDao transactionDao = TransactionDao.getInstance();

    private AdminWalletRequestService() {}

    public static AdminWalletRequestService getInstance() { return instance; }

    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.GET_PENDING_WALLET_REQUESTS, (client, req) -> getPendingRequests(client));
        router.register(RequestType.REVIEW_WALLET_REQUEST, this::reviewRequest);
    }

    public Response getPendingRequests(ClientHandler client) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);

            List<WalletRequest> pending = walletRequestDao.findPending();
            List<WalletRequestDto> dtos = new ArrayList<>();

            for (WalletRequest r : pending) {
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

            return new Response(RequestStatus.SUCCESS, "Pending requests loaded", dtos);
        });
    }

    public Response reviewRequest(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User admin = ServiceUtil.requireSessionUser(client);
            ServiceUtil.requireAdmin(client);

            WalletReviewRequest data = JsonUtil.fromMap(request.getData(), WalletReviewRequest.class);
            ServiceUtil.validateRequestData(data);

            WalletRequest walletRequest = walletRequestDao.findById(data.getWalletRequestId());
            if (walletRequest == null) {
                return new Response(RequestStatus.FAILED, "Wallet request not found");
            }
            if (walletRequest.getStatus() != WalletRequestStatus.PENDING) {
                return new Response(RequestStatus.FAILED, "Request already reviewed");
            }

            User targetUser = ServiceUtil.getOrLoadUser(walletRequest.getUsername());
            if (targetUser == null) {
                return new Response(RequestStatus.FAILED, "Target user not found");
            }

            targetUser.lock();
            try {
                Wallet wallet = targetUser.getWallet();

                if (data.isApproved()) {
                    if (walletRequest.getType() == TransactionType.DEPOSIT) {
                        wallet.deposit(walletRequest.getAmount());
                    } else if (walletRequest.getType() == TransactionType.WITHDRAW) {
                        wallet.unlockBalance(walletRequest.getAmount());
                        wallet.withdraw(walletRequest.getAmount());
                    }
                    userDao.save(targetUser, false);
                    transactionDao.create(new Transaction(targetUser.getUsername(), walletRequest.getType(), walletRequest.getAmount()));
                    walletRequest.approve(admin.getUsername());
                } else {
                    if (walletRequest.getType() == TransactionType.WITHDRAW) {
                        wallet.unlockBalance(walletRequest.getAmount());
                        userDao.save(targetUser, false);
                    }
                    walletRequest.deny(admin.getUsername());
                }

                walletRequestDao.update(walletRequest);
            } finally {
                targetUser.unlock();
            }

            // Publish events
            publishWalletRequestsChanged(client);
            publishUserRequestsChanged(walletRequest.getUsername());
            
            // Send wallet changed to target user if their wallet changed
            if (data.isApproved() || (!data.isApproved() && walletRequest.getType() == TransactionType.WITHDRAW)) {
                ClientHandler targetClient = RealtimeDatabase.getUserClient(targetUser.getUsername());
                if (targetClient != null) {
                    targetClient.sendEvent(new Event(EventType.WALLET_CHANGED, "Wallet changed", UserMapper.toDto(targetUser)));
                }
            }

            return new Response(RequestStatus.SUCCESS, "Request reviewed successfully");
        });
    }

    private void publishWalletRequestsChanged(ClientHandler client) {
        if (client == null) return;
        // Only send to admin clients, not every connected user.
        Event event = new Event(EventType.WALLET_REQUESTS_CHANGED, "Wallet requests changed", null);
        for (ClientHandler c : RealtimeDatabase.getAllUserClients()) {
            User u = RealtimeDatabase.getActiveUser(c.getCurrentUsername());
            if (u != null && u.getRole() == UserRole.ADMIN) {
                c.sendEvent(event);
            }
        }
    }

    /** Notify the target user that their own request list changed. */
    private void publishUserRequestsChanged(String username) {
        if (username == null) return;
        ClientHandler targetClient = RealtimeDatabase.getUserClient(username);
        if (targetClient != null) {
            targetClient.sendEvent(new Event(EventType.WALLET_REQUESTS_CHANGED, "Your wallet request was reviewed", null));
        }
    }
}
