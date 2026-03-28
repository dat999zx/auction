package com.bidify.server;

import java.io.IOException;
import java.net.Socket;

import com.bidify.server.network.ClientHandler;
import com.bidify.server.database.DatabaseManager;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.repository.UserRepository;

import java.net.ServerSocket;

public class ServerApp {
    private static final int PORT = 5000;
    public static void main(String[] args) {
        System.out.println("Server is starting...");
        DatabaseManager.init();
        RealtimeDatabase.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server is shutting down, saving all data...");
            RealtimeDatabase.saveAll();
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Server running on port: " + PORT);

            while (true){
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress());

                new Thread(new ClientHandler(socket)).start();
            }
        }
        catch (IOException e){ e.printStackTrace(); }
    }
}
