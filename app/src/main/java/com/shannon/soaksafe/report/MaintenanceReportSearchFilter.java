package com.shannon.soaksafe.report;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.shannon.soaksafe.R;
import com.shannon.soaksafe.data.EventLineItemsCodec;
import com.shannon.soaksafe.data.MaintenanceEvent;

import java.util.Locale;

/**
 * Filters maintenance events using the user's query. Keywords match the same signals shown on cards:
 * tasks that are done, chemicals with amount greater than zero, plus event type.
 */
public final class MaintenanceReportSearchFilter {

    private final Resources resources;

    public MaintenanceReportSearchFilter(@NonNull Context context) {
        this.resources = context.getApplicationContext().getResources();
    }

    public boolean matches(@NonNull MaintenanceEvent event, @Nullable String rawQuery) {
        return haystackContainsAllTokens(buildHaystack(event), rawQuery);
    }

    /**
     * True when every non-empty whitespace-separated token from {@code rawQuery} appears as a
     * substring of {@code haystack} (case-insensitive, {@link Locale#US}).
     */
    public static boolean haystackContainsAllTokens(
            @NonNull String haystack,
            @Nullable String rawQuery
    ) {
        if (rawQuery == null) {
            return true;
        }
        String normalized = rawQuery.trim().toLowerCase(Locale.US);
        if (normalized.isEmpty()) {
            return true;
        }
        String hay = haystack.toLowerCase(Locale.US);
        for (String token : normalized.split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (!hay.contains(token)) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    private String buildHaystack(@NonNull MaintenanceEvent e) {
        StringBuilder sb = new StringBuilder();
        EventLineItemsCodec.appendJsonHaystack(sb, e.getLineItemsJson());
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
        if (amount <= 0f) {
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
