package com.wgu.d424.soaksafe.report;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.wgu.d424.soaksafe.R;
import com.wgu.d424.soaksafe.data.MaintenanceEvent;

import java.util.Locale;

/**
 * Filters maintenance events using the user's query. Keywords match the same signals shown on cards:
 * tasks that are done, chemicals with amount greater than 1, plus event type.
 */
public final class MaintenanceReportSearchFilter {

    private static final float CHEMICAL_VISIBLE_THRESHOLD = 1f;

    private final Resources resources;

    public MaintenanceReportSearchFilter(@NonNull Context context) {
        this.resources = context.getApplicationContext().getResources();
    }

    public boolean matches(@NonNull MaintenanceEvent event, @Nullable String rawQuery) {
        if (rawQuery == null) {
            return true;
        }
        String normalized = rawQuery.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) {
            return true;
        }
        String haystack = buildHaystack(event).toLowerCase(Locale.US);
        for (String token : normalized.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (!haystack.contains(token)) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    private String buildHaystack(@NonNull MaintenanceEvent e) {
        StringBuilder sb = new StringBuilder();
        if (e.isVacuum()) {
            sb.append(resources.getString(R.string.task_vacuum)).append(' ');
        }
        if (e.isCleanSkimmer()) {
            sb.append(resources.getString(R.string.task_clean_skimmer)).append(' ');
        }
        if (e.isAddWater()) {
            sb.append(resources.getString(R.string.task_add_water)).append(' ');
        }
        if (e.isBrushWalls()) {
            sb.append(resources.getString(R.string.task_brush_walls)).append(' ');
        }
        appendChemicalIfPresent(sb, e.getChlorine(), R.string.chemical_chlorine);
        appendChemicalIfPresent(sb, e.getPhUp(), R.string.chemical_ph_up);
        appendChemicalIfPresent(sb, e.getPhDown(), R.string.chemical_ph_down);
        appendChemicalIfPresent(sb, e.getNoPhos(), R.string.chemical_no_phos);

        String type = e.getEventType();
        if (!type.isEmpty()) {
            sb.append(type.toLowerCase(Locale.US)).append(' ');
        }
        return sb.toString();
    }

    private void appendChemicalIfPresent(
            @NonNull StringBuilder sb,
            float amount,
            int labelRes
    ) {
        if (amount <= CHEMICAL_VISIBLE_THRESHOLD) {
            return;
        }
        sb.append(resources.getString(labelRes)).append(' ');
        sb.append(trimmedAmount(amount)).append(' ');
    }

    @NonNull
    private static String trimmedAmount(float value) {
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        }
        return String.format(Locale.US, "%s", value);
    }
}
