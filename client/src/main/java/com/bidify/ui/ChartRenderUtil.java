package com.bidify.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import com.bidify.common.utility.TimeUtil;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

/**
 * Shared chart utilities for auction analytics controllers.
 * Eliminates duplicate chart helper code in AuctionDetailsController and HistoryController.
 */
public class ChartRenderUtil {
    private static final DateTimeFormatter CHART_SHORT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter CHART_FULL_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM HH:mm");
    private static final DateTimeFormatter CHART_DAY_FORMATTER = DateTimeFormatter.ofPattern("dd MMM");
    private static final DateTimeFormatter CHART_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    private ChartRenderUtil() {}

    public static Tooltip createDetailedTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.getStyleClass().add("chart-tooltip");
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(300);
        return tooltip;
    }

    public static void attachTooltip(Node node, Tooltip tooltip) {
        node.setOnMouseEntered(event -> tooltip.show(node, event.getScreenX() + 14, event.getScreenY() + 14));
        node.setOnMouseMoved(event -> {
            if (tooltip.isShowing()) {
                tooltip.setAnchorX(event.getScreenX() + 14);
                tooltip.setAnchorY(event.getScreenY() + 14);
            }
        });
        node.setOnMouseExited(event -> tooltip.hide());
    }

    public static StringConverter<Number> createTimeAxisFormatter(LocalDateTime firstTime, LocalDateTime lastTime) {
        DateTimeFormatter formatter = resolveTimeAxisFormatter(firstTime, lastTime);
        return new StringConverter<>() {
            @Override
            public String toString(Number value) {
                LocalDateTime dateTime = fromEpochSeconds(value.longValue());
                return dateTime == null ? "" : dateTime.format(formatter);
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        };
    }

    public static DateTimeFormatter resolveTimeAxisFormatter(LocalDateTime firstTime, LocalDateTime lastTime) {
        if (firstTime == null || lastTime == null) {
            return CHART_SHORT_TIME_FORMATTER;
        }
        long totalHours = Math.abs(ChronoUnit.HOURS.between(firstTime, lastTime));
        long totalDays = Math.abs(ChronoUnit.DAYS.between(firstTime.toLocalDate(), lastTime.toLocalDate()));
        if (totalHours < 24) return CHART_SHORT_TIME_FORMATTER;
        if (totalDays <= 14) return CHART_DAY_FORMATTER;
        if (totalDays <= 90) return CHART_FULL_TIME_FORMATTER;
        return CHART_YEAR_FORMATTER;
    }

    public static long toEpochSeconds(LocalDateTime dateTime) {
        return dateTime == null ? 0L : TimeUtil.toVietnamEpochSeconds(dateTime);
    }

    public static LocalDateTime fromEpochSeconds(long epochSeconds) {
        try {
            return TimeUtil.fromVietnamEpochSeconds(epochSeconds);
        } catch (Exception e) {
            return null;
        }
    }
}
