package com.wgu.d424.soaksafe.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MaintenanceEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    void insert(MaintenanceEvent event);

    @Query("SELECT * FROM maintenance_events WHERE userId = :userId ORDER BY event_time_millis DESC, id DESC")
    List<MaintenanceEvent> listByUserIdSync(long userId);
}
