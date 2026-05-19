package com.bidify.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.BidDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.network.SocketClient;

public class BidClientService {
    private final SocketClient client = SocketClient.getClient();

    // dùng để lấy lượt đặt giá lịch sử
    public List<BidDto> getBidHistory() throws IOException {
        Response response = client.send(new Request(RequestType.GET_BID_HISTORY, null));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new ValidationException(
                response.getMessage() == null ? "Cannot load bid history." : response.getMessage()
            );
        }

        List<?> rawItems = JsonUtil.fromMap(response.getData(), List.class);
        List<BidDto> bids = new ArrayList<>();
        if (rawItems == null)
            return bids;

        for (Object rawItem : rawItems) {
            BidDto dto = JsonUtil.fromMap(rawItem, BidDto.class);
            if (dto != null)
                bids.add(dto);
        }

        return bids;
    }
}
