package com.bidify.server.service;

import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.TransactionDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.Response;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dispatcher.RequestDispatcher;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Transaction;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;

public class TransactionService {
    private static TransactionService instance = new TransactionService();
    private final TransactionDao transactionDao = TransactionDao.getInstance();

    private TransactionService() {}

    public static TransactionService getInstance() { return instance; }

    public void initialize() {
        RequestDispatcher router = RequestDispatcher.getInstance();
        router.register(RequestType.GET_TRANSACTION_HISTORY, (client, req) -> getUserTransactions(client));
    }

    public Response getUserTransactions(ClientHandler client) {
        return ServiceUtil.handleRequest(() -> {
            String username = client.getCurrentUsername();
            if (!client.isInSession() || username == null)
                return new Response(RequestStatus.UNAUTHORIZED, "Unauthorized");

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
