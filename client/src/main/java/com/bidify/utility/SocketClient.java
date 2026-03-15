package com.bidify.utility;

import java.net.ConnectException;
import java.net.Socket;
import java.io.*;

import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.google.gson.Gson;

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
    private Gson gson = new Gson();

    public void connect(String host, int port) throws IOException{ // kết nối với máy chủ (chỉ gọi MỘT lần ở MainApp)
        try{
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }
        catch (ConnectException e){
            System.out.println("Server has not started");
            Platform.exit();
        }
    }

    public static SocketClient getClient(){ // lấy client để thực hiện giao tiếp với server
        if (client == null) client = new SocketClient();
        return client;
    }

    public Response send(Request request) throws IOException{ // gửi Request cho server và nhận về Response
        String json = gson.toJson(request);
        out.println(json);
        String responseJson = in.readLine();
        return gson.fromJson(responseJson, Response.class);
    }

    public void close() throws IOException{ // ngắt kết nối
        socket.close();
    }
}