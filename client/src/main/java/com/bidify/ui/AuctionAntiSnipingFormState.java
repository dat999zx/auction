package com.bidify.ui;

import java.time.LocalDateTime;
import java.util.Objects;

public final class AuctionAntiSnipingFormState {
    private final boolean enabled;
    private final boolean maxEndBeforeStandardEnd;
    private final String labelText;
    private final String style;

    private static final String ENABLED_TEXT = "Anti-Sniping Status: Enabled";
    private static final String DISABLED_TEXT = "Anti-Sniping Status: Disabled";
    private static final String ENABLED_STYLE = "-fx-text-fill: #2ecc71; -fx-font-weight: bold;";
    private static final String DISABLED_STYLE = "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";

    public AuctionAntiSnipingFormState(
            boolean enabled,
            boolean maxEndBeforeStandardEnd,
            String labelText,
            String style) {
        this.enabled = enabled;
        this.maxEndBeforeStandardEnd = maxEndBeforeStandardEnd;
        this.labelText = labelText;
        this.style = style;
    }

    public static AuctionAntiSnipingFormState from(LocalDateTime standardEnd, LocalDateTime maxEnd) {
        boolean before = maxEnd.isBefore(standardEnd);
        boolean enabled = maxEnd.isAfter(standardEnd);
        return new AuctionAntiSnipingFormState(
                enabled,
                before,
                enabled ? ENABLED_TEXT : DISABLED_TEXT,
                enabled ? ENABLED_STYLE : DISABLED_STYLE);
    }

    public static AuctionAntiSnipingFormState disabled() {
        return new AuctionAntiSnipingFormState(false, false, DISABLED_TEXT, DISABLED_STYLE);
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean maxEndBeforeStandardEnd() {
        return maxEndBeforeStandardEnd;
    }

    public String labelText() {
        return labelText;
    }

    public String style() {
        return style;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuctionAntiSnipingFormState that = (AuctionAntiSnipingFormState) o;
        return enabled == that.enabled &&
                maxEndBeforeStandardEnd == that.maxEndBeforeStandardEnd &&
                Objects.equals(labelText, that.labelText) &&
                Objects.equals(style, that.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, maxEndBeforeStandardEnd, labelText, style);
    }

    @Override
    public String toString() {
        return "AuctionAntiSnipingFormState{" +
                "enabled=" + enabled +
                ", maxEndBeforeStandardEnd=" + maxEndBeforeStandardEnd +
                ", labelText='" + labelText + '\'' +
                ", style='" + style + '\'' +
                '}';
    }
}
