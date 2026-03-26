package com.bidify.server.database;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bidify.server.model.Auction;
import com.bidify.server.network.ClientHandler;

// database được lưu trong ram, giúp truy cập nhanh trong thời gian thực. chỉ có hiệu lực khi server chạy
public class RealtimeDatabase {
    private static final ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>(); // người dùng đang kết nối server
    private static final ConcurrentHashMap<String, Auction> liveAuctions = new ConcurrentHashMap<>(); // các cuộc đấu giá đang chạy
    private static final ConcurrentHashMap<String, Set<String>> auctionWatchers = new ConcurrentHashMap<>(); // cuộc đấu giá đang chứa người xem nào
    private static final ConcurrentHashMap<String, Set<String>> userWatching = new ConcurrentHashMap<>(); // người dùng đang xem các cuộc đấu giá nào
    // auctionWatchers và userWatching là 2 map ngược nhau
    // auctionWatchers có dạng (auction_id, [username1, username2, ...]) là auction này đang chứa các user nào
    // userWatching có dạng (username, [auction_id1, auction_id2, ...]) là user này đang xem các auction nào

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

    public static void saveClient(ClientHandler client){ // lưu client data
        // TODO: save client data
        System.out.println("saved client: " + client.getCurrentUsername());
    }

    public static void saveAllClients(){ // lưu tất cả client data
        for (ClientHandler client : activeClients.values())
            saveClient(client);
    }

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

    public static void saveAuction(Auction auction){ // lưu auction data
        // TODO: save auction data
        System.out.println("saved auction: " + auction.getAuctionName());
    }

    public static void saveAllAuctions(){ // lưu tất cả auction data
        for (Auction auction : liveAuctions.values())
            saveAuction(auction);
    }

    public static void addAuctionWatcher(String auctionId, String username){ // thêm người xem vào database
        if (auctionId == null || username == null) return;
        auctionWatchers.computeIfAbsent(auctionId, k -> ConcurrentHashMap.newKeySet()).add(username);
        userWatching.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(auctionId);
    }

    public static void removeAuctionWatcher(String auctionId, String username){ // xóa người xem khỏi database
        if (auctionId == null || username == null) return;
        if (!auctionWatchers.containsKey(auctionId) || !auctionWatchers.get(auctionId).contains(username)) return;
        if (!userWatching.containsKey(username) || !userWatching.get(username).contains(auctionId)) return;
        auctionWatchers.get(auctionId).remove(username);
        userWatching.get(username).remove(auctionId);
    }

    public static void saveAll(){ // lưu tất cả dữ liệu
        saveAllClients();
        saveAllAuctions();
    }

    public static void clearAll(){ // xóa tất cả dữ liệu trong database
        activeClients.clear();
        liveAuctions.clear();
    }
}
