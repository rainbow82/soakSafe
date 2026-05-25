package com.shannon.soaksafe.data;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public final class SoakSafeDatabaseMigrations {

    private SoakSafeDatabaseMigrations() {
    }

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `maintenance_details` "
                            + "(`userId` INTEGER NOT NULL, `dateMillis` INTEGER NOT NULL, "
                            + "PRIMARY KEY(`userId`), "
                            + "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) "
                            + "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `maintenance_task_completions` "
                            + "(`userId` INTEGER NOT NULL, `taskKey` TEXT NOT NULL, "
                            + "`completedDayMillis` INTEGER NOT NULL, `completedAtMillis` INTEGER NOT NULL, "
                            + "PRIMARY KEY(`userId`, `taskKey`, `completedDayMillis`), "
                            + "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) "
                            + "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_task_completions_userId` "
                    + "ON `maintenance_task_completions` (`userId`)");
        }
    };

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS `maintenance_task_completions`");
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `maintenance_checklist` ("
                            + "`userId` INTEGER NOT NULL, "
                            + "`vacuum` INTEGER NOT NULL, "
                            + "`clean_skimmer` INTEGER NOT NULL, "
                            + "`add_water` INTEGER NOT NULL, "
                            + "`brush_walls` INTEGER NOT NULL, "
                            + "`chlorine` REAL NOT NULL, "
                            + "`ph_up` REAL NOT NULL, "
                            + "`ph_down` REAL NOT NULL, "
                            + "`no_phos` REAL NOT NULL, "
                            + "PRIMARY KEY(`userId`), "
                            + "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) "
                            + "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `maintenance_events` ("
                            + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "`userId` INTEGER NOT NULL, "
                            + "`event_type` TEXT NOT NULL, "
                            + "`event_time_millis` INTEGER NOT NULL, "
                            + "`dateMillis` INTEGER NOT NULL, "
                            + "`vacuum` INTEGER NOT NULL, "
                            + "`clean_skimmer` INTEGER NOT NULL, "
                            + "`add_water` INTEGER NOT NULL, "
                            + "`brush_walls` INTEGER NOT NULL, "
                            + "`chlorine` REAL NOT NULL, "
                            + "`ph_up` REAL NOT NULL, "
                            + "`ph_down` REAL NOT NULL, "
                            + "`no_phos` REAL NOT NULL, "
                            + "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) "
                            + "ON UPDATE NO ACTION ON DELETE CASCADE)"
            );
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_events_userId` "
                    + "ON `maintenance_events` (`userId`)");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_events_userId` "
                    + "ON `maintenance_events` (`userId`)");
        }
    };

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE maintenance_events ADD COLUMN line_items_json TEXT");
        }
    };

    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE maintenance_checklist ADD COLUMN custom_lines_json TEXT");
        }
    };

    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE users ADD COLUMN pool_size_gallons INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE users ADD COLUMN pool_salt_water INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE users ADD COLUMN pool_above_ground INTEGER NOT NULL DEFAULT 0");
        }
    };
}
