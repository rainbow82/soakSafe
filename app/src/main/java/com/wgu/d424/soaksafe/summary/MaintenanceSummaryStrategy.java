package com.wgu.d424.soaksafe.summary;

import androidx.annotation.NonNull;

import com.wgu.d424.soaksafe.data.MaintenanceChecklist;

/**
 * Polymorphism: different implementations produce different text from the same
 * {@link MaintenanceChecklist} model (strategy interface).
 */
public interface MaintenanceSummaryStrategy {

    @NonNull
    String summarize(@NonNull MaintenanceChecklist checklist);
}
