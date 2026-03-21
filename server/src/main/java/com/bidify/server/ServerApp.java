package com.bidify.server;

import java.io.IOException;
import java.net.Socket;

import com.bidify.server.network.ClientHandler;
import com.bidify.server.database.DatabaseInitializer;
import com.bidify.server.repository.UserRepository;

import java.net.ServerSocket;

public class ServerApp {
    private static final int PORT = 5000;
    public static void main(String[] args) {
        System.out.println("Server is starting...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server shutting down, clearing all sessions...");
            new UserRepository().resetAllSessions(); // nếu tắt mà ko logout cũng tự động reset session account về 0
        }));

        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Server running on port: " + PORT);
            DatabaseInitializer.init();

            while (true){
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress());

                new Thread(new ClientHandler(socket)).start();
            }
        }
        catch (IOException e){ e.printStackTrace(); }
    }
}