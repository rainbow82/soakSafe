package com.wgu.d424.soaksafe.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wgu.d424.soaksafe.base.AsyncRepositoryBase;

public class MaintenanceRepository extends AsyncRepositoryBase {

    public interface DateCallback {
        void onDate(@Nullable Long dateMillisUtc);
    }

    public interface ChecklistCallback {
        void onChecklist(@NonNull MaintenanceChecklist row);
    }

    private final MaintenanceDetailDao maintenanceDetailDao;
    private final MaintenanceChecklistDao checklistDao;

    public MaintenanceRepository(
            @NonNull MaintenanceDetailDao maintenanceDetailDao,
            @NonNull MaintenanceChecklistDao checklistDao
    ) {
        this.maintenanceDetailDao = maintenanceDetailDao;
        this.checklistDao = checklistDao;
    }

    public void getSavedDateMillis(long userId, @NonNull DateCallback callback) {
        runInBackground(() -> {
            MaintenanceDetail row = maintenanceDetailDao.getByUserIdSync(userId);
            Long value = row != null ? row.getDateMillis() : null;
            runOnMainThread(() -> callback.onDate(value));
        });
    }

    public void saveDateMillis(long userId, long dateMillisUtc, @NonNull Runnable onSaved) {
        runInBackground(() -> {
            MaintenanceDetail detail = new MaintenanceDetail();
            detail.setUserId(userId);
            detail.setDateMillis(dateMillisUtc);
            maintenanceDetailDao.upsert(detail);
            runOnMainThread(onSaved);
        });
    }

    /**
     * Returns the stored row or defaults (all tasks false, all chemicals 0) if none exists yet.
     */
    public void loadChecklist(long userId, @NonNull ChecklistCallback callback) {
        runInBackground(() -> {
            MaintenanceChecklist row = checklistDao.getByUserIdSync(userId);
            if (row == null) {
                row = defaultChecklist(userId);
            }
            MaintenanceChecklist finalRow = row;
            runOnMainThread(() -> callback.onChecklist(finalRow));
        });
    }

    @NonNull
    private MaintenanceChecklist defaultChecklist(long userId) {
        MaintenanceChecklist c = new MaintenanceChecklist();
        c.setUserId(userId);
        c.setVacuum(false);
        c.setCleanSkimmer(false);
        c.setAddWater(false);
        c.setBrushWalls(false);
        c.setChlorine(0f);
        c.setPhUp(0f);
        c.setPhDown(0f);
        c.setNoPhos(0f);
        return c;
    }

    public void setVacuum(long userId, boolean value, @NonNull Runnable onDone) {
        runInBackground(() -> {
            MaintenanceChecklist row = getOrCreateRow(userId);
            row.setVacuum(value);
            checklistDao.upsert(row);
            runOnMainThread(onDone);
        });
    }

    public void setCleanSkimmer(long userId, boolean value, @NonNull Runnable onDone) {
        runInBackground(() -> {
            MaintenanceChecklist row = getOrCreateRow(userId);
            row.setCleanSkimmer(value);
            checklistDao.upsert(row);
            runOnMainThread(onDone);
        });
    }

    public void setAddWater(long userId, boolean value, @NonNull Runnable onDone) {
        runInBackground(() -> {
            MaintenanceChecklist row = getOrCreateRow(userId);
            row.setAddWater(value);
            checklistDao.upsert(row);
            runOnMainThread(onDone);
        });
    }

    public void setBrushWalls(long userId, boolean value, @NonNull Runnable onDone) {
        runInBackground(() -> {
            MaintenanceChecklist row = getOrCreateRow(userId);
            row.setBrushWalls(value);
            checklistDao.upsert(row);
            runOnMainThread(onDone);
        });
    }

    public void saveChemicals(
            long userId,
            float chlorine,
            float phUp,
            float phDown,
            float noPhos,
            @NonNull Runnable onDone
    ) {
        runInBackground(() -> {
            MaintenanceChecklist row = getOrCreateRow(userId);
            row.setChlorine(chlorine);
            row.setPhUp(phUp);
            row.setPhDown(phDown);
            row.setNoPhos(noPhos);
            checklistDao.upsert(row);
            runOnMainThread(onDone);
        });
    }

    @NonNull
    private MaintenanceChecklist getOrCreateRow(long userId) {
        MaintenanceChecklist row = checklistDao.getByUserIdSync(userId);
        if (row == null) {
            row = defaultChecklist(userId);
        }
        return row;
    }
}
