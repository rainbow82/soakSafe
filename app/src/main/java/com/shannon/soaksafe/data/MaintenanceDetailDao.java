package com.shannon.soaksafe.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface MaintenanceDetailDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(MaintenanceDetail detail);

    @Query("SELECT * FROM maintenance_details WHERE userId = :userId LIMIT 1")
    MaintenanceDetail getByUserIdSync(long userId);
}
