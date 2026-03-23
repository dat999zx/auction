package com.bidify.network;

import java.net.ConnectException;
import java.net.Socket;
import java.io.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import com.bidify.common.model.Message;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.util.JsonUtil;

import javafx.application.Platform;

/*
kết nối client và server
gọi connect() ở MainApp để nối server và client
SocketClient client = SocketClient.getClient(); để lấy client
Response response = client.send(request) để gửi request đến server và nhận về response
*/
public class SocketClient {
    private static SocketClient client;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public void connect(String host, int port) throws IOException { // kết nối với máy chủ (chỉ gọi MỘT lần ở MainApp)
        if (socket != null) {
            System.out.println("Already connected to server");
            return;
        }
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (ConnectException e) {
            System.out.println("Server has not started");
            Platform.exit();
        }
    }

    public static SocketClient getClient() { // lấy client để thực hiện giao tiếp với server
        if (client == null)
            client = new SocketClient();
        return client;
    }

    public Response send(Request request) throws IOException { // gửi Request cho server và nhận về Response
        String json = JsonUtil.toJson(request);
        out.println(json);
        String responseJson = in.readLine();
        return JsonUtil.fromJson(responseJson, Response.class);
    }

    public void startListening() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    Message message = JsonUtil.fromJson(line, Message.class);
                    System.out.println("Message: " + message.getMessage());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    };

    public void close() throws IOException { // ngắt kết nối
        if (socket != null)
            socket.close();
    }
}