package com.shannon.soaksafe.report;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.shannon.soaksafe.data.EventLineItem;
import com.shannon.soaksafe.data.EventLineItemsCodec;
import com.shannon.soaksafe.data.MaintenanceEvent;

import java.util.Locale;

/**
 * Filters maintenance events using the user's query. Keywords match the same signals shown on cards:
 * tasks that are done, chemicals with amount greater than zero, plus event type.
 */
public final class MaintenanceReportSearchFilter {

    private final Context appContext;

    public MaintenanceReportSearchFilter(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
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
        for (EventLineItem item : EventLineItemsCodec.resolveLineItems(appContext, e)) {
            sb.append(item.label).append(' ');
            if (item.amount != null) {
                sb.append(trimmedAmount(item.amount)).append(' ');
            }
        }
        String type = e.getEventType();
        if (!type.isEmpty()) {
            sb.append(type.toLowerCase(Locale.US)).append(' ');
        }
        return sb.toString();
    }

    @NonNull
    private static String trimmedAmount(float value) {
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        }
        return String.format(Locale.US, "%s", value);
    }
}
