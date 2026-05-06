package com.wgu.d424.soaksafe.data;

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
}
