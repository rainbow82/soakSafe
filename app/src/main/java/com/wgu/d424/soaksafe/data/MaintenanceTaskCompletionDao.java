package com.wgu.d424.soaksafe.data;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface MaintenanceTaskCompletionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(@NonNull MaintenanceTaskCompletion row);

    @Delete
    void delete(@NonNull MaintenanceTaskCompletion row);

    @Query(
            "DELETE FROM maintenance_task_completions WHERE userId = :userId AND taskKey = :taskKey "
                    + "AND completedDayMillis = :dayMillis"
    )
    void deleteForTaskOnDay(long userId, @NonNull String taskKey, long dayMillis);

    @Query(
            "SELECT * FROM maintenance_task_completions WHERE userId = :userId AND taskKey = :taskKey "
                    + "AND completedDayMillis = :dayMillis LIMIT 1"
    )
    MaintenanceTaskCompletion getForTaskOnDay(long userId, @NonNull String taskKey, long dayMillis);

    @Query(
            "SELECT * FROM maintenance_task_completions WHERE userId = :userId AND taskKey = :taskKey "
                    + "ORDER BY completedDayMillis DESC, completedAtMillis DESC LIMIT 1"
    )
    MaintenanceTaskCompletion getLatestForTask(long userId, @NonNull String taskKey);
}
