package com.shannon.soaksafe;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.room.Room;

import com.shannon.soaksafe.data.MaintenanceRepository;
import com.shannon.soaksafe.data.SoakSafeDatabase;
import com.shannon.soaksafe.data.SoakSafeDatabaseMigrations;
import com.shannon.soaksafe.data.UserRepository;
import com.shannon.soaksafe.util.AppIconManager;

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
                        SoakSafeDatabaseMigrations.MIGRATION_2_3,
                        SoakSafeDatabaseMigrations.MIGRATION_3_4,
                        SoakSafeDatabaseMigrations.MIGRATION_4_5,
                        SoakSafeDatabaseMigrations.MIGRATION_5_6,
                        SoakSafeDatabaseMigrations.MIGRATION_6_7,
                        SoakSafeDatabaseMigrations.MIGRATION_7_8,
                        SoakSafeDatabaseMigrations.MIGRATION_8_9
                )
                .build();
        userRepository = new UserRepository(db.userDao());
        maintenanceRepository = new MaintenanceRepository(
                db.maintenanceDetailDao(),
                db.maintenanceChecklistDao(),
                db.maintenanceEventDao(),
                this
        );

        AppIconManager.schedulePeriodicRefresh(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                AppIconManager.onAppForeground(SoakSafeApplication.this);
            }
        });
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
