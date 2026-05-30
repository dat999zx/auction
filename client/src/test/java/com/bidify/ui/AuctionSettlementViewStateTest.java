package com.bidify.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AuctionSettlementViewStateTest {
    @Test
    void winnerCanPayAwaitingPaymentAuction() {
        AuctionSettlementViewState state = AuctionSettlementViewState.resolve(
            "AWAITING_PAYMENT", "winner", "seller", "winner", false
        );
        assertTrue(state.showPayButton());
        assertFalse(state.showConfirmDeliveryButton());
    }

    @Test
    void sellerCanConfirmAwaitingDeliveryAuction() {
        AuctionSettlementViewState state = AuctionSettlementViewState.resolve(
            "AWAITING_DELIVERY", "winner", "seller", "seller", false
        );
        assertFalse(state.showPayButton());
        assertTrue(state.showConfirmDeliveryButton());
    }

    @Test
    void adminCanResolveSettlementStates() {
        AuctionSettlementViewState state = AuctionSettlementViewState.resolve(
            "AWAITING_PAYMENT", "winner", "seller", "admin", true
        );
        assertTrue(state.showAdminCancelButton());
    }

    @Test
    void completedAuctionStillShowsSettlementSection() {
        AuctionSettlementViewState state = AuctionSettlementViewState.resolve(
            "COMPLETED", "winner", "seller", "winner", false
        );
        assertTrue(state.showSettlementSection());
        assertFalse(state.showPayButton());
        assertFalse(state.showConfirmDeliveryButton());
    }

    @Test
    void sellerCannotUseCancelButtonDuringAwaitingPayment() {
        AuctionSettlementViewState state = AuctionSettlementViewState.resolve(
            "AWAITING_PAYMENT", "winner", "seller", "seller", false
        );
        assertFalse(state.showSellerCancelButton());
    }
}
