package com.bidify.server.service.auction;

import java.util.ArrayList;
import java.util.List;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.model.GetAuctionDetailRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.SearchAuctionRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.common.utility.ValidationUtil;
import com.bidify.server.dao.AuctionDao;
import com.bidify.server.database.RealtimeDatabase;
import com.bidify.server.model.Auction;
import com.bidify.server.model.AutoBid;
import com.bidify.server.model.Item;
import com.bidify.server.model.User;
import com.bidify.server.network.ClientHandler;
import com.bidify.server.utility.ServiceUtil;

public class AuctionQueryService {
    private final AuctionDao auctionDao;
    private final AuctionDtoAssembler dtoAssembler;

    public AuctionQueryService(AuctionDao auctionDao, AuctionDtoAssembler dtoAssembler) {
        this.auctionDao = auctionDao;
        this.dtoAssembler = dtoAssembler;
    }

    public Response search(Request request) {
        return ServiceUtil.handleRequest(() -> {
            SearchAuctionRequest data = JsonUtil.fromMap(request.getData(), SearchAuctionRequest.class);
            ServiceUtil.validateRequestData(data);

            String query = data.getQuery();
            if (query == null) query = "";
            String finalQuery = query.toLowerCase().trim();

            List<Auction> allAuctions = RealtimeDatabase.getAllRuntimeAuctions();
            List<AuctionDto> results = new ArrayList<>();

            for (Auction auction : allAuctions) {
                Item item = dtoAssembler.getLinkedAuctionItem(auction);
                String auctionName = item != null ? item.getName() : auction.getAuctionName();
                String description = item != null ? item.getDescription() : auction.getDescription();
                String category = item != null ? item.getCategory() : null;
                String productType = item != null ? item.getProductType() : null;

                boolean matchesName = auctionName != null && auctionName.toLowerCase().contains(finalQuery);
                boolean matchesDesc = description != null && description.toLowerCase().contains(finalQuery);
                boolean matchesSeller = auction.getSellerUsername() != null && auction.getSellerUsername().toLowerCase().contains(finalQuery);
                boolean matchesCategory = category != null && category.toLowerCase().contains(finalQuery);
                boolean matchesProductType = productType != null && productType.toLowerCase().contains(finalQuery);
                if (matchesName || matchesDesc || matchesSeller || matchesCategory || matchesProductType)
                    results.add(dtoAssembler.toAuctionDto(auction, item, false));
            }

            return new Response(RequestStatus.SUCCESS, "Search completed", results);
        });
    }

    public Response getDetail(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            GetAuctionDetailRequest data = JsonUtil.fromMap(request.getData(), GetAuctionDetailRequest.class);
            ServiceUtil.validateRequestData(data);

            String auctionId = data.getAuctionId();
            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");

            Auction auction = RealtimeDatabase.getRuntimeAuction(auctionId);
            if (auction == null)
                auction = auctionDao.findById(auctionId);
            if (auction == null)
                throw new AuctionException("Auction not found");

            AuctionDto auctionDto = dtoAssembler.toAuctionDto(auction, true);
            if (client != null && client.isInSession()) {
                AutoBid currentUserAutoBid = auction.getAutoBid(client.getCurrentUsername());
                auctionDto.setCurrentUserAutoBidActive(currentUserAutoBid != null);
                auctionDto.setCurrentUserAutoBidMax(currentUserAutoBid == null ? null : currentUserAutoBid.getMaxBid());
            }
            return new Response(RequestStatus.SUCCESS, "Get auction detail successfully", auctionDto);
        });
    }

    public Response getAllLiveAuctions() {
        return ServiceUtil.handleRequest(() -> {
            List<Auction> auctions = RealtimeDatabase.getAllLiveAuctions();
            List<AuctionDto> summaries = new ArrayList<>();

            if (auctions == null || auctions.isEmpty())
                return new Response(RequestStatus.SUCCESS, "No live auctions", summaries);

            for (Auction auction : auctions)
                summaries.add(dtoAssembler.toAuctionDto(auction, false));

            return new Response(RequestStatus.SUCCESS, "Get live auctions successfully", summaries);
        });
    }

    public Response getAllUpcomingAuctions() {
        return ServiceUtil.handleRequest(() -> {
            List<Auction> auctions = RealtimeDatabase.getAllUpcomingAuctions();
            List<AuctionDto> summaries = new ArrayList<>();

            if (auctions == null || auctions.isEmpty())
                return new Response(RequestStatus.SUCCESS, "No upcoming auctions", summaries);

            for (Auction auction : auctions)
                summaries.add(dtoAssembler.toAuctionDto(auction, false));

            return new Response(RequestStatus.SUCCESS, "Get upcoming auctions successfully", summaries);
        });
    }

    public Response getUserSettlements(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User user = ServiceUtil.requireSessionUser(client);
            List<Auction> auctions = auctionDao.findUserSettlements(user.getUsername());
            List<AuctionDto> result = new ArrayList<>();
            for (Auction auction : auctions) {
                result.add(dtoAssembler.toAuctionDto(auction, false));
            }
            return new Response(RequestStatus.SUCCESS, "Get user settlements successfully", result);
        });
    }

    public Response getMyAuctions(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            User user = ServiceUtil.requireSessionUser(client);
            String username = user.getUsername();
            List<Auction> dbAuctions = auctionDao.findBySellerUsername(username);
            List<AuctionDto> result = new ArrayList<>();
            for (Auction auction : dbAuctions) {
                Auction runtime = RealtimeDatabase.getRuntimeAuction(auction.getId());
                Auction effective = runtime != null ? runtime : auction;
                result.add(dtoAssembler.toAuctionDto(effective, false));
            }
            return new Response(RequestStatus.SUCCESS, "Get my auctions successfully", result);
        });
    }

    public Response getAdminAuctions(ClientHandler client, Request request) {
        return ServiceUtil.handleRequest(() -> {
            ServiceUtil.requireAdmin(client);

            List<Auction> dbAuctions = auctionDao.findAll();
            List<AuctionDto> result = new ArrayList<>();
            for (Auction auction : dbAuctions) {
                Auction runtime = RealtimeDatabase.getRuntimeAuction(auction.getId());
                Auction effective = runtime != null ? runtime : auction;
                result.add(dtoAssembler.toAuctionDto(effective, false));
            }

            return new Response(RequestStatus.SUCCESS, "Get admin auctions successfully", result);
        });
    }
}
