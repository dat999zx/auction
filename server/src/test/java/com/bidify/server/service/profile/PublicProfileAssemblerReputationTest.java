package com.bidify.server.service.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PublicProfileAssemblerReputationTest {
    @Test
    void newSellerHasNewSellerLabel() {
        PublicProfileAssembler.ReputationSummary summary =
                PublicProfileAssembler.ReputationSummary.from(1, 0, 1);

        assertEquals(1, summary.completedSales());
        assertEquals(0, summary.failedSales());
        assertEquals("100.0%", summary.completionRate());
        assertEquals("New Seller", summary.reputationLabel());
        assertEquals(5.0, summary.starRating(), 0.01);
        assertEquals("★★★★★", summary.starVisual());
    }

    @Test
    void reliableSellerHasReliableSellerLabel() {
        PublicProfileAssembler.ReputationSummary summary =
                PublicProfileAssembler.ReputationSummary.from(8, 1, 9);

        assertEquals("88.9%", summary.completionRate());
        assertEquals("Reliable Seller", summary.reputationLabel());
        assertEquals(4.44, summary.starRating(), 0.01);
        assertEquals("★★★★☆", summary.starVisual());
    }

    @Test
    void topSellerRequiresVolumeAndHighCompletionRate() {
        PublicProfileAssembler.ReputationSummary summary =
                PublicProfileAssembler.ReputationSummary.from(19, 1, 20);

        assertEquals("95.0%", summary.completionRate());
        assertEquals("Top Seller", summary.reputationLabel());
        assertEquals(4.75, summary.starRating(), 0.01);
        assertEquals("★★★★★", summary.starVisual());
    }

    @Test
    void lowCompletionSellerNeedsReview() {
        PublicProfileAssembler.ReputationSummary summary =
                PublicProfileAssembler.ReputationSummary.from(2, 2, 4);

        assertEquals("50.0%", summary.completionRate());
        assertEquals("Needs Review", summary.reputationLabel());
        assertEquals(2.5, summary.starRating(), 0.01);
        assertEquals("★★★☆☆", summary.starVisual());
    }
}
