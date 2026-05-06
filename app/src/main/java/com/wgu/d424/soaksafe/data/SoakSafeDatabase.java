package com.wgu.d424.soaksafe.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(
        entities = {User.class, MaintenanceDetail.class, MaintenanceTaskCompletion.class},
        version = 3,
        exportSchema = false
)
public abstract class SoakSafeDatabase extends RoomDatabase {

    public abstract UserDao userDao();

    public abstract MaintenanceDetailDao maintenanceDetailDao();

    public abstract MaintenanceTaskCompletionDao maintenanceTaskCompletionDao();
}
