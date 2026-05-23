package com.bidify.utility;

import java.util.LinkedList;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class NotificationUtil {
    private static final double MAX_NOTIFICATION_WIDTH = 300;
    private static final Duration ANIM_DURATION = Duration.millis(250);
    private static final Duration DISPLAY_DURATION = Duration.seconds(3);
    private static final int MAX_NOTIFICATIONS = 5;

    private static VBox notificationContainer = null;
    private static final LinkedList<String> activeMessages = new LinkedList<>();

    private NotificationUtil() {}

    public enum NotificationType {
        SUCCESS("#1f7a1f", "#e6f4ea"),
        ERROR("#ba1a1a", "#fce8e6"),
        INFO("#0048d8", "#e7eeff");

        final String textColor;
        final String bgColor;

        NotificationType(String textColor, String bgColor) {
            this.textColor = textColor;
            this.bgColor = bgColor;
        }
    }

    public static void success(String message) {
        show(message, NotificationType.SUCCESS);
    }

    public static void error(String message) {
        show(message, NotificationType.ERROR);
    }

    public static void info(String message) {
        show(message, NotificationType.INFO);
    }

    // hiện notification
    private static void show(String message, NotificationType type) {
        Platform.runLater(() -> {
            StackPane overlay = SceneManager.getOverlayLayer();
            if (overlay == null) return;

            VBox container = getNotificationContainer(overlay);

            // Chống spam: nếu tin nhắn giống hệt cái đang ở trên cùng thì thôi
            if (!activeMessages.isEmpty() && message.equals(activeMessages.peekFirst())) {
                return;
            }

            // Giới hạn số lượng notification cùng lúc
            if (container.getChildren().size() >= MAX_NOTIFICATIONS) {
                Node oldest = container.getChildren().get(container.getChildren().size() - 1);
                hideNotification((HBox) oldest, container, null);
            }

            doShow(message, type, container);
        });
    }

    private static VBox getNotificationContainer(StackPane overlay) {
        if (notificationContainer == null) {
            notificationContainer = new VBox(10); // khoảng cách giữa các thông báo
            notificationContainer.setAlignment(Pos.TOP_RIGHT);
            notificationContainer.setPickOnBounds(false);
            notificationContainer.setMouseTransparent(true);
            notificationContainer.setMaxWidth(MAX_NOTIFICATION_WIDTH + 40);
            notificationContainer.setMaxHeight(Region.USE_COMPUTED_SIZE);
            
            StackPane.setAlignment(notificationContainer, Pos.TOP_RIGHT);
            StackPane.setMargin(notificationContainer, new Insets(95, 15, 0, 0));
            overlay.getChildren().add(notificationContainer);
        }
        return notificationContainer;
    }

    private static void doShow(String message, NotificationType type, VBox container) {
        activeMessages.addFirst(message);
        
        HBox notification = createNotification(message, type);
        
        // Thêm vào đầu VBox để cái mới nhất nằm trên cùng
        container.getChildren().add(0, notification);

        // animation bay vào
        notification.setOpacity(0);
        notification.setTranslateX(20);

        FadeTransition fadeIn = new FadeTransition(ANIM_DURATION, notification);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(ANIM_DURATION, notification);
        slideIn.setToX(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition showAnim = new ParallelTransition(fadeIn, slideIn);
        showAnim.play();

        // Tự động ẩn
        Thread hideThread = new Thread(() -> {
            try {
                Thread.sleep((long) DISPLAY_DURATION.toMillis());
                Platform.runLater(() -> {
                    // Xóa message khỏi queue và ẩn notification
                    hideNotification(notification, container, () -> activeMessages.remove(message));
                });
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        hideThread.setDaemon(true);
        hideThread.start();
    }

    // tạo notification
    private static HBox createNotification(String message, NotificationType type) {
        Label label = new Label(message);
        label.setWrapText(true);
        label.setMaxWidth(MAX_NOTIFICATION_WIDTH - 20);
        label.setStyle("-fx-text-fill: " + type.textColor + "; -fx-font-weight: bold; -fx-font-size: 11px;");

        HBox container = new HBox(label);
        container.setPadding(new Insets(6, 12, 6, 12));
        container.setAlignment(Pos.CENTER_LEFT);
        
        container.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        container.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        
        container.setStyle("-fx-background-color: " + type.bgColor + "; " +
                           "-fx-background-radius: 6; " +
                           "-fx-border-color: " + type.textColor + "; " +
                           "-fx-border-radius: 6; " +
                           "-fx-border-width: 0.8;");

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.08));
        shadow.setRadius(3);
        shadow.setOffsetY(1);
        container.setEffect(shadow);

        return container;
    }

    // ẩn notification
    private static void hideNotification(HBox notification, VBox container, Runnable onFinished) {
        if (!container.getChildren().contains(notification)) return;

        FadeTransition fadeOut = new FadeTransition(ANIM_DURATION, notification);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(ANIM_DURATION, notification);
        slideOut.setToX(30);
        slideOut.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition hideAnim = new ParallelTransition(fadeOut, slideOut);
        hideAnim.setOnFinished(e -> {
            container.getChildren().remove(notification);
            if (onFinished != null) onFinished.run();
        });
        hideAnim.play();
    }
}
