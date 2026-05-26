package com.bidify;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainAppTest {
    @Test
    void calculateHeightForWidthPreservesSixteenByNineRatio() {
        assertEquals(900.0, MainApp.calculateHeightForWidth(1600.0));
    }

    @Test
    void calculateWidthForHeightPreservesSixteenByNineRatio() {
        assertEquals(1600.0, MainApp.calculateWidthForHeight(900.0));
    }
}
