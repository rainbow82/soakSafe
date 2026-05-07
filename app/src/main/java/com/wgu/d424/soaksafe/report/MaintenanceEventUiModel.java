package com.wgu.d424.soaksafe.report;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MaintenanceEventUiModel {

    public final String timeLabel;
    @NonNull
    public final List<ReportDetailLine> detailLines;

    public MaintenanceEventUiModel(
            @NonNull String timeLabel,
            @NonNull List<ReportDetailLine> detailLines
    ) {
        this.timeLabel = timeLabel;
        this.detailLines = Collections.unmodifiableList(new ArrayList<>(detailLines));
    }
}
