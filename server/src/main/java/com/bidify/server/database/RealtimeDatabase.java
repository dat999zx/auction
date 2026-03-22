package com.bidify.server.database;

import java.util.concurrent.ConcurrentHashMap;

import com.bidify.server.model.Auction;
import com.bidify.server.network.ClientHandler;

// database được lưu trong ram, giúp truy cập nhanh trong thời gian thực. chỉ có hiệu lực khi server chạy
public class RealtimeDatabase {
    private static final ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>(); // người dùng đang kết nối server
    private static final ConcurrentHashMap<String, Auction> liveAuctions = new ConcurrentHashMap<>(); // các cuộc đấu giá đang chạy

    public static void addActiveClient(ClientHandler client){ // thêm client vào database
        if (client == null || client.getCurrentUsername() == null) return;
        activeClients.put(client.getCurrentUsername(), client);
    }

    public static ClientHandler getActiveClient(String username){ // lấy client trong database
        if (username == null) return null;
        return activeClients.get(username);
    }

    public static ClientHandler[] getAllActiveClients(){ // lấy tất cả client trong database
        return activeClients.values().toArray(new ClientHandler[0]);
    }

    public static void removeActiveClient(String username){ // xóa client khỏi database
        if (username == null) return;
        activeClients.remove(username);
    }

    public static void removeAllActiveClients(){ activeClients.clear(); } // xóa tất cả client khỏi database

    public static void addLiveAuction(Auction auction){ // thêm cuộc đấu giá vào database
        if (auction == null) return;
        liveAuctions.put(auction.getId(), auction);
    }

    public static Auction getLiveAuction(String auctionId){ // lấy cuộc đấu giá từ database
        if (auctionId == null) return null;
        return liveAuctions.get(auctionId);
    }

    public static Auction[] getAllLiveAuctions(){ // lấy tất cả cuộc đấu giá đang chạy
        return liveAuctions.values().toArray(new Auction[0]);
    }

    public static void removeLiveAuction(String auctionId){ // xóa cuộc đấu giá khỏi database
        if (auctionId == null) return;
        liveAuctions.remove(auctionId);
    }

    public static void removeAllLiveAuctions(){ liveAuctions.clear(); } // xóa tất cả cuộc đấu giá khỏi database

    public static void clearAll(){ // xóa tất cả dữ liệu trong database
        activeClients.clear();
        liveAuctions.clear();
    }
}
