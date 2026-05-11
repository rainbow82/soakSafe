package com.shannon.soaksafe.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * One row in a maintenance report entry. {@code amount == null} means a task-style line (no dose).
 */
public final class EventLineItem {

    @NonNull
    public final String label;
    @Nullable
    public final Float amount;

    public EventLineItem(@NonNull String label, @Nullable Float amount) {
        this.label = label;
        this.amount = amount;
    }
}
