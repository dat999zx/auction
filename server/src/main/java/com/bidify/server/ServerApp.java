package com.bidify.server;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import com.bidify.server.network.ClientHandler;
import com.bidify.server.repository.AuctionRepository;
import com.bidify.server.repository.UserRepository;
import com.bidify.server.database.SQLiteHelper;
import com.bidify.server.model.Auction;
import com.bidify.server.database.RealtimeDatabase;

import java.net.ServerSocket;

public class ServerApp {
    private static final int PORT = 5000;
    public static void main(String[] args) {
        System.out.println("Server is starting...");
        
        SQLiteHelper.init();
        new AuctionRepository().init();

        List<Auction> auctions = RealtimeDatabase.getAllLiveAuctions();
        for (Auction auction : auctions){
            System.out.println(auction.getAuctionName());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server is shutting down, saving all data...");
            new UserRepository().saveAllClients();
            new AuctionRepository().saveAllAuctions();
            RealtimeDatabase.clearAll();
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
