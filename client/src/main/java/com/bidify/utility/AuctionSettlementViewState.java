package com.bidify.utility;

/**
 * Pure record representing visibility of settlement controls in the auction detail view.
 * Helps keep the controller clean of nested UI state selection logic.
 */
public record AuctionSettlementViewState(
        boolean showSettlementSection,
        boolean showPayButton,
        boolean showConfirmDeliveryButton,
        boolean showAdminCompleteButton,
        boolean showAdminCancelButton,
        boolean showSellerCancelButton) {

    public static AuctionSettlementViewState resolve(
            String status,
            String winnerUsername,
            String sellerUsername,
            String currentUsername,
            boolean admin) {
        boolean active = "ACTIVE".equalsIgnoreCase(status);
        boolean awaitingPayment = "AWAITING_PAYMENT".equalsIgnoreCase(status);
        boolean awaitingDelivery = "AWAITING_DELIVERY".equalsIgnoreCase(status);
        boolean completed = "COMPLETED".equalsIgnoreCase(status);
        boolean winner = currentUsername != null && currentUsername.equals(winnerUsername);
        boolean seller = currentUsername != null && currentUsername.equals(sellerUsername);

        boolean showPay = awaitingPayment && winner;
        boolean showConfirmDelivery = awaitingDelivery && (seller || admin);
        boolean showAdminComplete = admin && awaitingDelivery;
        boolean showAdminCancel = admin && (awaitingPayment || awaitingDelivery);
        boolean showSellerCancel = active && (seller || admin);
        boolean showSection = completed || showPay || showConfirmDelivery || showAdminComplete || showAdminCancel;

        return new AuctionSettlementViewState(
            showSection,
            showPay,
            showConfirmDelivery,
            showAdminComplete,
            showAdminCancel,
            showSellerCancel
        );
    }
}
