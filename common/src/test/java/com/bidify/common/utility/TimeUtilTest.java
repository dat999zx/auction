package com.bidify.common.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class TimeUtilTest {
    @Test
    void vietnamZoneIsStable() {
        assertEquals(ZoneId.of("Asia/Ho_Chi_Minh"), TimeUtil.VIETNAM_ZONE);
    }

    @Test
    void nowInVietnamReturnsLocalDateTime() {
        LocalDateTime now = TimeUtil.nowInVietnam();

        assertNotNull(now);
        assertTrue(Math.abs(Duration.between(
                LocalDateTime.now(TimeUtil.VIETNAM_ZONE),
                now
        ).toSeconds()) < 2);
    }

    @Test
    void parsesIsoLocalDateTimeString() {
        assertEquals(
                LocalDateTime.of(2026, 5, 21, 9, 30),
                TimeUtil.parseDateTime("2026-05-21T09:30:00")
        );
    }

    @Test
    void convertsLocalDateTimeToVietnamEpochSeconds() {
        LocalDateTime value = LocalDateTime.of(2026, 5, 21, 9, 0);

        long epoch = TimeUtil.toVietnamEpochSeconds(value);

        assertEquals(
                value.atZone(TimeUtil.VIETNAM_ZONE).toEpochSecond(),
                epoch
        );
    }

    @Test
    void convertsVietnamEpochSecondsToLocalDateTime() {
        LocalDateTime value = LocalDateTime.of(2026, 5, 21, 9, 0);
        long epoch = value.atZone(TimeUtil.VIETNAM_ZONE).toEpochSecond();

        assertEquals(value, TimeUtil.fromVietnamEpochSeconds(epoch));
    }

    @Test
    void formatDateTimeAcceptsIsoLocalVietnamTime() {
        assertEquals(
                "2026-05-21 09:30:00",
                DisplayUtil.formatDateTime("2026-05-21T09:30:00", "Unknown")
        );
    }
}
