package com.bidify.server;

import com.bidify.server.network.ClientHandler;
import com.bidify.server.service.AuctionSchedulerService;
import com.bidify.server.service.AuctionService;
import com.bidify.server.service.AuthService;
import com.bidify.server.service.UserProfileService;
import com.bidify.server.service.BidService;
import com.bidify.server.service.TransactionService;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.database.RealtimeDatabase;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerApp {
    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);
    private static final int PORT = 5000;
    private static final String KEYSTORE_PATH = "/keystore/server.jks";
    private static final char[] KEYSTORE_PASSWORD = "blablablabidifyserver".toCharArray();

    public static void main(String[] args) {
        logger.info("Server is starting...");
        
        try {
            SQLiteHelper.init();
        }
        catch (DatabaseException e) {
            logger.error("Exception occurred", e);
            System.exit(1);
        }
        AuctionService auctionService = AuctionService.getInstance();
        AuctionSchedulerService auctionSchedulerService = AuctionSchedulerService.getInstance();
        
        AuthService.getInstance().initialize();
        auctionService.initialize();
        UserProfileService.getInstance().initialize();
        BidService.getInstance().initialize();
        TransactionService.getInstance().initialize();
        
        auctionService.loadToRuntime();
        auctionSchedulerService.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Server is shutting down, saving all data...");
            AuthService.getInstance().saveAllUsers();
            auctionSchedulerService.stop();
            auctionService.saveAllRuntimeAuctions();
            RealtimeDatabase.clearAll();
        }));

        try {
            SSLContext sslContext = createServerSslContext();
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();

            try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(PORT)) {
                logger.info("Server running on port: " + PORT);

                while (!serverSocket.isClosed()) {
                    SSLSocket socket = (SSLSocket) serverSocket.accept();
                    logger.info("Client connected: {}", socket.getInetAddress());

                    new Thread(new ClientHandler(socket), socket.getInetAddress().toString()).start();
                }
            }
        }
        catch (Exception e) {
            logger.error("Exception occurred", e);
            System.exit(1);
        }
    }

    private static SSLContext createServerSslContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (InputStream in = ServerApp.class.getResourceAsStream(KEYSTORE_PATH)) {
            if (in == null) throw new FileNotFoundException("Missing resource: " + KEYSTORE_PATH);
            keyStore.load(in, KEYSTORE_PASSWORD);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), null, new SecureRandom());
        return context;
    }
}
