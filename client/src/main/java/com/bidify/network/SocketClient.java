package com.bidify.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

// SocketClient quản lý kết nối SSL/TLS từ client đến server, hỗ trợ giao tiếp đồng bộ và bất đồng bộ.
public class SocketClient {
    private static final Logger logger = LoggerFactory.getLogger(SocketClient.class);
    private static SocketClient client = new SocketClient();
    private final ClientSession clientSession = ClientSession.getInstance();

    private final Object connectionLock = new Object();

    private volatile SSLSocket socket;
    private volatile BufferedReader in;
    private volatile PrintWriter out;
    private volatile Thread listenerThread;
    private volatile boolean closing;

    private final Map<String, BlockingQueue<Response>> pendingResponses = new ConcurrentHashMap<>();

    private static final String TRUSTSTORE_PATH = "/truststore/client-truststore.jks";
    private static final char[] TRUSTSTORE_PASSWORD = "blablablabidifyclient".toCharArray();

    protected SocketClient() {}

    public static SocketClient getClient() {
        return client;
    }

    public String getCurrentUsername() {
        return clientSession.getCurrentUsername();
    }

    public ClientSession getClientSession() {
        return clientSession;
    }

    public void setCurrentUsername(String currentUsername) {
        clientSession.setCurrentUsername(currentUsername);
    }

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

    public Response send(Request request) throws IOException {
        BlockingQueue<Response> queue = new ArrayBlockingQueue<>(1);

        synchronized (connectionLock) {
            if (listenerThread == null || !listenerThread.isAlive() || out == null)
                throw new IOException("Client has not started listening");
    
            pendingResponses.put(request.getId(), queue);
        }

        try {
            synchronized (connectionLock) {
                out.println(JsonUtil.toJson(request));
            }
            Response response = queue.poll(120, TimeUnit.SECONDS);
            if (response == null) throw new IOException("Request timed out");
            return response;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for response", e);
        }
        finally {
            pendingResponses.remove(request.getId());
        }
    }

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

    // Khởi tạo SSLSocketFactory sử dụng TrustStore đã đóng gói để xác thực Server.
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
