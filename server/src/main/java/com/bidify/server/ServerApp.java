package com.bidify.server;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.ServerSocket;

public class ServerApp {
    private static final int PORT = 5000;
    public static void main(String[] args) {
        System.out.println("Server is starting...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)){
            System.out.println("Server running on port: " + PORT);

            while (true){
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getInetAddress());

                new Thread(new ClientHandler(socket)).start();
            }
        }
        catch(SocketException e){ System.out.println("Client disconnected"); }
        catch (IOException e){ e.printStackTrace(); }
    }
}