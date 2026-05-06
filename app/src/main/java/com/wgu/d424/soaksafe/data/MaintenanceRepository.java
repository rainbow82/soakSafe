package com.wgu.d424.soaksafe.data;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wgu.d424.soaksafe.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MaintenanceRepository {

    public interface DateCallback {
        void onDate(@Nullable Long dateMillisUtc);
    }

    public interface TaskRowsCallback {
        void onRows(@NonNull List<TaskRowState> rows, int completedTodayCount, int totalTasks);
    }

    public static final class TaskRowState {
        public final String taskKey;
        public final boolean completedToday;
        @Nullable
        public final Long lastCompletedAtMillis;

        public TaskRowState(
                @NonNull String taskKey,
                boolean completedToday,
                @Nullable Long lastCompletedAtMillis
        ) {
            this.taskKey = taskKey;
            this.completedToday = completedToday;
            this.lastCompletedAtMillis = lastCompletedAtMillis;
        }
    }

    private final MaintenanceDetailDao maintenanceDetailDao;
    private final MaintenanceTaskCompletionDao taskCompletionDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public MaintenanceRepository(
            @NonNull MaintenanceDetailDao maintenanceDetailDao,
            @NonNull MaintenanceTaskCompletionDao taskCompletionDao
    ) {
        this.maintenanceDetailDao = maintenanceDetailDao;
        this.taskCompletionDao = taskCompletionDao;
    }

    public void getSavedDateMillis(long userId, @NonNull DateCallback callback) {
        executor.execute(() -> {
            MaintenanceDetail row = maintenanceDetailDao.getByUserIdSync(userId);
            Long value = row != null ? row.getDateMillis() : null;
            mainHandler.post(() -> callback.onDate(value));
        });
    }

    public void saveDateMillis(long userId, long dateMillisUtc, @NonNull Runnable onSaved) {
        executor.execute(() -> {
            MaintenanceDetail detail = new MaintenanceDetail();
            detail.setUserId(userId);
            detail.setDateMillis(dateMillisUtc);
            maintenanceDetailDao.upsert(detail);
            mainHandler.post(onSaved);
        });
    }

    public void loadTaskRows(long userId, @NonNull TaskRowsCallback callback) {
        executor.execute(() -> {
            long todayStart = TimeUtil.startOfLocalDayMillis(System.currentTimeMillis());
            List<TaskRowState> rows = new ArrayList<>();
            int doneToday = 0;
            for (MaintenanceTasksCatalog.Entry entry : MaintenanceTasksCatalog.ENTRIES) {
                MaintenanceTaskCompletion onDay = taskCompletionDao.getForTaskOnDay(
                        userId,
                        entry.key,
                        todayStart
                );
                boolean today = onDay != null;
                if (today) {
                    doneToday++;
                }
                MaintenanceTaskCompletion latest = taskCompletionDao.getLatestForTask(userId, entry.key);
                Long lastAt = latest != null ? latest.getCompletedAtMillis() : null;
                rows.add(new TaskRowState(entry.key, today, lastAt));
            }
            int total = MaintenanceTasksCatalog.ENTRIES.size();
            int finalDone = doneToday;
            mainHandler.post(() -> callback.onRows(rows, finalDone, total));
        });
    }

    public void setTaskCompletedToday(
            long userId,
            @NonNull String taskKey,
            boolean completed,
            @NonNull Runnable onDone
    ) {
        executor.execute(() -> {
            long todayStart = TimeUtil.startOfLocalDayMillis(System.currentTimeMillis());
            if (completed) {
                MaintenanceTaskCompletion row = new MaintenanceTaskCompletion();
                row.setUserId(userId);
                row.setTaskKey(taskKey);
                row.setCompletedDayMillis(todayStart);
                row.setCompletedAtMillis(System.currentTimeMillis());
                taskCompletionDao.insert(row);
            } else {
                taskCompletionDao.deleteForTaskOnDay(userId, taskKey, todayStart);
            }
            mainHandler.post(onDone);
        });
    }

    /**
     * Removes the most recent completion row for this task (any day), for the trash control.
     */
    public void deleteLatestTaskCompletion(
            long userId,
            @NonNull String taskKey,
            @NonNull Runnable onDone
    ) {
        executor.execute(() -> {
            MaintenanceTaskCompletion latest = taskCompletionDao.getLatestForTask(userId, taskKey);
            if (latest != null) {
                taskCompletionDao.delete(latest);
            }
            mainHandler.post(onDone);
        });
    }
}
