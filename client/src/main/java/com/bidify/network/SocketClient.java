package com.bidify.network;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.bidify.common.model.Event;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.utility.JsonUtil;
import com.bidify.event.EventManager;
import com.bidify.model.ClientSession;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/*
kết nối client và server
gọi connect() ở MainApp để nối server và client
SocketClient client = SocketClient.getClient(); để lấy client
Response response = client.send(request) để gửi request đến server và nhận về response
*/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketClient {
    private static final Logger logger = LoggerFactory.getLogger(SocketClient.class);
    private static SocketClient client = new SocketClient(); // singleton
    private final ClientSession clientSession = ClientSession.getInstance(); // session hiện tại

    private final Object connectionLock = new Object(); // khóa method (connect, close, send)

    private volatile SSLSocket socket;
    private volatile BufferedReader in; // nhận data từ server
    private volatile PrintWriter out; // gửi data đến server
    private volatile Thread listenerThread; // lắng nghe server
    private volatile boolean closing;

    private final Map<String, BlockingQueue<Response>> pendingResponses = new ConcurrentHashMap<>();

    private static final String TRUSTSTORE_PATH = "/truststore/client-truststore.jks";
    private static final char[] TRUSTSTORE_PASSWORD = "blablablabidifyclient".toCharArray();

    private SocketClient() {} // tránh tạo object từ ngoài -> singleton

    // kết nối đến server
    public void connect(String host, int port) throws IOException {
        synchronized (connectionLock) {
            if (socket != null) {
                logger.debug("Already connected to server");
                return;
            }
            try {
                closing = false;
                SSLSocketFactory factory = createSocketFactory();
                socket = (SSLSocket) factory.createSocket(host, port);
                socket.startHandshake();

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                startListening();
            }
            catch (ConnectException e) {
                throw new IOException("Server has not started", e);
            }
            catch (IOException e) {
                logger.error("Exception occurred", e);
                close();
            }
            catch (Exception e) {
                logger.error("Failed to initialize TLS", e);
                close();
            }
        }
    }

    public static SocketClient getClient() { // lấy client
        return client;
    }

    public String getCurrentUsername() { // lấy username hiện tại
        return clientSession.getCurrentUsername();
    }

    public ClientSession getClientSession() { // lấy session hiện tại
        return clientSession;
    }

    public void setCurrentUsername(String currentUsername) { // set username hiện tại
        clientSession.setCurrentUsername(currentUsername);
    }

    // gửi request đến server và nhận về response
    public Response send(Request request) throws IOException {
        BlockingQueue<Response> queue = new ArrayBlockingQueue<>(1);

        synchronized (connectionLock) {
            if (listenerThread == null || !listenerThread.isAlive() || out == null)
                throw new IOException("Client has not started listening");
    
            pendingResponses.put(request.getId(), queue);
            out.println(JsonUtil.toJson(request));
        }

        try {
            Response response = queue.poll(120, TimeUnit.SECONDS);
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
                while (!closing && socket != null && in != null && (line = in.readLine()) != null) {
                    JsonObject json = JsonParser.parseString(line).getAsJsonObject();

                    if (json.has("status")) {
                        Response response = JsonUtil.fromJson(line, Response.class);
                        BlockingQueue<Response> queue = pendingResponses.remove(response.getId());

                        if (queue != null) queue.offer(response);
                    }
                    else if (json.has("type")) {
                        Event event = JsonUtil.fromJson(line, Event.class);
                        logger.debug("Received: {}", event.getType());
                        Platform.runLater(() -> EventManager.getInstance().publish(event));
                    }
                }
            }
            catch (SocketException e) {
                if (!closing) logger.warn("Exception occurred", e);
            }
            catch (IOException e) {
                if (!closing) logger.warn("Exception occurred", e);
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    // đóng kết nối
    public void close() throws IOException {
        synchronized (connectionLock) {
            closing = true;
            if (listenerThread != null) listenerThread.interrupt();
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
            socket = null;
            in = null;
            out = null;
            listenerThread = null;
            clientSession.clear();
            pendingResponses.clear();
            logger.info("Disconnected from server");
            Platform.exit();
        }
    }

    private SSLSocketFactory createSocketFactory() throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");

        try (InputStream in = SocketClient.class.getResourceAsStream(TRUSTSTORE_PATH)) {
            if (in == null) throw new FileNotFoundException("Missing resource: " + TRUSTSTORE_PATH);
            trustStore.load(in, TRUSTSTORE_PASSWORD);
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), new SecureRandom());
        return context.getSocketFactory();
    }
}
