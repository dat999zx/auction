package com.bidify.server.contract;

import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.network.ClientHandler;

public interface Dispatcher {
    public Response dispatch(ClientHandler client, Request request);
}
