package com.shannon.soaksafe.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional custom rows on the maintenance checklist (label + selected + optional chemical amount),
 * stored as JSON on {@link MaintenanceChecklist}.
 */
public final class MaintenanceCustomLinesCodec {

    private static final String KEY_LABEL = "label";
    private static final String KEY_SELECTED = "selected";
    private static final String KEY_AMOUNT = "amount";

    public static final class Entry {
        @NonNull
        public final String label;
        public final boolean selected;
        @Nullable
        public final Float amount;

        public Entry(@NonNull String label, boolean selected, @Nullable Float amount) {
            this.label = label;
            this.selected = selected;
            this.amount = amount;
        }
    }

    private MaintenanceCustomLinesCodec() {
    }

    @NonNull
    public static List<Entry> decode(@Nullable String json) {
        List<Entry> out = new ArrayList<>();
        if (json == null || json.trim().isEmpty()) {
            return out;
        }
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String label = o.optString(KEY_LABEL, "").trim();
                if (label.isEmpty()) {
                    continue;
                }
                Float amount = null;
                if (o.has(KEY_AMOUNT) && !o.isNull(KEY_AMOUNT)) {
                    amount = (float) o.getDouble(KEY_AMOUNT);
                }
                out.add(new Entry(label, o.optBoolean(KEY_SELECTED, false), amount));
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    @NonNull
    public static String encode(@NonNull List<Entry> entries) {
        JSONArray arr = new JSONArray();
        try {
            for (Entry e : entries) {
                String l = e.label.trim();
                if (l.isEmpty()) {
                    continue;
                }
                JSONObject o = new JSONObject();
                o.put(KEY_LABEL, l);
                o.put(KEY_SELECTED, e.selected);
                if (e.amount != null) {
                    o.put(KEY_AMOUNT, e.amount.doubleValue());
                } else {
                    o.put(KEY_AMOUNT, JSONObject.NULL);
                }
                arr.put(o);
            }
        } catch (JSONException e) {
            return "[]";
        }
        return arr.toString();
    }

    /**
     * Custom rows to merge into a saved {@link com.shannon.soaksafe.data.MaintenanceEvent} snapshot:
     * only rows the user marked done ({@code selected}) become line items (tasks with null amount,
     * chemicals with their dose).
     */
    @NonNull
    public static List<EventLineItem> selectedAsEventLineItems(@Nullable String json) {
        List<EventLineItem> out = new ArrayList<>();
        for (Entry e : decode(json)) {
            if (!e.selected) {
                continue;
            }
            if (e.amount != null && e.amount > 0f) {
                out.add(new EventLineItem(e.label, e.amount));
            } else {
                out.add(new EventLineItem(e.label, null));
            }
        }
        return out;
    }
}
