package com.shannon.soaksafe.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MaintenanceEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(MaintenanceEvent event);

    @Update
    void update(MaintenanceEvent event);

    @Query("SELECT * FROM maintenance_events WHERE id = :id LIMIT 1")
    MaintenanceEvent getByIdSync(long id);

    @Query("DELETE FROM maintenance_events WHERE id = :id AND userId = :userId")
    void deleteByIdForUser(long id, long userId);

    @Query("SELECT * FROM maintenance_events WHERE userId = :userId ORDER BY event_time_millis DESC, id DESC")
    List<MaintenanceEvent> listByUserIdSync(long userId);
}
