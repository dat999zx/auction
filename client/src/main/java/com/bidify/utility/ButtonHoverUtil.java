package com.bidify.utility;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

public final class ButtonHoverUtil {
    static final String PROCESSED_KEY = "button.hover.processed";
    static final String ACTIVE_TRANSITION_KEY = "button.hover.active.animation";
    static final String HOVERED_KEY = "button.hover.hovered";

    private static final Duration HOVER_DURATION = Duration.millis(140);
    private static final Duration PRESS_DURATION = Duration.millis(85);
    private static final Duration POP_UP_DURATION = Duration.millis(70);
    private static final Duration POP_DOWN_DURATION = Duration.millis(110);

    private static final double HOVER_SCALE = 1.04;
    private static final double PRESSED_SCALE = 0.97;
    private static final double POP_SCALE = 1.08;

    // dùng để tạo một đối tượng ButtonHoverUtil
    private ButtonHoverUtil() {}

    // dùng để áp dụng
    public static void apply(Parent root) {
        if (root == null) return;
        // dùng để áp dụng xử lý sự kiện node
        applyOnNode(root);
    }

    // dùng để áp dụng xử lý sự kiện node
    private static void applyOnNode(Node node) {
        if (node == null) return;

        if (node instanceof Button button)
            // dùng để áp dụng xử lý sự kiện nút nhấn
            applyOnButton(button);

        if (node instanceof ScrollPane scrollPane)
            applyOnNode(scrollPane.getContent());

        if (!(node instanceof Parent parent)) return;

        for (Node child : parent.getChildrenUnmodifiable())
            // dùng để áp dụng xử lý sự kiện node
            applyOnNode(child);
    }

    // dùng để áp dụng xử lý sự kiện nút nhấn
    private static void applyOnButton(Button button) {
        if (Boolean.TRUE.equals(button.getProperties().get(PROCESSED_KEY))) return;

        button.getProperties().put(PROCESSED_KEY, Boolean.TRUE);
        button.getProperties().put(HOVERED_KEY, Boolean.FALSE);

        button.addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
            button.getProperties().put(HOVERED_KEY, Boolean.TRUE);
            if (!button.isPressed()) animateScale(button, HOVER_SCALE, HOVER_DURATION);
        });

        button.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            button.getProperties().put(HOVERED_KEY, Boolean.FALSE);
            if (!button.isPressed()) animateScale(button, 1.0, HOVER_DURATION);
        });

        button.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            // dùng để tạo hiệu ứng chuyển động scale
            animateScale(button, PRESSED_SCALE, PRESS_DURATION);
        });

        button.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            animateScale(button, getRestingScale(button), HOVER_DURATION);
        });

        button.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            // dùng để tạo hiệu ứng chuyển động click pop
            animateClickPop(button);
        });
    }

    // dùng để tạo hiệu ứng chuyển động scale
    private static void animateScale(Button button, double targetScale, Duration duration) {
        // dùng để dừng active animation
        stopActiveAnimation(button);

        ScaleTransition transition = new ScaleTransition(duration, button);
        transition.setFromX(button.getScaleX());
        transition.setFromY(button.getScaleY());
        transition.setToX(targetScale);
        transition.setToY(targetScale);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.setOnFinished(event -> button.getProperties().remove(ACTIVE_TRANSITION_KEY));

        button.getProperties().put(ACTIVE_TRANSITION_KEY, transition);
        transition.playFromStart();
    }

    // dùng để tạo hiệu ứng chuyển động click pop
    private static void animateClickPop(Button button) {
        // dùng để dừng active animation
        stopActiveAnimation(button);

        double currentScaleX = button.getScaleX();
        double currentScaleY = button.getScaleY();
        double restingScale = getRestingScale(button);

        ScaleTransition popUp = new ScaleTransition(POP_UP_DURATION, button);
        popUp.setFromX(currentScaleX);
        popUp.setFromY(currentScaleY);
        popUp.setToX(POP_SCALE);
        popUp.setToY(POP_SCALE);
        popUp.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition popDown = new ScaleTransition(POP_DOWN_DURATION, button);
        popDown.setFromX(POP_SCALE);
        popDown.setFromY(POP_SCALE);
        popDown.setToX(restingScale);
        popDown.setToY(restingScale);
        popDown.setInterpolator(Interpolator.EASE_BOTH);

        SequentialTransition transition = new SequentialTransition(button, popUp, popDown);
        transition.setOnFinished(event -> button.getProperties().remove(ACTIVE_TRANSITION_KEY));

        button.getProperties().put(ACTIVE_TRANSITION_KEY, transition);
        transition.playFromStart();
    }

    // dùng để dừng active animation
    private static void stopActiveAnimation(Button button) {
        Object existing = button.getProperties().get(ACTIVE_TRANSITION_KEY);
        if (existing instanceof Animation animation)
            animation.stop();
    }

    // dùng để lấy resting scale
    private static double getRestingScale(Button button) {
        return Boolean.TRUE.equals(button.getProperties().get(HOVERED_KEY)) ? HOVER_SCALE : 1.0;
    }
}
