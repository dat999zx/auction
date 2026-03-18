package com.bidify.server;

import java.io.IOException;
import java.net.Socket;

import com.bidify.server.network.ClientHandler;
import com.bidify.server.database.DatabaseInitializer;

import java.net.ServerSocket;

public class ServerApp {
    private static final int PORT = 5000;
    public static void main(String[] args) {
        System.out.println("Server is starting...");

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