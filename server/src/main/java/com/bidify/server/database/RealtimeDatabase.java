package com.bidify.server.database;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.model.Auction;
import com.bidify.server.model.User;
import com.bidify.server.model.runtime.AuctionChannel;
import com.bidify.server.model.runtime.ClientSession;
import com.bidify.server.model.runtime.GlobalChannel;
import com.bidify.server.network.ClientHandler;

// database được lưu trong ram, giúp truy cập nhanh trong thời gian thực. chỉ có hiệu lực khi server chạy
public class RealtimeDatabase {
    private static final ConcurrentHashMap<String, ClientSession> activeUsers = new ConcurrentHashMap<>(); // người dùng đang kết nối server
    private static final ConcurrentHashMap<String, Auction> runtimeAuctions = new ConcurrentHashMap<>(); // các cuộc đấu giá UPCOMING / ACTIVE trong RAM
    private static final ConcurrentHashMap<String, AuctionChannel> auctionChannels = new ConcurrentHashMap<>(); // các channel của các cuộc đấu giá
    private static final GlobalChannel globalChannel = new GlobalChannel(); // channel chung của hệ thống

    private RealtimeDatabase(){}

    public static boolean isUserOnline(String username){ // kiểm tra user có online ko
        if (username == null) return false;
        return activeUsers.containsKey(username);
    }

    public static boolean isWatchingAuction(String username, String auctionId){ // kiểm tra user có đang xem auction ko
        if (username == null || auctionId == null) return false;
        ClientHandler client = getUserClient(username);
        AuctionChannel channel = auctionChannels.get(auctionId);
        if (client == null || channel == null) return false;
        return channel.hasObserver(client);
    }

    public static void addActiveUser(ClientHandler client, User user){ // thêm user vào database
        if (client == null || client.getCurrentUsername() == null) return;
        activeUsers.put(client.getCurrentUsername(), new ClientSession(client, user));
        globalChannel.subscribe(client);
    }

    public static ClientHandler getUserClient(String username){ // lấy client trong database
        if (username == null) return null;
        ClientSession session = activeUsers.get(username);
        if (session == null) return null;
        return session.getClientHandler();
    }

    public static User getActiveUser(String username){ // lấy user trong database
        if (username == null) return null;
        ClientSession session = activeUsers.get(username);
        if (session == null) return null;
        return session.getUser();
    }

    public static List<User> getAllActiveUsers(){ // lấy tất cả user trong database
        List<User> users = new ArrayList<>();
        for (ClientSession session : activeUsers.values()) {
            if (session != null && session.getUser() != null)
                users.add(session.getUser());
        }
        return users;
    }

    public static ClientSession getUserSession(String username){ // lấy session trong database
        if (username == null) return null;
        return activeUsers.get(username);
    }

    public static List<ClientHandler> getAllUserClients(){ // lấy tất cả client trong database
        List<ClientHandler> clients = new ArrayList<>();
        for (ClientSession session : activeUsers.values()){
            if (session != null && session.getClientHandler() != null)
                clients.add(session.getClientHandler());
        }
        return clients;
    }

    public static List<String> removeActiveUser(String username){ // xóa user khỏi database
        List<String> affectedAuctionIds = new ArrayList<>();
        if (username == null) return affectedAuctionIds;
        ClientSession session = activeUsers.remove(username);
        if (session == null) return affectedAuctionIds;
        ClientHandler client = session.getClientHandler();
        if (client == null) return affectedAuctionIds;
        globalChannel.unsubscribe(client);
        for (AuctionChannel channel : auctionChannels.values()) {
            if (channel == null || !channel.hasObserver(client))
                continue;
            channel.unsubscribe(client);
            affectedAuctionIds.add(channel.getAuctionId());
        }
        return affectedAuctionIds;
    }

    public static void addRuntimeAuction(Auction auction){ // thêm cuộc đấu giá UPCOMING / ACTIVE vào runtime
        if (auction == null) return;
        AuctionStatus status = auction.getStatus();
        if (status != AuctionStatus.UPCOMING && status != AuctionStatus.ACTIVE) return;
        runtimeAuctions.put(auction.getId(), auction);
        auctionChannels.putIfAbsent(auction.getId(), new AuctionChannel(auction.getId()));
    }

    public static Auction getRuntimeAuction(String auctionId){ // lấy cuộc đấu giá từ runtime
        if (auctionId == null) return null;
        return runtimeAuctions.get(auctionId);
    }

    public static List<Auction> getAllRuntimeAuctions(){ // lấy tất cả cuộc đấu giá UPCOMING / ACTIVE trong runtime
        return new ArrayList<>(runtimeAuctions.values());
    }

    public static Auction getLiveAuction(String auctionId){ // lấy cuộc đấu giá ACTIVE từ runtime
        Auction auction = getRuntimeAuction(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE) return null;
        return auction;
    }

    public static Auction getUpcomingAuction(String auctionId){ // lấy cuộc đấu giá UPCOMING từ runtime
        Auction auction = getRuntimeAuction(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.UPCOMING) return null;
        return auction;
    }

    public static List<Auction> getAllLiveAuctions(){ // lấy tất cả cuộc đấu giá ACTIVE trong runtime
        return getRuntimeAuctionsByStatus(AuctionStatus.ACTIVE);
    }

    public static List<Auction> getAllUpcomingAuctions(){ // lấy tất cả cuộc đấu giá UPCOMING trong runtime
        return getRuntimeAuctionsByStatus(AuctionStatus.UPCOMING);
    }

    public static List<Auction> getRuntimeAuctionsByStatus(AuctionStatus status){ // lấy các cuộc đấu giá trong runtime theo status
        List<Auction> auctions = new ArrayList<>();
        if (status == null) return auctions;
        for (Auction auction : runtimeAuctions.values()) {
            if (auction != null && auction.getStatus() == status)
                auctions.add(auction);
        }
        return auctions;
    }

    public static void removeRuntimeAuction(String auctionId){ // xóa cuộc đấu giá khỏi runtime
        if (auctionId == null) return;
        runtimeAuctions.remove(auctionId);
        auctionChannels.remove(auctionId);
    }

    public static void subscribeAuctionChannel(String auctionId, String username){ // thêm người xem vào database
        if (auctionId == null || username == null) return;
        AuctionChannel channel = auctionChannels.get(auctionId);
        ClientHandler client = getUserClient(username);
        if (channel == null || client == null) return;
        channel.subscribe(client);
    }

    public static void unsubscribeAuctionChannel(String auctionId, String username){ // xóa người xem auction
        if (auctionId == null || username == null) return;
        AuctionChannel channel = auctionChannels.get(auctionId);
        ClientHandler client = getUserClient(username);
        if (channel == null || client == null) return;
        channel.unsubscribe(client);
    }

    public static AuctionChannel getAuctionChannel(String auctionId){ // lấy channel của auction
        if (auctionId == null) return null;
        return auctionChannels.get(auctionId);
    }

    public static GlobalChannel getGlobalChannel(){ // lấy channel chung của hệ thống
        return globalChannel;
    }

    public static void clearAll(){ // xóa tất cả dữ liệu trong database
        activeUsers.clear();
        runtimeAuctions.clear();
        for (AuctionChannel channel : auctionChannels.values())
            channel.clear();
        auctionChannels.clear();
        globalChannel.clear();
    }
}
