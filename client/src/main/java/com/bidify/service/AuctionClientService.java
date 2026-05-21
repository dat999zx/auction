package com.bidify.service;

import java.io.IOException;

import com.bidify.common.dto.AuctionDto;
import com.bidify.common.enums.RequestStatus;
import com.bidify.common.enums.RequestType;
import com.bidify.common.exception.AuctionException;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.DeleteAuctionRequest;
import com.bidify.common.model.DisableAutoBidRequest;
import com.bidify.common.model.GetAuctionDetailRequest;
import com.bidify.common.model.JoinAuctionRequest;
import com.bidify.common.model.LeaveAuctionRequest;
import com.bidify.common.model.PlaceBidRequest;
import com.bidify.common.model.PayAuctionRequest;
import com.bidify.common.model.ConfirmDeliveryRequest;
import com.bidify.common.model.ResolveAuctionRequest;
import com.bidify.common.enums.AuctionResolutionAction;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.model.SearchAuctionRequest;
import com.bidify.common.model.SetAutoBidRequest;
import com.bidify.common.model.UpdateAuctionRequest;
import com.bidify.common.utility.JsonUtil;
import com.bidify.network.SocketClient;

public class AuctionClientService {
    private final SocketClient client = SocketClient.getClient();

    // dùng để tìm kiếm danh sách đấu giá
    public AuctionDto[] searchAuctions(String query) throws IOException {
        Response response = client.send(new Request(RequestType.SEARCH_AUCTIONS, new SearchAuctionRequest(query)));
        if (response.getStatus() != RequestStatus.SUCCESS) {
            throw new AuctionException(response.getMessage() == null ? "No result" : response.getMessage());
        }

        AuctionDto[] results = JsonUtil.fromMap(response.getData(), AuctionDto[].class);
        return (results != null) ? results : new AuctionDto[0];
    }

    // dùng để lấy live danh sách đấu giá
    public AuctionDto[] getLiveAuctions() throws IOException {
        Response response = client.send(new Request(RequestType.GET_LIVE_AUCTIONS, null));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new AuctionException(response.getMessage() == null ? "Cannot load live auctions." : response.getMessage());
        }

        AuctionDto[] auctions = JsonUtil.fromMap(response.getData(), AuctionDto[].class);
        if (auctions == null) throw new AuctionException("Cannot load live auctions.");
        return auctions;
    }

    // dùng để lấy upcoming danh sách đấu giá
    public AuctionDto[] getUpcomingAuctions() throws IOException {
        Response response = client.send(new Request(RequestType.GET_UPCOMING_AUCTIONS, null));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null)
            throw new AuctionException(response.getMessage() == null ? "Cannot load upcoming auctions." : response.getMessage());

        AuctionDto[] auctions = JsonUtil.fromMap(response.getData(), AuctionDto[].class);
        if (auctions == null) throw new AuctionException("Cannot load upcoming auctions.");
        return auctions;
    }

    public AuctionDto getAuctionDetail(String auctionId) throws IOException {
        Response response = client.send(new Request(RequestType.GET_AUCTION_DETAIL, new GetAuctionDetailRequest(auctionId)));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new AuctionException(response.getMessage() == null ? "Cannot load auction details." : response.getMessage());
        }

        AuctionDto auction = JsonUtil.fromMap(response.getData(), AuctionDto.class);
        if (auction == null) throw new AuctionException("Auction details came back in an unexpected format.");
        return auction;
    }

    // dùng để tạo đấu giá
    public Response createAuction(CreateAuctionRequest data) throws IOException {
        Response response = client.send(new Request(RequestType.CREATE_AUCTION, data));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    // dùng để cập nhật đấu giá
    public Response updateAuction(UpdateAuctionRequest data) throws IOException {
        Response response = client.send(new Request(RequestType.UPDATE_AUCTION, data));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    // dùng để xóa đấu giá
    public Response deleteAuction(String auctionId) throws IOException {
        Response response = client.send(new Request(RequestType.DELETE_AUCTION, new DeleteAuctionRequest(auctionId)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }


    // dùng để place lượt đặt giá
    public Response placeBid(String auctionId, double bidAmount) throws IOException {
        Response response = client.send(new Request(RequestType.PLACE_BID, new PlaceBidRequest(auctionId, bidAmount)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    public Response setAutoBid(String auctionId, double maxBid) throws IOException {
        Response response = client.send(new Request(RequestType.SET_AUTO_BID, new SetAutoBidRequest(auctionId, maxBid)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    // dùng để disable auto lượt đặt giá
    public Response disableAutoBid(String auctionId) throws IOException {
        Response response = client.send(new Request(RequestType.DISABLE_AUTO_BID, new DisableAutoBidRequest(auctionId)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    // dùng để tham gia
    public Response join(String auctionId) throws IOException {
        Response response = client.send(new Request(RequestType.JOIN_AUCTION, new JoinAuctionRequest(auctionId)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    // dùng để rời khỏi
    public Response leave(String auctionId) throws IOException {
        Response response = client.send(new Request(RequestType.LEAVE_AUCTION, new LeaveAuctionRequest(auctionId)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    // dùng để thanh toán đấu giá
    public Response payAuction(String auctionId) throws IOException {
        Response response = client.send(new Request(RequestType.PAY_AUCTION, new PayAuctionRequest(auctionId)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    // dùng để xác nhận giao hàng đấu giá
    public Response confirmAuctionDelivery(String auctionId) throws IOException {
        Response response = client.send(new Request(RequestType.CONFIRM_AUCTION_DELIVERY, new ConfirmDeliveryRequest(auctionId)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    // dùng để giải quyết đấu giá (cho admin)
    public Response resolveAuction(String auctionId, AuctionResolutionAction action) throws IOException {
        Response response = client.send(new Request(RequestType.RESOLVE_AUCTION, new ResolveAuctionRequest(auctionId, action)));
        if (response.getStatus() == RequestStatus.SUCCESS) return response;
        throw new AuctionException(response.getMessage());
    }

    // dùng để lấy các phiên đấu giá đã kết thúc cần xử lý của user
    public AuctionDto[] getUserSettlements() throws IOException {
        Response response = client.send(new Request(RequestType.GET_USER_SETTLEMENTS, null));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new AuctionException(response.getMessage() == null ? "Cannot load user settlements." : response.getMessage());
        }

        AuctionDto[] auctions = JsonUtil.fromMap(response.getData(), AuctionDto[].class);
        if (auctions == null) throw new AuctionException("Cannot load user settlements.");
        return auctions;
    }

    public AuctionDto[] getMyAuctions() throws IOException {
        Response response = client.send(new Request(RequestType.GET_MY_AUCTIONS, null));
        if (response.getStatus() != RequestStatus.SUCCESS || response.getData() == null) {
            throw new AuctionException(response.getMessage() == null ? "Cannot load my auctions." : response.getMessage());
        }

        AuctionDto[] auctions = JsonUtil.fromMap(response.getData(), AuctionDto[].class);
        if (auctions == null) throw new AuctionException("Cannot load my auctions.");
        return auctions;
    }
}

