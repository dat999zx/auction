package com.bidify.server;

import com.bidify.server.network.ClientHandler;
import com.bidify.server.service.AuctionSchedulerService;
import com.bidify.server.service.AuctionService;
import com.bidify.server.service.AuthService;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.database.RealtimeDatabase;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.*;

public class ServerApp {
    private static final int PORT = 5000;
    private static final String KEYSTORE_PATH = "/keystore/server.jks";
    private static final char[] KEYSTORE_PASSWORD = "blablablabidifyserver".toCharArray();

    public static void main(String[] args) {
        System.out.println("Server is starting...");
        
        try {
            SQLiteHelper.init();
        }
        catch (DatabaseException e) {
            e.printStackTrace();
            System.exit(1);
        }
        AuctionService auctionService = new AuctionService();
        AuctionSchedulerService auctionSchedulerService = new AuctionSchedulerService();

        auctionService.loadToRuntime();
        auctionSchedulerService.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server is shutting down, saving all data...");
            new AuthService().saveAllUsers();
            auctionSchedulerService.stop();
            auctionService.saveAllRuntimeAuctions();
            RealtimeDatabase.clearAll();
        }));

        try {
            SSLContext sslContext = createServerSslContext();
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();

            try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(PORT)) {
                System.out.println("Server running on port: " + PORT);

                while (true) {
                    SSLSocket socket = (SSLSocket) serverSocket.accept();
                    System.out.println("Client connected: " + socket.getInetAddress());

                    new Thread(new ClientHandler(socket)).start();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static SSLContext createServerSslContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream in = ServerApp.class.getResourceAsStream(KEYSTORE_PATH)) {
            if (in == null) throw new Exception("Missing resource: " + KEYSTORE_PATH);
            keyStore.load(in, KEYSTORE_PASSWORD);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), null, new SecureRandom());
        return context;
    }
}
