package com.shannon.soaksafe.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.shannon.soaksafe.util.AppIconManager;

/**
 * Periodically updates the launcher icon when the app has been idle (Duolingo-style).
 */
public class AppIconUpdateWorker extends Worker {

    public AppIconUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppIconManager.refreshIconFromIdleTime(getApplicationContext());
        return Result.success();
    }
}
