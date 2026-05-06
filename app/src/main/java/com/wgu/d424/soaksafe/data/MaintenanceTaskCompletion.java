package com.wgu.d424.soaksafe.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/**
 * Records that a task was completed on a given calendar day.
 * Primary key is {@code userId} + {@code taskKey} + {@code completedDayMillis} (local start-of-day).
 */
@Entity(
        tableName = "maintenance_task_completions",
        primaryKeys = {"userId", "taskKey", "completedDayMillis"},
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("userId")}
)
public class MaintenanceTaskCompletion {

    private long userId;

    @NonNull
    private String taskKey = "";

    /**
     * Local start-of-day millis for the day this completion counts toward (part of composite PK).
     */
    private long completedDayMillis;

    /**
     * Actual wall-clock time when the user checked the task (for “Last: 2 hours ago”).
     */
    private long completedAtMillis;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @NonNull
    public String getTaskKey() {
        return taskKey;
    }

    public void setTaskKey(@NonNull String taskKey) {
        this.taskKey = taskKey;
    }

    public long getCompletedDayMillis() {
        return completedDayMillis;
    }

    public void setCompletedDayMillis(long completedDayMillis) {
        this.completedDayMillis = completedDayMillis;
    }

    public long getCompletedAtMillis() {
        return completedAtMillis;
    }

    public void setCompletedAtMillis(long completedAtMillis) {
        this.completedAtMillis = completedAtMillis;
    }
}
