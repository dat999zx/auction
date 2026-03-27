package com.bidify.server.network;

import com.bidify.common.enums.EventType;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.Event;
import com.bidify.common.model.LogoutRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.util.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

// lắng nghe client
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final RequestDispatcher dispatcher = new RequestDispatcher();
    private String currentUsername;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
            
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);

                Request request = JsonUtil.fromJson(line, Request.class);
                Response response = dispatcher.dispatch(this, request);
                response.setId(request.getId());
                sendResponse(response);
            }
        } catch (SocketException e2) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } catch (IOException e2) {
            e2.printStackTrace();
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

    public void setCurrentUsername(String username) { // thiết lập username của client
        this.currentUsername = username;
    }

    public String getCurrentUsername() { // lấy username của client
        return currentUsername;
    }

    public boolean isValidClient(){ // xác thực client đã đăng nhập chưa
        return socket != null && currentUsername != null;
    }

    private void handleDisconnect() { // xử lý khi client ngắt kết nối
        if (currentUsername == null)
            return;
        Request request = new Request(RequestType.LOGOUT, new LogoutRequest(currentUsername));
        dispatcher.dispatch(this, request);
    }
}
