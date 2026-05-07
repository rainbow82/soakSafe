package com.wgu.d424.soaksafe.base;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Encapsulates background executor and main-thread {@link Handler} for Room access.
 * Concrete repositories extend this class and use {@link #runInBackground(Runnable)} /
 * {@link #runOnMainThread(Runnable)} instead of duplicating threading code (inheritance).
 */
public abstract class AsyncRepositoryBase {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    protected void runInBackground(@NonNull Runnable task) {
        executor.execute(task);
    }

    protected void runOnMainThread(@NonNull Runnable task) {
        mainHandler.post(task);
    }
}
