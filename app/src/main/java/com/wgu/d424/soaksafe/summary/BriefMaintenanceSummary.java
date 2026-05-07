package com.wgu.d424.soaksafe.summary;

import androidx.annotation.NonNull;

import com.wgu.d424.soaksafe.data.MaintenanceChecklist;

/**
 * Short one-line summary for snackbars and quick share text.
 */
public final class BriefMaintenanceSummary implements MaintenanceSummaryStrategy {

    @NonNull
    @Override
    public String summarize(@NonNull MaintenanceChecklist checklist) {
        int done = countCompletedTasks(checklist);
        return done + "/4 tasks on, chemicals (Cl/pH↑/pH↓/NoPhos): "
                + formatReading(checklist.getChlorine()) + " / "
                + formatReading(checklist.getPhUp()) + " / "
                + formatReading(checklist.getPhDown()) + " / "
                + formatReading(checklist.getNoPhos());
    }

    /** Encapsulation: task counting logic is private to this strategy. */
    private int countCompletedTasks(@NonNull MaintenanceChecklist c) {
        int n = 0;
        if (c.isVacuum()) {
            n++;
        }
        if (c.isCleanSkimmer()) {
            n++;
        }
        if (c.isAddWater()) {
            n++;
        }
        if (c.isBrushWalls()) {
            n++;
        }
        return n;
    }

    private static String formatReading(float v) {
        if (v == 0f) {
            return "0";
        }
        if (v == (long) v) {
            return String.valueOf((long) v);
        }
        return String.valueOf(v);
    }
}
