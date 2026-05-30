package com.bidify.ui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

// cập nhật định kì UI sau mỗi 1 khoảng thời gian nhất định
public final class UiUpdateScheduler {
    // thời gian cập nhật
    private static final Duration UPDATE_INTERVAL = Duration.seconds(1);
    private static final UiUpdateScheduler INSTANCE = new UiUpdateScheduler();

    // lưu các task đã đăng kí
    private final Map<String, Runnable> tasks = new ConcurrentHashMap<>();
    private Timeline timeline;
    // dùng Timeline mà ko dùng ScheduledExecutorService như trong server vì Timeline trong JavaFX thread, chuyên để update UI

    private UiUpdateScheduler() {}

    public static UiUpdateScheduler getInstance() { return INSTANCE; }

    public String subscribe(Runnable task) {
        if (task == null)
            throw new IllegalArgumentException("Task cannot be null");

        String taskId = UUID.randomUUID().toString();
        tasks.put(taskId, task);

        Platform.runLater(() -> {
            ensureTimelineStarted();
            task.run();
        });

        return taskId;
    }

    public void unsubscribe(String taskId) {
        if (taskId == null || taskId.isBlank())
            return;

        tasks.remove(taskId);
        Platform.runLater(this::stopTimelineIfIdle);
    }

    private void ensureTimelineStarted() {
        if (timeline != null) {
            if (timeline.getStatus() != Timeline.Status.RUNNING)
                timeline.play();
            return;
        }

        timeline = new Timeline(new KeyFrame(UPDATE_INTERVAL, event -> runTasks()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void runTasks() {
        for (Runnable task : tasks.values()) task.run();

        stopTimelineIfIdle();
    }

    private void stopTimelineIfIdle() {
        if (!tasks.isEmpty() || timeline == null)
            return;

        timeline.stop();
        timeline = null;
    }
}
