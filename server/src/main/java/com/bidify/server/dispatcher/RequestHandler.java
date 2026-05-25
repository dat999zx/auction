package com.bidify.server.dispatcher;

import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.network.ClientHandler;

@FunctionalInterface
public interface RequestHandler {
    Response handle(ClientHandler client, Request request);
}
