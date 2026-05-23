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

// RealtimeDatabase lưu trữ trạng thái runtime (in-memory) phục vụ kết nối trực tuyến và sự kiện thời gian thực.
public class RealtimeDatabase {
    private static final ConcurrentHashMap<String, ClientSession> activeUsers = new ConcurrentHashMap<>(); // Người dùng hiện đang kết nối trực tuyến
    private static final ConcurrentHashMap<String, Auction> runtimeAuctions = new ConcurrentHashMap<>(); // Các cuộc đấu giá UPCOMING hoặc ACTIVE đang diễn ra
    private static final ConcurrentHashMap<String, AuctionChannel> auctionChannels = new ConcurrentHashMap<>(); // Kênh sự kiện cho từng cuộc đấu giá
    private static final GlobalChannel globalChannel = new GlobalChannel(); // Kênh sự kiện chung toàn hệ thống

    private RealtimeDatabase(){}

    public static boolean isUserOnline(String username){
        if (username == null) return false;
        return activeUsers.containsKey(username);
    }

    public static boolean isWatchingAuction(String username, String auctionId){
        if (username == null || auctionId == null) return false;
        ClientHandler client = getUserClient(username);
        AuctionChannel channel = auctionChannels.get(auctionId);
        if (client == null || channel == null) return false;
        return channel.hasObserver(client);
    }

    public static void addActiveUser(ClientHandler client, User user){
        if (client == null || client.getCurrentUsername() == null) return;
        activeUsers.put(client.getCurrentUsername(), new ClientSession(client, user));
        globalChannel.subscribe(client);
    }

    public static ClientHandler getUserClient(String username){
        if (username == null) return null;
        ClientSession session = activeUsers.get(username);
        if (session == null) return null;
        return session.getClientHandler();
    }

    public static User getActiveUser(String username){
        if (username == null) return null;
        ClientSession session = activeUsers.get(username);
        if (session == null) return null;
        return session.getUser();
    }

    public static List<User> getAllActiveUsers(){
        List<User> users = new ArrayList<>();
        for (ClientSession session : activeUsers.values()) {
            if (session != null && session.getUser() != null)
                users.add(session.getUser());
        }
        return users;
    }

    public static ClientSession getUserSession(String username){
        if (username == null) return null;
        return activeUsers.get(username);
    }

    public static List<ClientHandler> getAllUserClients(){
        List<ClientHandler> clients = new ArrayList<>();
        for (ClientSession session : activeUsers.values()){
            if (session != null && session.getClientHandler() != null)
                clients.add(session.getClientHandler());
        }
        return clients;
    }

    public static List<String> removeActiveUser(String username){
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

    public static void addRuntimeAuction(Auction auction){
        if (auction == null) return;
        AuctionStatus status = auction.getStatus();
        if (status != AuctionStatus.UPCOMING && status != AuctionStatus.ACTIVE) return;
        runtimeAuctions.put(auction.getId(), auction);
        auctionChannels.putIfAbsent(auction.getId(), new AuctionChannel(auction.getId()));
    }

    public static Auction getRuntimeAuction(String auctionId){
        if (auctionId == null) return null;
        return runtimeAuctions.get(auctionId);
    }

    public static List<Auction> getAllRuntimeAuctions(){
        return new ArrayList<>(runtimeAuctions.values());
    }

    public static Auction getLiveAuction(String auctionId){
        Auction auction = getRuntimeAuction(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE) return null;
        return auction;
    }

    public static Auction getUpcomingAuction(String auctionId){
        Auction auction = getRuntimeAuction(auctionId);
        if (auction == null || auction.getStatus() != AuctionStatus.UPCOMING) return null;
        return auction;
    }

    public static List<Auction> getAllLiveAuctions(){
        return getRuntimeAuctionsByStatus(AuctionStatus.ACTIVE);
    }

    public static List<Auction> getAllUpcomingAuctions(){
        return getRuntimeAuctionsByStatus(AuctionStatus.UPCOMING);
    }

    public static List<Auction> getRuntimeAuctionsByStatus(AuctionStatus status){
        List<Auction> auctions = new ArrayList<>();
        if (status == null) return auctions;
        for (Auction auction : runtimeAuctions.values()) {
            if (auction != null && auction.getStatus() == status)
                auctions.add(auction);
        }
        return auctions;
    }

    public static void removeRuntimeAuction(String auctionId){
        if (auctionId == null) return;
        runtimeAuctions.remove(auctionId);
        auctionChannels.remove(auctionId);
    }

    public static void subscribeAuctionChannel(String auctionId, String username){
        if (auctionId == null || username == null) return;
        AuctionChannel channel = auctionChannels.get(auctionId);
        ClientHandler client = getUserClient(username);
        if (channel == null || client == null) return;
        channel.subscribe(client);
    }

    public static void unsubscribeAuctionChannel(String auctionId, String username){
        if (auctionId == null || username == null) return;
        AuctionChannel channel = auctionChannels.get(auctionId);
        ClientHandler client = getUserClient(username);
        if (channel == null || client == null) return;
        channel.unsubscribe(client);
    }

    public static AuctionChannel getAuctionChannel(String auctionId){
        if (auctionId == null) return null;
        return auctionChannels.get(auctionId);
    }

    public static GlobalChannel getGlobalChannel(){
        return globalChannel;
    }

    public static void clearAll(){
        activeUsers.clear();
        runtimeAuctions.clear();
        for (AuctionChannel channel : auctionChannels.values())
            channel.clear();
        auctionChannels.clear();
        globalChannel.clear();
    }
}
