package com.shannon.soaksafe.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.shannon.soaksafe.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EventLineItemsCodec {

    private static final String KEY_LABEL = "label";
    private static final String KEY_AMOUNT = "amount";

    private EventLineItemsCodec() {
    }

    @NonNull
    public static List<EventLineItem> decode(@Nullable String json) {
        List<EventLineItem> out = new ArrayList<>();
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
                out.add(new EventLineItem(label, amount));
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    @NonNull
    public static String encode(@NonNull List<EventLineItem> items) {
        JSONArray arr = new JSONArray();
        try {
            for (EventLineItem item : items) {
                if (item.label.trim().isEmpty()) {
                    continue;
                }
                JSONObject o = new JSONObject();
                o.put(KEY_LABEL, item.label.trim());
                if (item.amount != null) {
                    o.put(KEY_AMOUNT, item.amount.doubleValue());
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

    @NonNull
    public static List<EventLineItem> fromLegacyEvent(
            @NonNull MaintenanceEvent e,
            @NonNull Context context
    ) {
        List<EventLineItem> out = new ArrayList<>();
        if (e.isVacuum()) {
            out.add(new EventLineItem(context.getString(R.string.task_vacuum), null));
        }
        if (e.isCleanSkimmer()) {
            out.add(new EventLineItem(context.getString(R.string.task_clean_skimmer), null));
        }
        if (e.isAddWater()) {
            out.add(new EventLineItem(context.getString(R.string.task_add_water), null));
        }
        if (e.isBrushWalls()) {
            out.add(new EventLineItem(context.getString(R.string.task_brush_walls), null));
        }
        if (e.getChlorine() > 0f) {
            out.add(new EventLineItem(context.getString(R.string.chemical_chlorine), e.getChlorine()));
        }
        if (e.getPhUp() > 0f) {
            out.add(new EventLineItem(context.getString(R.string.chemical_ph_up), e.getPhUp()));
        }
        if (e.getPhDown() > 0f) {
            out.add(new EventLineItem(context.getString(R.string.chemical_ph_down), e.getPhDown()));
        }
        if (e.getNoPhos() > 0f) {
            out.add(new EventLineItem(context.getString(R.string.chemical_no_phos), e.getNoPhos()));
        }
        return out;
    }

    /**
     * Writes JSON and mirrors known labels into legacy columns for older code paths.
     */
    public static void applyLinesToEvent(
            @NonNull Context context,
            @NonNull MaintenanceEvent e,
            @NonNull List<EventLineItem> items
    ) {
        e.setLineItemsJson(encode(items));
        e.setVacuum(false);
        e.setCleanSkimmer(false);
        e.setAddWater(false);
        e.setBrushWalls(false);
        e.setChlorine(0f);
        e.setPhUp(0f);
        e.setPhDown(0f);
        e.setNoPhos(0f);

        for (EventLineItem item : items) {
            String l = item.label.trim();
            if (l.isEmpty()) {
                continue;
            }
            if (item.amount == null) {
                if (l.equals(context.getString(R.string.task_vacuum))) {
                    e.setVacuum(true);
                } else if (l.equals(context.getString(R.string.task_clean_skimmer))) {
                    e.setCleanSkimmer(true);
                } else if (l.equals(context.getString(R.string.task_add_water))) {
                    e.setAddWater(true);
                } else if (l.equals(context.getString(R.string.task_brush_walls))) {
                    e.setBrushWalls(true);
                }
            } else {
                if (l.equals(context.getString(R.string.chemical_chlorine))) {
                    e.setChlorine(item.amount);
                } else if (l.equals(context.getString(R.string.chemical_ph_up))) {
                    e.setPhUp(item.amount);
                } else if (l.equals(context.getString(R.string.chemical_ph_down))) {
                    e.setPhDown(item.amount);
                } else if (l.equals(context.getString(R.string.chemical_no_phos))) {
                    e.setNoPhos(item.amount);
                }
            }
        }
    }

    public static void appendJsonHaystack(@NonNull StringBuilder sb, @Nullable String json) {
        for (EventLineItem item : decode(json)) {
            sb.append(item.label).append(' ');
            if (item.amount != null) {
                sb.append(trimmedAmount(item.amount)).append(' ');
            }
        }
    }

    @NonNull
    private static String trimmedAmount(float value) {
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        }
        return String.format(Locale.US, "%s", value);
    }
}
