package com.bidify.ui;

import com.bidify.media.SoundUtil;
import com.bidify.navigation.SceneManager;

import java.io.IOException;
import java.util.LinkedList;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class NotificationUtil {
    private static final double MAX_NOTIFICATION_WIDTH = 380;
    private static final Duration ANIM_DURATION = Duration.millis(250);
    private static final Duration DISPLAY_DURATION = Duration.seconds(3);
    private static final int MAX_NOTIFICATIONS = 5;

    private static VBox notificationContainer = null;
    private static final LinkedList<String> activeMessages = new LinkedList<>();

    private NotificationUtil() {}

    public enum NotificationType {
        SUCCESS("Success", "success"),
        ERROR("Error", "error"),
        INFO("Notice", "info");

        final String title;
        final String styleClass;

        NotificationType(String title, String styleClass) {
            this.title = title;
            this.styleClass = styleClass;
        }
    }

    public static void success(String message) {
        show(message, NotificationType.SUCCESS);
    }

    public static void error(String message) {
        SoundUtil.error();
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
            notificationContainer = new VBox(12);
            notificationContainer.setAlignment(Pos.TOP_RIGHT);
            notificationContainer.setPickOnBounds(false);
            notificationContainer.setMouseTransparent(true);
            notificationContainer.setMaxWidth(MAX_NOTIFICATION_WIDTH + 48);
            notificationContainer.setMaxHeight(Region.USE_COMPUTED_SIZE);
            
            StackPane.setAlignment(notificationContainer, Pos.TOP_RIGHT);
            StackPane.setMargin(notificationContainer, new Insets(88, 24, 0, 0));
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
        try {
            FXMLLoader loader = new FXMLLoader(NotificationUtil.class.getResource("/fxml/notification.fxml"));
            HBox notification = loader.load();

            Label title = (Label) loader.getNamespace().get("notificationTitle");
            Label body = (Label) loader.getNamespace().get("notificationMessage");

            title.setText(type.title);
            body.setText(message);
            notification.getStyleClass().add(type.styleClass);

            return notification;
        }
        catch (IOException e) {
            throw new IllegalStateException("Cannot load notification view.", e);
        }
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
