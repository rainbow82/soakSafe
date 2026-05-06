package com.wgu.d424.soaksafe.data;

import androidx.annotation.NonNull;

import com.wgu.d424.soaksafe.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Rows shown on Maintenance Details (matches course mockup).
 */
public final class MaintenanceTasksCatalog {

    private MaintenanceTasksCatalog() {
    }

    public static final class Entry {
        public final String key;
        public final int titleRes;
        /** When true, shows the gray “Amount” label next to the title (chemical row). */
        public final boolean showAmountHint;

        public Entry(@NonNull String key, int titleRes, boolean showAmountHint) {
            this.key = key;
            this.titleRes = titleRes;
            this.showAmountHint = showAmountHint;
        }
    }

    public static final String KEY_TASK_1 = "task_row_1";
    public static final String KEY_TASK_2 = "task_row_2";
    public static final String KEY_TASK_3 = "task_row_3";
    public static final String KEY_TASK_4 = "task_row_4";
    public static final String KEY_CHEMICAL = "task_chemical";

    public static final List<Entry> ENTRIES = Collections.unmodifiableList(Arrays.asList(
            new Entry(KEY_TASK_1, R.string.mock_task_label, false),
            new Entry(KEY_TASK_2, R.string.mock_task_label, false),
            new Entry(KEY_TASK_3, R.string.mock_task_label, false),
            new Entry(KEY_TASK_4, R.string.mock_task_label, false),
            new Entry(KEY_CHEMICAL, R.string.mock_task_chemical, true)
    ));
}
