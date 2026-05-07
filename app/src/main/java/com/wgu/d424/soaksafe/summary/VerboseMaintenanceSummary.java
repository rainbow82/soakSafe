package com.wgu.d424.soaksafe.summary;

import androidx.annotation.NonNull;

import com.wgu.d424.soaksafe.data.MaintenanceChecklist;

/**
 * Full multi-line report for dialogs or detailed export.
 */
public final class VerboseMaintenanceSummary implements MaintenanceSummaryStrategy {

    @NonNull
    @Override
    public String summarize(@NonNull MaintenanceChecklist checklist) {
        return buildTasksSection(checklist) + "\n" + buildChemicalsSection(checklist);
    }

    private String buildTasksSection(@NonNull MaintenanceChecklist c) {
        return "Tasks:\n"
                + line("Vacuum", c.isVacuum())
                + line("Clean skimmer", c.isCleanSkimmer())
                + line("Add water", c.isAddWater())
                + line("Brush walls", c.isBrushWalls());
    }

    private static String line(@NonNull String label, boolean on) {
        return " • " + label + ": " + (on ? "yes" : "no") + "\n";
    }

    private String buildChemicalsSection(@NonNull MaintenanceChecklist c) {
        return "Chemicals:\n"
                + " • Chlorine: " + c.getChlorine() + "\n"
                + " • pH up: " + c.getPhUp() + "\n"
                + " • pH down: " + c.getPhDown() + "\n"
                + " • No phos: " + c.getNoPhos();
    }
}
