package com.bidify.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.TransactionDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.network.SocketClient;

public class TransactionClientService {
    private final SocketClient client = SocketClient.getClient();

    // dùng để lấy giao dịch lịch sử
    public List<TransactionDto> getTransactionHistory() throws IOException {
        Response response = client.send(new Request(RequestType.GET_TRANSACTION_HISTORY, null));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new ValidationException(
                response.getMessage() == null ? "Cannot load transaction history." : response.getMessage()
            );
        }

        List<?> rawItems = JsonUtil.fromMap(response.getData(), List.class);
        List<TransactionDto> transactions = new ArrayList<>();
        if (rawItems == null)
            return transactions;

        for (Object rawItem : rawItems) {
            TransactionDto dto = JsonUtil.fromMap(rawItem, TransactionDto.class);
            if (dto != null)
                transactions.add(dto);
        }

        return transactions;
    }
}
