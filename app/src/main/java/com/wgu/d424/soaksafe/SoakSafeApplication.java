package com.wgu.d424.soaksafe;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.wgu.d424.soaksafe.data.MaintenanceRepository;
import com.wgu.d424.soaksafe.data.SoakSafeDatabase;
import com.wgu.d424.soaksafe.data.SoakSafeDatabaseMigrations;
import com.wgu.d424.soaksafe.data.UserRepository;

public class SoakSafeApplication extends Application {

    private UserRepository userRepository;
    private MaintenanceRepository maintenanceRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        SoakSafeDatabase db = Room.databaseBuilder(
                getApplicationContext(),
                SoakSafeDatabase.class,
                "soaksafe.db"
        )
                .addMigrations(
                        SoakSafeDatabaseMigrations.MIGRATION_1_2,
                        SoakSafeDatabaseMigrations.MIGRATION_2_3
                )
                .build();
        userRepository = new UserRepository(db.userDao());
        maintenanceRepository = new MaintenanceRepository(
                db.maintenanceDetailDao(),
                db.maintenanceTaskCompletionDao()
        );
    }

    @NonNull
    public UserRepository getUserRepository() {
        return userRepository;
    }

    @NonNull
    public MaintenanceRepository getMaintenanceRepository() {
        return maintenanceRepository;
    }
}
