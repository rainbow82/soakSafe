package com.shannon.soaksafe.report;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ReportDetailLine {

    @NonNull
    public final String label;
    public final boolean showCheckmark;
    @Nullable
    public final String valueText;

    private ReportDetailLine(
            @NonNull String label,
            boolean showCheckmark,
            @Nullable String valueText
    ) {
        this.label = label;
        this.showCheckmark = showCheckmark;
        this.valueText = valueText;
    }

    @NonNull
    public static ReportDetailLine taskDone(@NonNull String label) {
        return new ReportDetailLine(label, true, null);
    }

    @NonNull
    public static ReportDetailLine chemical(@NonNull String label, @NonNull String valueText) {
        return new ReportDetailLine(label, false, valueText);
    }
}
