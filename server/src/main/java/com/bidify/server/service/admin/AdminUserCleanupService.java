package com.bidify.server.service.admin;

import java.util.List;

import com.bidify.common.enums.EventType;
import com.bidify.common.model.Event;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.dao.BidDao;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.dao.TransactionDao;
import com.bidify.server.dao.UserDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.service.AuctionService;
import com.bidify.server.service.ImageService;

public class AdminUserCleanupService {
    private final UserDao userDao;
    private final AuctionDao auctionDao;
    private final ItemDao itemDao;
    private final BidDao bidDao;
    private final TransactionDao transactionDao;
    private final ImageDao imageDao;
    private final ImageService imageService;
    private final AuctionService auctionService;

    public AdminUserCleanupService(
            UserDao userDao,
            AuctionDao auctionDao,
            ItemDao itemDao,
            BidDao bidDao,
            TransactionDao transactionDao,
            ImageDao imageDao,
            ImageService imageService,
            AuctionService auctionService) {
        this.userDao = userDao;
        this.auctionDao = auctionDao;
        this.itemDao = itemDao;
        this.bidDao = bidDao;
        this.transactionDao = transactionDao;
        this.imageDao = imageDao;
        this.imageService = imageService;
        this.auctionService = auctionService;
    }

    public void deleteUser(User target) {
        String username = target.getUsername();
        disconnectUser(username, "Your account has been deleted by an administrator.");

        for (Auction auction : auctionDao.findBySellerUsername(username)) {
            auctionService.deleteAuctionCascade(auction, false);
        }

        for (Item item : itemDao.findByOwnerUsername(username))
            deleteItemCascade(item);

        bidDao.deleteByUsername(username);
        transactionDao.deleteByUsername(username);
        userDao.deleteByUsername(username);
    }

    public void forceLogoutUser(String username, String message) {
        ClientHandler clientHandler = RealtimeDatabase.getUserClient(username);
        clearActiveSession(username);
        if (clientHandler != null) {
            clientHandler.sendEvent(new Event(EventType.FORCED_LOGOUT, message));
            clientHandler.setCurrentUsername(null);
        }
    }

    private void disconnectUser(String username, String message) {
        ClientHandler clientHandler = RealtimeDatabase.getUserClient(username);
        clearActiveSession(username);
        if (clientHandler != null) {
            clientHandler.sendEvent(new Event(EventType.SERVER_NOTICE, message));
            clientHandler.setCurrentUsername(null);
            clientHandler.closeConnection();
        }
    }

    private void clearActiveSession(String username) {
        User activeUser = RealtimeDatabase.getActiveUser(username);
        if (activeUser != null)
            userDao.save(activeUser, false);

        List<String> affectedAuctionIds = RealtimeDatabase.removeActiveUser(username);
        for (String auctionId : affectedAuctionIds)
            auctionService.publishLiveAudienceUpdate(auctionId);
    }

    private void deleteItemCascade(Item item) {
        if (item == null) return;

        for (String imageId : itemDao.findImageIdsByItemId(item.getId())) {
            Image image = imageDao.findById(imageId);
            if (image != null)
                imageService.deleteImageFile(image.getFilePath());
            
            imageDao.deleteById(imageId);
        }

        itemDao.deleteItemImageLinks(item.getId());
        itemDao.deleteById(item.getId());
    }
}
