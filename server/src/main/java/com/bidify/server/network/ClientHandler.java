package com.bidify.server.network;

import com.bidify.common.enums.RequestType;
import com.bidify.common.model.Event;
import com.bidify.common.model.LogoutRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.server.contract.Observer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.SocketException;

import javax.net.ssl.SSLSocket;

// lắng nghe client
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler implements Runnable, Observer {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final SSLSocket socket;
    private final RequestDispatcher dispatcher = new RequestDispatcher();
    private String currentUsername;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(SSLSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            
            String line;
            while ((line = in.readLine()) != null) {
                Request request = JsonUtil.fromJson(line, Request.class);
                Response response = dispatcher.dispatch(this, request);
                response.setId(request.getId());
                sendResponse(response);
            }
        } catch (SocketException e2) {
            logger.info("Client disconnected: {}", socket.getInetAddress());
        } catch (IOException e2) {
            logger.warn("Exception occurred", e2);
        } finally {
            handleDisconnect();
        }
    }

    public void sendResponse(Response response) { // gửi response đến client
        if (out != null)
            out.println(JsonUtil.toJson(response));
    }

    public void sendEvent(Event event) { // gửi event đến client
        if (out != null)
            out.println(JsonUtil.toJson(event));
    }

    @Override
    public void onEvent(Event event) {
        sendEvent(event);
    }

    public void setCurrentUsername(String username) { // thiết lập username của client
        this.currentUsername = username;
    }

    public String getCurrentUsername() { // lấy username của client
        return currentUsername;
    }

    public boolean isInSession(){ // xác thực client đã đăng nhập chưa
        return socket != null && currentUsername != null;
    }

    private void handleDisconnect() { // xử lý khi client ngắt kết nối
        logger.info("Client disconnected: {}", socket.getInetAddress());
        if (currentUsername == null) return;
        Request request = new Request(RequestType.LOGOUT, new LogoutRequest());
        dispatcher.dispatch(this, request);
    }
}
