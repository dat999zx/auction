package com.bidify.server;

import com.bidify.server.network.ClientHandler;
import com.bidify.server.service.AuctionSchedulerService;
import com.bidify.server.service.AuctionService;
import com.bidify.server.service.AdminService;
import com.bidify.server.service.AuthService;
import com.bidify.server.service.BidService;
import com.bidify.server.service.ItemService;
import com.bidify.server.service.TransactionService;
import com.bidify.server.service.UserProfileService;
import com.bidify.server.service.AdminWalletRequestService;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.database.RealtimeDatabase;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerApp {
    private static final Logger logger = LoggerFactory.getLogger(ServerApp.class);
    private static final int DEFAULT_PORT = 5000;
    private static final int MAX_CLIENT_THREADS = 32;
    private static final String KEYSTORE_PATH = "/keystore/server.jks";
    private static final char[] KEYSTORE_PASSWORD = "blablablabidifyserver".toCharArray();

    public static void main(String[] args) {
        logger.info("Server is starting...");
        
        try {
            SQLiteHelper.init();
        }
        catch (DatabaseException e) {
            logger.error("Database initialization failed", e);
            System.exit(1);
        }

        AuctionService auctionService = AuctionService.getInstance();
        AuctionSchedulerService auctionSchedulerService = AuctionSchedulerService.getInstance();
        
        AuthService.getInstance().initialize();
        AdminService.getInstance().initialize();
        auctionService.initialize();
        UserProfileService.getInstance().initialize();
        BidService.getInstance().initialize();
        ItemService.getInstance().initialize();
        TransactionService.getInstance().initialize();
        AdminWalletRequestService.getInstance().initialize();
        
        auctionService.loadToRuntime();
        auctionSchedulerService.start();

        // chứa các thread client handler
        ExecutorService clientThreadPool = Executors.newFixedThreadPool(MAX_CLIENT_THREADS);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Server is shutting down, saving all data...");
            AuthService.getInstance().saveAllUsers();
            auctionSchedulerService.stop();
            clientThreadPool.shutdownNow();
            auctionService.saveAllRuntimeAuctions();
            RealtimeDatabase.clearAll();
        }));

        try {
            SSLContext sslContext = createServerSslContext();
            SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
            int port = resolvePort();
 
            try (SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port)) {
                logger.info("Server running on port: {}", port);
 
                while (!serverSocket.isClosed()) {
                    SSLSocket socket = (SSLSocket) serverSocket.accept();
                    logger.info("Client connected: {}", socket.getInetAddress());
 
                    clientThreadPool.execute(new ClientHandler(socket));
                }
            }
        }
        catch (Exception e) {
            logger.error("Exception occurred", e);
            System.exit(1);
        }
    }
    
    // Khởi tạo SSLContext sử dụng Keystore cấu hình sẵn cho kết nối bảo mật SSL/TLS.
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
 
    // Đọc cổng cấu hình từ System properties hoặc dùng cổng mặc định.
    private static int resolvePort() {
        String rawPort = System.getProperty("server.port");
        if (rawPort == null || rawPort.isBlank())
            return DEFAULT_PORT;

        try {
            return Integer.parseInt(rawPort.trim());
        }
        catch (NumberFormatException e) {
            logger.warn("Invalid server.port '{}', using default {}", rawPort, DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
}
