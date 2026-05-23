package com.shannon.soaksafe.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.shannon.soaksafe.worker.AppIconUpdateWorker;

import java.util.concurrent.TimeUnit;

/**
 * Switches the launcher icon (activity-alias) based on how long the app has been idle.
 * Happy: &lt; 7 days · Sad: 7–13 days · Storm: 14+ days since last foreground use.
 */
public final class AppIconManager {

    public static final int DAYS_SAD = 7;
    public static final int DAYS_STORM = 14;

    private static final String PREF = "soaksafe_app_icon";
    private static final String KEY_LAST_FOREGROUND_MS = "last_foreground_ms";
    private static final String WORK_NAME = "soaksafe_app_icon_refresh";

    public enum Variant {
        HAPPY,
        SAD,
        STORM
    }

    private AppIconManager() {
    }

    /** Call when the app enters the foreground; records usage and shows the happy icon. */
    public static void onAppForeground(@NonNull Context context) {
        Context app = context.getApplicationContext();
        saveLastForegroundMs(app, System.currentTimeMillis());
        applyVariant(app, Variant.HAPPY);
    }

    /**
     * Recompute icon from stored last-open time without updating it (for background refresh).
     */
    public static void refreshIconFromIdleTime(@NonNull Context context) {
        applyVariant(context.getApplicationContext(), variantForIdleTime(context));
    }

    @NonNull
    public static Variant variantForIdleTime(@NonNull Context context) {
        long lastMs = getLastForegroundMs(context);
        if (lastMs <= 0L) {
            return Variant.HAPPY;
        }
        long idleDays = TimeUnit.MILLISECONDS.toDays(
                System.currentTimeMillis() - lastMs
        );
        if (idleDays >= DAYS_STORM) {
            return Variant.STORM;
        }
        if (idleDays >= DAYS_SAD) {
            return Variant.SAD;
        }
        return Variant.HAPPY;
    }

    public static void schedulePeriodicRefresh(@NonNull Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                AppIconUpdateWorker.class,
                1,
                TimeUnit.DAYS
        ).build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    static void applyVariant(@NonNull Context context, @NonNull Variant variant) {
        PackageManager pm = context.getPackageManager();
        setAliasEnabled(context, pm, "LauncherHappy", variant == Variant.HAPPY);
        setAliasEnabled(context, pm, "LauncherSad", variant == Variant.SAD);
        setAliasEnabled(context, pm, "LauncherStorm", variant == Variant.STORM);
    }

    private static void setAliasEnabled(
            @NonNull Context context,
            @NonNull PackageManager pm,
            @NonNull String aliasSimpleName,
            boolean enabled
    ) {
        String packageName = context.getPackageName();
        ComponentName component = new ComponentName(packageName, packageName + "." + aliasSimpleName);
        int state = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        try {
            pm.setComponentEnabledSetting(
                    component,
                    state,
                    PackageManager.DONT_KILL_APP
            );
        } catch (IllegalArgumentException e) {
            // Older installs or partial deploys may lack activity-alias entries; icon swap is non-critical.
            android.util.Log.w("AppIconManager", "Launcher alias unavailable: " + aliasSimpleName, e);
        }
    }

    private static long getLastForegroundMs(@NonNull Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_FOREGROUND_MS, 0L);
    }

    private static void saveLastForegroundMs(@NonNull Context context, long whenMs) {
        context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_FOREGROUND_MS, whenMs)
                .commit();
    }
}
