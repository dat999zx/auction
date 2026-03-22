package com.bidify.server.network;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.model.LoginRequest;
import com.bidify.common.model.LogoutRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.network.RequestDispatcher;
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

    public ClientHandler(Socket socket){ this.socket = socket; }

    @Override
    public void run(){ // chạy liên tục để nhận mọi request từ client (mỗi client là 1 thread)
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // dùng để nhận request
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // dùng để trả response
        ){
            String message;
            while ((message = in.readLine()) != null) { // liên tục đọc dữ liệu nhận từ client
                System.out.println("Received: " + message);
                
                Request request = JsonUtil.fromJson(message, Request.class);
                Response response = dispatcher.dispatch(this, request);
                out.println(JsonUtil.toJson(response));
            }
        }
        catch(SocketException e){ System.out.println("Client disconnected: " + socket.getInetAddress()); }
        catch(IOException e){ e.printStackTrace(); }
        finally{
            handleDisconnect();
        }
    }

    public void setCurrentUsername(String username){ this.currentUsername = username; }
    public String getCurrentUsername(){ return currentUsername; }

    private void handleDisconnect(){ // khi user mất kết nối
        if (currentUsername == null) return;
        Request request = new Request(RequestType.LOGOUT, new LogoutRequest(currentUsername));
        dispatcher.dispatch(this, request);
        System.out.println(currentUsername + " logged out");
    }
}