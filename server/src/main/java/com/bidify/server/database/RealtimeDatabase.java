package com.bidify.server.database;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.bidify.common.enums.AuctionStatus;
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

    public static boolean isUserOnline(String username){ // kiểm tra user có online ko
        if (username == null) return false;
        return activeClients.containsKey(username);
    }

    public static boolean isWatchingAuction(String username, String auctionId){ // kiểm tra user có đang xem auction ko
        if (username == null || auctionId == null) return false;
        return userWatching.containsKey(username) && userWatching.get(username).contains(auctionId);
    }

    public static boolean addActiveClient(ClientHandler client){ // thêm client vào database
        if (client == null || client.getCurrentUsername() == null) return false;
        activeClients.put(client.getCurrentUsername(), client);
        return true;
    }

    public static ClientHandler getActiveClient(String username){ // lấy client trong database
        if (username == null || !activeClients.containsKey(username)) return null;
        return activeClients.get(username);
    }

    public static List<ClientHandler> getAllActiveClients(){ // lấy tất cả client trong database
        return new ArrayList<>(activeClients.values());
    }

    public static boolean removeActiveClient(String username){ // xóa client khỏi database
        if (username == null) return false;
        for (String auctionId : auctionWatchers.keySet())
            removeAuctionWatcher(auctionId, username);
        if (activeClients.containsKey(username)) activeClients.remove(username);
        if (userWatching.containsKey(username)) userWatching.remove(username);
        return true;
    }

    public static boolean addLiveAuction(Auction auction){ // thêm cuộc đấu giá vào database
        if (auction == null) return false;
        liveAuctions.put(auction.getId(), auction);
        return true;
    }

    public static Auction getLiveAuction(String auctionId){ // lấy cuộc đấu giá từ database
        if (auctionId == null || !liveAuctions.containsKey(auctionId)) return null;
        return liveAuctions.get(auctionId);
    }

    public static List<Auction> getAllLiveAuctions(){ // lấy tất cả cuộc đấu giá đang chạy
        return new ArrayList<>(liveAuctions.values());
    }

    public static boolean removeLiveAuction(String auctionId){ // xóa cuộc đấu giá khỏi database
        if (auctionId == null || !liveAuctions.containsKey(auctionId)) return false;
        liveAuctions.remove(auctionId);
        return true;
    }

    public static boolean addAuctionWatcher(String auctionId, String username){ // thêm người xem vào database
        if (auctionId == null || username == null) return false;
        auctionWatchers.computeIfAbsent(auctionId, k -> ConcurrentHashMap.newKeySet()).add(username);
        userWatching.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(auctionId);
        return true;
    }

    public static List<ClientHandler> getAuctionWatchers(String auctionId){ // lấy tất cả người xem auction
        if (auctionId == null || !auctionWatchers.containsKey(auctionId)) return null;
        List<ClientHandler> watchers = new ArrayList<>();
        for (String username : auctionWatchers.get(auctionId))
            watchers.add(getActiveClient(username));
        return watchers;
    }

    public static boolean removeAuctionWatcher(String auctionId, String username){ // xóa người xem auction
        if (auctionId == null || username == null) return false;
        Set<String> watchers = auctionWatchers.get(auctionId);
        if (watchers != null){
            watchers.remove(username);
            if (watchers.isEmpty()) auctionWatchers.remove(auctionId);
        }

        Set<String> watchingAuctions = userWatching.get(username);
        if (watchingAuctions != null){
            watchingAuctions.remove(auctionId);
            if (watchingAuctions.isEmpty()) userWatching.remove(username);
        }
        return true;
    }

    public static void clearAll(){ // xóa tất cả dữ liệu trong database
        activeClients.clear();
        liveAuctions.clear();
        auctionWatchers.clear();
        userWatching.clear();
    }
}
