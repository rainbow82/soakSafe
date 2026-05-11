package com.shannon.soaksafe.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface MaintenanceChecklistDao {

    @Query("SELECT * FROM maintenance_checklist WHERE userId = :userId LIMIT 1")
    MaintenanceChecklist getByUserIdSync(long userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(MaintenanceChecklist row);
}
