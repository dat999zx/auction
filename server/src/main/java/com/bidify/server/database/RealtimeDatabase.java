package com.bidify.server.database;

import java.time.LocalDateTime;
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

    public static void init(){
        clearAll();
        DatabaseManager.query(
            "SELECT * FROM Auctions WHERE status = ?",
            rs -> {
                while (rs.next()) {
                    Auction auction = new Auction(rs.getString("id"));
                    auction.setAuctionName(rs.getString("auctionName"));
                    auction.setDescription(rs.getString("description"));
                    auction.setCategory(rs.getString("category"));
                    auction.setProductType(rs.getString("type"));
                    auction.setStartingPrice(rs.getDouble("startingPrice"));
                    auction.setMinIncrement(rs.getDouble("minIncrement"));
                    auction.setMaxIncrement(rs.getDouble("maxIncrement"));
                    auction.setSeller(rs.getString("seller"));
                    auction.setStatus(AuctionStatus.valueOf(rs.getString("status")));
                    auction.setStartTime(LocalDateTime.parse(rs.getString("startAt")));
                    auction.setEndTime(LocalDateTime.parse(rs.getString("endTime")));
                    addLiveAuction(auction);
                }
                return null;
            },
            AuctionStatus.ACTIVE.toString()
        );
    }

    public static boolean isUserOnline(String username){ // kiểm tra user có online ko
        if (username == null) return false;
        return activeClients.containsKey(username);
    }

    public static boolean isWatchingAuction(String username, String auctionId){
        if (username == null || auctionId == null) return false;
        return userWatching.containsKey(username) && userWatching.get(username).contains(auctionId);
    }

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
        for (String auctionId : auctionWatchers.keySet())
            removeAuctionWatcher(auctionId, username);
        userWatching.remove(username);
        activeClients.remove(username);
    }

    public static void saveClient(ClientHandler client){ // lưu client data
        if (client == null || client.getCurrentUsername() == null) return;
        DatabaseManager.update(
            "UPDATE Users SET lastLogin = ? WHERE username = ?",
            LocalDateTime.now().toString(),
            client.getCurrentUsername()
        );
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

    public static void saveAuction(Auction auction){ // lưu auction data
        if (auction == null) return;
        String currentBidderName = auction.getCurrentBidder() != null
            ? auction.getCurrentBidder().getNickname()
            : null;
        DatabaseManager.update(
            """
                                    UPDATE Auctions SET 
                                auctionName = ?,
                            description = ?,
                        category = ?,
                    type = ?,
                startingPrice = ?,
            minIncrement = ?,
                maxIncrement = ?,
                    seller = ?,
                        currentBidder = ?,
                            status = ?,
                                startAt = ?,
                                    endTime = ?
                                        WHERE id = ?
            """,
                                        auction.getAuctionName(),
                                    auction.getDescription(),
                            auction.getCategory(),
                        auction.getProductType(),
                    auction.getStartingPrice(),
                auction.getMinIncrement(),
            auction.getMaxIncrement(),
                auction.getSeller(),
                    currentBidderName,
                        auction.getStatus().toString(),
                            auction.getStartTime().toString(),
                                    auction.getEndTime().toString(),
                                        auction.getId()
        );
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

    public static void removeAuctionWatcher(String auctionId, String username){ // xóa người xem auction
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
        auctionWatchers.clear();
        userWatching.clear();
    }
}
