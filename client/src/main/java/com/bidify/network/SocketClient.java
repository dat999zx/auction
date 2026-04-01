package com.bidify.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.bidify.common.model.Event;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.util.JsonUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;

/*
kết nối client và server
gọi connect() ở MainApp để nối server và client
SocketClient client = SocketClient.getClient(); để lấy client
Response response = client.send(request) để gửi request đến server và nhận về response
*/
public class SocketClient {
    private static SocketClient client;

    private String currentUsername;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;

    private final Map<String, BlockingQueue<Response>> pendingResponses = new ConcurrentHashMap<>();

    private SocketClient(){} // tránh tạo object từ ngoài -> singleton

    // kết nối đến server
    public void connect(String host, int port) throws IOException {
        if (socket != null) {
            System.out.println("Already connected to server");
            return;
        }
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            startListening();
        }
        catch (ConnectException e) {
            System.out.println("Server has not started");
            Platform.exit();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static SocketClient getClient() { // lấy client
        if (client == null) client = new SocketClient();
        return client;
    }

    public String getCurrentUsername() { // lấy username hiện tại
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) { // set username hiện tại
        this.currentUsername = currentUsername;
    }

    // gửi request đến server và nhận về response
    public Response send(Request request) throws IOException {
        if (listenerThread == null || !listenerThread.isAlive() || out == null)
            throw new IOException("Client has not started listening");

        BlockingQueue<Response> queue = new ArrayBlockingQueue<>(1);
        pendingResponses.put(request.getId(), queue);

        out.println(JsonUtil.toJson(request));
        try {
            Response response = queue.poll(30, TimeUnit.SECONDS);
            pendingResponses.remove(request.getId());
            if (response == null) throw new IOException("Request timed out");
            return response;
        }
        catch (InterruptedException e) {
            pendingResponses.remove(request.getId());
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for response", e);
        }
    }

    // bắt đầu lắng nghe server
    public void startListening() {
        if ((listenerThread != null && listenerThread.isAlive()) || socket == null || in == null) return;

        listenerThread = new Thread(() -> {
            try {
                String line;
                while (socket != null && in != null && (line = in.readLine()) != null) {
                    JsonObject json = JsonParser.parseString(line).getAsJsonObject();

                    if (json.has("status")) {
                        Response response = JsonUtil.fromJson(line, Response.class);
                        BlockingQueue<Response> queue = pendingResponses.remove(response.getId());

                        if (queue != null) {
                            queue.offer(response);
                        }
                    } else if (json.has("type")) {
                        Event event = JsonUtil.fromJson(line, Event.class);
                        System.out.println(event.getMessage());
                    }
                }
            }
            catch (SocketException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // đóng kết nối
    public void close() throws IOException {
        if (socket != null) socket.close();
        if (listenerThread != null) listenerThread.interrupt();
        if (in != null) in.close();
        if (out != null) out.close();
        socket = null;
        in = null;
        out = null;
        listenerThread = null;
        currentUsername = null;
    }
}
