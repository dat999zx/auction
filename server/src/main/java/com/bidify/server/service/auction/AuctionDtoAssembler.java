package com.bidify.server.service.auction;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.server.dao.ImageDao;
import com.bidify.server.dao.ItemDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Auction;
import com.bidify.server.model.Bid;
import com.bidify.server.model.Image;
import com.bidify.server.model.Item;
import com.bidify.server.model.ItemImageLink;
import com.bidify.server.model.runtime.AuctionChannel;
import com.bidify.server.service.ImageService;
import com.bidify.server.utility.AuctionMapper;

// assembler cho AuctionMapper
public class AuctionDtoAssembler {
    private static final Logger logger = LoggerFactory.getLogger(AuctionDtoAssembler.class);

    private final ItemDao itemDao = ItemDao.getInstance();
    private final ImageDao imageDao = ImageDao.getInstance();
    private final ImageService imageService = ImageService.getInstance();

    public AuctionDto toAuctionDto(Auction auction, boolean includeGallery) {
        return toAuctionDto(auction, getLinkedAuctionItem(auction), includeGallery);
    }

    public AuctionDto toAuctionDto(Auction auction, Item item, boolean includeGallery) {
        List<String> gallery = includeGallery ? getGallery(item) : null;
        AuctionDto dto = AuctionMapper.toDto(auction, item, getThumbnail(item), gallery);
        dto.setWatcherCount(resolveWatcherCount(auction));
        dto.setActiveBidderCount(resolveActiveBidderCount(auction));
        return dto;
    }

    public Item getLinkedAuctionItem(Auction auction) {
        if (auction == null) return null;
        String itemId = auction.getItemId();
        if (itemId == null || itemId.isBlank()) return null;
        return itemDao.findById(itemId);
    }

    private String getThumbnail(Item item) {
        if (item == null) return null;
        try {
            List<ItemImageLink> links = itemDao.getItemImageLinks(item.getId());
            for (ItemImageLink link : links) {
                if (!link.isPrimary()) continue;
                Image image = imageDao.findById(link.getImageId());
                if (image != null) return imageService.getBase64Image(image.getFilePath());
            }
            for (ItemImageLink link : links) {
                Image image = imageDao.findById(link.getImageId());
                if (image != null) return imageService.getBase64Image(image.getFilePath());
            }
        } catch (DatabaseException e) {
            logger.warn("Could not load thumbnail for item {}: {}", item.getId(), e.getMessage());
        }
        return null;
    }

    private List<String> getGallery(Item item) {
        List<String> gallery = new ArrayList<>();
        if (item == null) return gallery;
        try {
            for (ItemImageLink link : itemDao.getItemImageLinks(item.getId())) {
                Image image = imageDao.findById(link.getImageId());
                if (image == null) continue;
                String base64 = imageService.getBase64Image(image.getFilePath());
                if (base64 != null) gallery.add(base64);
            }
        } catch (DatabaseException e) {
            logger.warn("Could not load gallery for item {}: {}", item.getId(), e.getMessage());
        }
        return gallery;
    }

    private int resolveWatcherCount(Auction auction) {
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE) return 0;
        AuctionChannel channel = RealtimeDatabase.getAuctionChannel(auction.getId());
        return channel == null ? 0 : channel.getObserverCount();
    }

    private int resolveActiveBidderCount(Auction auction) {
        if (auction == null || auction.getBids() == null || auction.getBids().isEmpty()) return 0;
        Set<String> bidders = new LinkedHashSet<>();
        for (Bid bid : auction.getBids()) {
            if (bid == null || bid.getBidderUsername() == null || bid.getBidderUsername().isBlank()) continue;
            bidders.add(bid.getBidderUsername());
        }
        return bidders.size();
    }
}
