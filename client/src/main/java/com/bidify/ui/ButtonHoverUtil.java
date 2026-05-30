package com.bidify.ui;

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

    private ButtonHoverUtil() {}

    public static void apply(Parent root) {
        if (root == null) return;
        applyOnNode(root);
    }

    private static void applyOnNode(Node node) {
        if (node == null) return;

        if (node instanceof Button button)
            applyOnButton(button);

        if (node instanceof ScrollPane scrollPane)
            applyOnNode(scrollPane.getContent());

        if (!(node instanceof Parent parent)) return;

        for (Node child : parent.getChildrenUnmodifiable())
            applyOnNode(child);
    }

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
            animateScale(button, PRESSED_SCALE, PRESS_DURATION);
        });

        button.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            animateScale(button, getRestingScale(button), HOVER_DURATION);
        });

        button.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() != MouseButton.PRIMARY) return;
            animateClickPop(button);
        });
    }

    private static void animateScale(Button button, double targetScale, Duration duration) {
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

    private static void animateClickPop(Button button) {
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

    private static void stopActiveAnimation(Button button) {
        Object existing = button.getProperties().get(ACTIVE_TRANSITION_KEY);
        if (existing instanceof Animation animation)
            animation.stop();
    }

    private static double getRestingScale(Button button) {
        return Boolean.TRUE.equals(button.getProperties().get(HOVERED_KEY)) ? HOVER_SCALE : 1.0;
    }
}
