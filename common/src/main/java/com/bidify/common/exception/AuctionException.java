package com.bidify.common.exception;

public class AuctionException extends RuntimeException{
    // dùng để tạo một đối tượng AuctionException
    public AuctionException(String message){
        // dùng để super
        super(message);
    }
}
