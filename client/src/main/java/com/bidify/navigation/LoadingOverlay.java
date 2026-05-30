package com.bidify.navigation;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

final class LoadingOverlay {
    private final StackPane loadingNode = new StackPane();
    private RotateTransition spinAnimation;

    StackPane node() {
        return loadingNode;
    }

    void initialize() {
        Circle background = new Circle(26);
        background.setFill(Color.rgb(255, 255, 255, 0.92));
        background.setEffect(new DropShadow(20, Color.rgb(15, 23, 42, 0.18)));

        Arc arc = new Arc(0, 0, 18, 18, 20, 280);
        arc.setType(ArcType.OPEN);
        arc.setStrokeLineCap(StrokeLineCap.ROUND);
        arc.setStrokeWidth(4);
        arc.setFill(null);
        arc.setStroke(new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2563eb")),
                new Stop(1, Color.web("#14b8a6"))));

        loadingNode.getChildren().setAll(background, arc);
        loadingNode.setOpacity(0);
        loadingNode.setTranslateY(-450);
        loadingNode.setVisible(false);
        loadingNode.setManaged(false);
        StackPane.setAlignment(loadingNode, Pos.TOP_CENTER);
        StackPane.setMargin(loadingNode, new Insets(10, 0, 0, 0));

        spinAnimation = new RotateTransition(Duration.millis(900), loadingNode);
        spinAnimation.setByAngle(360);
        spinAnimation.setCycleCount(RotateTransition.INDEFINITE);
        spinAnimation.setInterpolator(Interpolator.LINEAR);
    }

    void show() {
        loadingNode.setVisible(true);
        loadingNode.setManaged(true);

        spinAnimation.playFromStart();

        FadeTransition fade = new FadeTransition(Duration.millis(100), loadingNode);
        fade.setFromValue(loadingNode.getOpacity());
        fade.setToValue(1);

        TranslateTransition drop = new TranslateTransition(Duration.millis(150), loadingNode);
        drop.setFromY(loadingNode.getTranslateY());
        drop.setToY(-350);
        drop.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, drop).play();
    }

    void hide(Runnable onFinishedCallback) {
        FadeTransition fade = new FadeTransition(Duration.millis(50), loadingNode);
        fade.setFromValue(loadingNode.getOpacity());
        fade.setToValue(0);

        TranslateTransition rise = new TranslateTransition(Duration.millis(120), loadingNode);
        rise.setFromY(loadingNode.getTranslateY());
        rise.setToY(-450);
        rise.setInterpolator(Interpolator.EASE_IN);

        ParallelTransition hideTransition = new ParallelTransition(fade, rise);
        hideTransition.setOnFinished(event -> {
            spinAnimation.stop();
            loadingNode.setVisible(false);
            loadingNode.setManaged(false);
            loadingNode.setOpacity(0);
            loadingNode.setTranslateY(-450);
            if (onFinishedCallback != null) {
                onFinishedCallback.run();
            }
        });
        hideTransition.play();
    }
}
