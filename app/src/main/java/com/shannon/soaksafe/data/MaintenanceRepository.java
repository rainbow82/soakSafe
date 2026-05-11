package com.shannon.soaksafe.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.shannon.soaksafe.base.AsyncRepositoryBase;

import java.util.ArrayList;
import java.util.List;

public class MaintenanceRepository extends AsyncRepositoryBase {

    public interface DateCallback {
        void onDate(@Nullable Long dateMillisUtc);
    }

    public interface ChecklistCallback {
        void onChecklist(@NonNull MaintenanceChecklist row);
    }

    public interface EventsCallback {
        void onEvents(@NonNull List<MaintenanceEvent> events);
    }

    private final MaintenanceDetailDao maintenanceDetailDao;
    private final MaintenanceChecklistDao checklistDao;
    private final MaintenanceEventDao eventDao;
    private final Context appContext;

    public MaintenanceRepository(
            @NonNull MaintenanceDetailDao maintenanceDetailDao,
            @NonNull MaintenanceChecklistDao checklistDao,
            @NonNull MaintenanceEventDao eventDao,
            @NonNull Context context
    ) {
        this.maintenanceDetailDao = maintenanceDetailDao;
        this.checklistDao = checklistDao;
        this.eventDao = eventDao;
        this.appContext = context.getApplicationContext();
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

            MaintenanceChecklist checklist = getOrCreateRow(userId);
            appendEvent(userId, "DATE_SET", dateMillisUtc, checklist);
            runOnMainThread(onSaved);
        });
    }

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

    public void loadEvents(long userId, @NonNull EventsCallback callback) {
        runInBackground(() -> {
            List<MaintenanceEvent> rows = eventDao.listByUserIdSync(userId);
            runOnMainThread(() -> callback.onEvents(rows));
        });
    }

    public interface SingleEventCallback {
        void onEvent(@NonNull MaintenanceEvent event);

        void onMissing();
    }

    public void loadEventForUser(long eventId, long userId, @NonNull SingleEventCallback callback) {
        runInBackground(() -> {
            MaintenanceEvent row = eventDao.getByIdSync(eventId);
            if (row == null || row.getUserId() != userId) {
                runOnMainThread(callback::onMissing);
            } else {
                MaintenanceEvent result = row;
                runOnMainThread(() -> callback.onEvent(result));
            }
        });
    }

    public void updateEvent(@NonNull MaintenanceEvent event, @NonNull Runnable onDone) {
        runInBackground(() -> {
            eventDao.update(event);
            runOnMainThread(onDone);
        });
    }

    public void deleteEvent(long eventId, long userId, @NonNull Runnable onDone) {
        runInBackground(() -> {
            eventDao.deleteByIdForUser(eventId, userId);
            runOnMainThread(onDone);
        });
    }

    public void setVacuum(long userId, boolean value, @NonNull Runnable onDone) {
        runInBackground(() -> {
            MaintenanceChecklist row = getOrCreateRow(userId);
            row.setVacuum(value);
            checklistDao.upsert(row);
            appendEvent(userId, "TASK_VACUUM", System.currentTimeMillis(), row);
            runOnMainThread(onDone);
        });
    }

    public void setCleanSkimmer(long userId, boolean value, @NonNull Runnable onDone) {
        runInBackground(() -> {
            MaintenanceChecklist row = getOrCreateRow(userId);
            row.setCleanSkimmer(value);
            checklistDao.upsert(row);
            appendEvent(userId, "TASK_CLEAN_SKIMMER", System.currentTimeMillis(), row);
            runOnMainThread(onDone);
        });
    }

    public void setAddWater(long userId, boolean value, @NonNull Runnable onDone) {
        runInBackground(() -> {
            MaintenanceChecklist row = getOrCreateRow(userId);
            row.setAddWater(value);
            checklistDao.upsert(row);
            appendEvent(userId, "TASK_ADD_WATER", System.currentTimeMillis(), row);
            runOnMainThread(onDone);
        });
    }

    public void setBrushWalls(long userId, boolean value, @NonNull Runnable onDone) {
        runInBackground(() -> {
            MaintenanceChecklist row = getOrCreateRow(userId);
            row.setBrushWalls(value);
            checklistDao.upsert(row);
            appendEvent(userId, "TASK_BRUSH_WALLS", System.currentTimeMillis(), row);
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
            appendEvent(userId, "CHEMICALS_SAVED", System.currentTimeMillis(), row);
            runOnMainThread(onDone);
        });
    }

    /**
     * Persists tasks and chemicals together (e.g. after Save on maintenance details).
     */
    public void saveFullChecklist(
            long userId,
            boolean vacuum,
            boolean cleanSkimmer,
            boolean addWater,
            boolean brushWalls,
            float chlorine,
            float phUp,
            float phDown,
            float noPhos,
            @Nullable String customLinesJson,
            @NonNull Runnable onDone
    ) {
        runInBackground(() -> {
            MaintenanceChecklist row = getOrCreateRow(userId);
            row.setVacuum(vacuum);
            row.setCleanSkimmer(cleanSkimmer);
            row.setAddWater(addWater);
            row.setBrushWalls(brushWalls);
            row.setChlorine(chlorine);
            row.setPhUp(phUp);
            row.setPhDown(phDown);
            row.setNoPhos(noPhos);
            row.setCustomLinesJson(customLinesJson);
            checklistDao.upsert(row);
            appendEvent(userId, "CHECKLIST_SAVED", System.currentTimeMillis(), row);
            runOnMainThread(onDone);
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

    @NonNull
    private MaintenanceChecklist getOrCreateRow(long userId) {
        MaintenanceChecklist row = checklistDao.getByUserIdSync(userId);
        if (row == null) {
            row = defaultChecklist(userId);
        }
        return row;
    }

    private void appendEvent(
            long userId,
            @NonNull String eventType,
            long eventTimeMillis,
            @NonNull MaintenanceChecklist row
    ) {
        MaintenanceEvent event = new MaintenanceEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setEventTimeMillis(eventTimeMillis);
        event.setDateMillis(eventTimeMillis);
        event.setVacuum(row.isVacuum());
        event.setCleanSkimmer(row.isCleanSkimmer());
        event.setAddWater(row.isAddWater());
        event.setBrushWalls(row.isBrushWalls());
        event.setChlorine(row.getChlorine());
        event.setPhUp(row.getPhUp());
        event.setPhDown(row.getPhDown());
        event.setNoPhos(row.getNoPhos());

        MaintenanceEvent forCodec = new MaintenanceEvent();
        forCodec.setVacuum(row.isVacuum());
        forCodec.setCleanSkimmer(row.isCleanSkimmer());
        forCodec.setAddWater(row.isAddWater());
        forCodec.setBrushWalls(row.isBrushWalls());
        forCodec.setChlorine(row.getChlorine());
        forCodec.setPhUp(row.getPhUp());
        forCodec.setPhDown(row.getPhDown());
        forCodec.setNoPhos(row.getNoPhos());
        List<EventLineItem> merged = new ArrayList<>(
                EventLineItemsCodec.fromLegacyEvent(forCodec, appContext));
        merged.addAll(MaintenanceCustomLinesCodec.selectedAsEventLineItems(row.getCustomLinesJson()));
        if (merged.isEmpty()) {
            event.setLineItemsJson(null);
        } else {
            event.setLineItemsJson(EventLineItemsCodec.encode(merged));
        }
        eventDao.insert(event);
    }
}
