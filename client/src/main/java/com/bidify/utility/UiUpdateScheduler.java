package com.bidify.utility;

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

    // dùng để tạo một đối tượng UiUpdateScheduler
    private UiUpdateScheduler() {}

    // dùng để lấy đối tượng Singleton
    public static UiUpdateScheduler getInstance() { return INSTANCE; }

    // đăng kí cho task chạy sau mỗi UPDATE_INTERVAL giây (vd: task update thời gian)
    // dùng để đăng ký lắng nghe sự kiện
    public String subscribe(Runnable task) {
        if (task == null)
            throw new IllegalArgumentException("Task cannot be null");

        String taskId = UUID.randomUUID().toString();
        tasks.put(taskId, task);

        Platform.runLater(() -> {
            // dùng để ensure timeline started
            ensureTimelineStarted();
            task.run();
        });

        return taskId;
    }

    // hủy đăng kí task
    // dùng để hủy đăng ký lắng nghe sự kiện
    public void unsubscribe(String taskId) {
        if (taskId == null || taskId.isBlank())
            return;

        tasks.remove(taskId);
        Platform.runLater(this::stopTimelineIfIdle);
    }

    // đảm bảo timeline chạy, nếu chưa chạy thì bắt đầu
    // dùng để ensure timeline started
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

    // chạy các task đã đăng kí
    // dùng để chạy tasks
    private void runTasks() {
        for (Runnable task : tasks.values()) task.run();

        // dùng để dừng timeline if idle
        stopTimelineIfIdle();
    }

    // nếu không có task nào thì dừng timeline
    // dùng để dừng timeline if idle
    private void stopTimelineIfIdle() {
        if (!tasks.isEmpty() || timeline == null)
            return;

        timeline.stop();
        timeline = null;
    }
}
