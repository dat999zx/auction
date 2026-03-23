package com.bidify.server.service;

import com.bidify.server.exception.DatabaseException;
import com.bidify.server.model.Auction;
import com.bidify.server.network.ClientHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.enums.AuctionStatus;
import com.bidify.common.model.CreateAuctionRequest;
import com.bidify.common.model.UpdateAuctionRequest;
import com.bidify.common.model.Request;
import com.bidify.common.model.Response;
import com.bidify.common.util.JsonUtil;
import com.bidify.common.util.ValidationUtil;
import com.bidify.server.repository.AuctionRepository;

public class AuctionService {
    private final AuctionRepository auctionRepository = new AuctionRepository();

    public Response createAuction(ClientHandler client, Request request){
        CreateAuctionRequest data = JsonUtil.fromMap(request.getData(), CreateAuctionRequest.class);
        if (data == null) return new Response(RequestStatus.INVALID_REQUEST, "Invalid request data");

        String seller = data.getSeller();
        String auctionName = data.getAuctionName();
        String description = data.getDescription();
        double startingPrice = data.getStartingPrice();
        LocalDateTime startTime, endTime;
        try{
            startTime = LocalDateTime.parse(data.getStartTime());
            endTime = LocalDateTime.parse(data.getEndTime());
        } catch (DateTimeParseException e) {
            return new Response(RequestStatus.FAILED, "Invalid date time format");
        }

        try{
            ValidationUtil.requiresNonBlank(seller, "Seller");
            ValidationUtil.requiresNonBlank(auctionName, "Auction's name");
            ValidationUtil.requiresNonBlank(description, "Description");
            ValidationUtil.validateMaxLength("Description", description, 200);
            ValidationUtil.validatePositiveAmount(startingPrice, "Starting price");
        }
        catch (ValidationException e){
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        if (!endTime.isAfter(startTime)) return new Response(RequestStatus.FAILED, "End time must be after start time");

        try{
            Auction auction = new Auction(seller, auctionName, description, startingPrice, startTime, endTime);
            if (!auctionRepository.save(auction)) throw new DatabaseException("Failed to save auction");
        }
        catch (DatabaseException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }

        return new Response(RequestStatus.SUCCESS, "Create new auction successfully!");
    }

    public Response updateAuction(ClientHandler client, Request request){
        UpdateAuctionRequest data = JsonUtil.fromMap(request.getData(), UpdateAuctionRequest.class);
        if (data == null) return new Response(request, RequestStatus.INVALID_REQUEST, "Invalid request data");

        String auctionId = data.getAuctionId();
        try {
            ValidationUtil.requiresNonBlank(auctionId, "Auction ID");
        } catch (ValidationException e) {
            return new Response(request, RequestStatus.FAILED, e.getMessage());
        }

        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) return new Response(request, RequestStatus.NOT_FOUND, "Auction not found");

        if (!auction.getSeller().equals(client.getCurrentUsername()))
            return new Response(request, RequestStatus.UNAUTHORIZED, "Only the seller can update this auction");

        // chỉ cho phép update trước khi bắt đầu đấu giá
        if (auction.getStatus() != AuctionStatus.UPCOMING)
            return new Response(request, RequestStatus.FAILED, "Can only update auction before it starts");

        // validate và cập nhật các field
        String auctionName = data.getAuctionName();
        String description = data.getDescription();
        double startingPrice = data.getStartingPrice();
        LocalDateTime startTime, endTime;

        try {
            startTime = LocalDateTime.parse(data.getStartTime());
            endTime = LocalDateTime.parse(data.getEndTime());
        } catch (DateTimeParseException e) {
            return new Response(request, RequestStatus.FAILED, "Invalid date time format");
        }

        try {
            ValidationUtil.requiresNonBlank(auctionName, "Auction's name");
            ValidationUtil.requiresNonBlank(description, "Description");
            ValidationUtil.validateMaxLength("Description", description, 200);
            ValidationUtil.validatePositiveAmount(startingPrice, "Starting price");
        } catch (ValidationException e) {
            return new Response(request, RequestStatus.FAILED, e.getMessage());
        }

        if (!endTime.isAfter(startTime))
            return new Response(request, RequestStatus.FAILED, "End time must be after start time");

        auction.setAuctionName(auctionName);
        auction.setDescription(description);
        auction.setStartingPrice(startingPrice);
        auction.setStartTime(startTime);
        auction.setEndTime(endTime);

        try {
            if (!auctionRepository.update(auction)) throw new DatabaseException("Failed to update auction");
        } catch (DatabaseException e) {
            return new Response(request, RequestStatus.FAILED, e.getMessage());
        }

        // gửi tin nhắn cho người tham gia nếu có message
        String message = data.getMessage();
        try {
            ValidationUtil.requiresNonBlank(message, "Message");
        } catch (ValidationException e) { return new Response(request, RequestStatus.FAILED, e.getMessage()); }

        return new Response(request, RequestStatus.SUCCESS, "Auction updated successfully!");
    }
}
// CREATE_AUCTION, // tạo đấu giá
// UPDATE_AUCTION, // sửa lại cuộc đấu giá trước khi bắt đầu
// GET_AUCTION_DETAIL, // xem chi tiết cuộc đấu giá
// DELETE_AUCTION, // xóa cuộc đấu giá
/*
important not null:
id
auction name
description
starting price
seller
startTime
endTime

*/
