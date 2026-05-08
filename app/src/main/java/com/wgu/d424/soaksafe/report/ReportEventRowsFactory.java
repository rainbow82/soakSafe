package com.wgu.d424.soaksafe.report;

import android.content.Context;

import androidx.annotation.NonNull;

import com.wgu.d424.soaksafe.R;
import com.wgu.d424.soaksafe.data.EventLineItem;
import com.wgu.d424.soaksafe.data.EventLineItemsCodec;
import com.wgu.d424.soaksafe.data.MaintenanceEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Builds {@link MaintenanceEventUiModel} rows for report UIs (full screen or modal) from
 * {@link MaintenanceEvent} entities.
 */
public final class ReportEventRowsFactory {

    private ReportEventRowsFactory() {
    }

    @NonNull
    public static List<MaintenanceEventUiModel> toUiModels(
            @NonNull Context context,
            @NonNull List<MaintenanceEvent> rows,
            @NonNull SimpleDateFormat dateTimeFormat
    ) {
        List<MaintenanceEventUiModel> mapped = new ArrayList<>();
        for (MaintenanceEvent row : rows) {
            String time = dateTimeFormat.format(new Date(row.getEventTimeMillis()));
            mapped.add(new MaintenanceEventUiModel(row.getId(), time, buildDetailRows(context, row)));
        }
        return mapped;
    }

    @NonNull
    public static List<ReportDetailLine> buildDetailRows(@NonNull Context context, @NonNull MaintenanceEvent row) {
        String json = row.getLineItemsJson();
        if (json != null && !json.trim().isEmpty()) {
            return buildDetailRowsFromJson(json);
        }
        List<ReportDetailLine> lines = new ArrayList<>();
        if (row.isVacuum()) {
            lines.add(ReportDetailLine.taskDone(context.getString(R.string.task_vacuum)));
        }
        if (row.isCleanSkimmer()) {
            lines.add(ReportDetailLine.taskDone(context.getString(R.string.task_clean_skimmer)));
        }
        if (row.isAddWater()) {
            lines.add(ReportDetailLine.taskDone(context.getString(R.string.task_add_water)));
        }
        if (row.isBrushWalls()) {
            lines.add(ReportDetailLine.taskDone(context.getString(R.string.task_brush_walls)));
        }
        if (row.getChlorine() > 0f) {
            lines.add(ReportDetailLine.chemical(
                    context.getString(R.string.chemical_chlorine),
                    formatChem(row.getChlorine())
            ));
        }
        if (row.getPhUp() > 0f) {
            lines.add(ReportDetailLine.chemical(
                    context.getString(R.string.chemical_ph_up),
                    formatChem(row.getPhUp())
            ));
        }
        if (row.getPhDown() > 0f) {
            lines.add(ReportDetailLine.chemical(
                    context.getString(R.string.chemical_ph_down),
                    formatChem(row.getPhDown())
            ));
        }
        if (row.getNoPhos() > 0f) {
            lines.add(ReportDetailLine.chemical(
                    context.getString(R.string.chemical_no_phos),
                    formatChem(row.getNoPhos())
            ));
        }
        return lines;
    }

    @NonNull
    private static List<ReportDetailLine> buildDetailRowsFromJson(@NonNull String json) {
        List<ReportDetailLine> lines = new ArrayList<>();
        for (EventLineItem item : EventLineItemsCodec.decode(json)) {
            if (item.amount == null) {
                lines.add(ReportDetailLine.taskDone(item.label));
            } else if (item.amount > 0f) {
                lines.add(ReportDetailLine.chemical(item.label, formatChem(item.amount)));
            }
        }
        return lines;
    }

    @NonNull
    public static String formatChem(float value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
