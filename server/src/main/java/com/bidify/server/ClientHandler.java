package com.bidify.server;

import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

// lắng nghe client
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Gson gson = new Gson();

    public ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run(){ // chạy liên tục để nhận mọi request từ client (mỗi client là 1 thread)
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // dùng để nhận request
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // dùng để trả response
        ){
            String message;
            while ((message = in.readLine()) != null) { // liên tục đọc dữ liệu nhận từ client
                System.out.println("Received: " + message);

                Request request;
                Response response;

                try{ request = gson.fromJson(message, Request.class); }
                catch (Exception e){
                    response = new Response("ERROR", "Invalid JSON");
                    out.println(gson.toJson(response));
                    continue;
                }

                if (request == null || request.getType() == null || request.getType().isBlank()){
                    response = new Response("ERROR", "Invalid request type");
                    out.println(gson.toJson(response));
                    continue;
                }

                switch (request.getType()){
                    case "REGISTER" -> {
                        response = new Response("SUCCESS", "register");
                    }
                    default -> {
                        response = new Response("ERROR", "Invalid request type");
                    }
                }

                out.println(gson.toJson(response));
            }
        }
        catch(SocketException e){ System.out.println("Client disconnected: " + socket.getInetAddress()); }
        catch(IOException e){ e.printStackTrace(); }
    }
}