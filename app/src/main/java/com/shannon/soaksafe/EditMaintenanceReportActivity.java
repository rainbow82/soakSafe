package com.shannon.soaksafe;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.shannon.soaksafe.data.EventLineItem;
import com.shannon.soaksafe.data.EventLineItemsCodec;
import com.shannon.soaksafe.data.MaintenanceEvent;
import com.shannon.soaksafe.data.MaintenanceRepository;
import com.shannon.soaksafe.databinding.ActivityEditMaintenanceReportBinding;
import com.shannon.soaksafe.databinding.DialogAddReportLineBinding;
import com.shannon.soaksafe.databinding.DialogDeleteReportBinding;
import com.shannon.soaksafe.databinding.ItemEditReportLineBinding;
import com.shannon.soaksafe.util.AppBarInsetsHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditMaintenanceReportActivity extends AppCompatActivity {

    public static final String EXTRA_EVENT_ID = "extra_event_id";
    public static final String EXTRA_USER_ID = "extra_user_id";

    private ActivityEditMaintenanceReportBinding binding;
    private MaintenanceRepository maintenanceRepository;
    private long userId = -1L;
    private long eventId = -1L;
    @Nullable
    private MaintenanceEvent loadedEvent;

    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditMaintenanceReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppBarInsetsHelper.applyTopWindowInsets(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.edit_report_title);
        }

        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, -1L);
        userId = getIntent().getLongExtra(EXTRA_USER_ID, -1L);
        maintenanceRepository = ((SoakSafeApplication) getApplication()).getMaintenanceRepository();

        binding.buttonSaveEditReport.setOnClickListener(v -> saveEdits());
        binding.buttonDeleteReport.setOnClickListener(v -> confirmDelete());
        binding.fabAddLine.setOnClickListener(v -> showAddLineDialog());

        if (eventId <= 0L || userId <= 0L) {
            Snackbar.make(binding.getRoot(), R.string.edit_report_missing, Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }

        maintenanceRepository.loadEventForUser(eventId, userId, new MaintenanceRepository.SingleEventCallback() {
            @Override
            public void onEvent(@NonNull MaintenanceEvent event) {
                loadedEvent = event;
                binding.textEditReportTimestamp.setText(
                        dateTimeFormat.format(new Date(event.getEventTimeMillis()))
                );
                populateLines(event);
                setFormEnabled(true);
            }

            @Override
            public void onMissing() {
                Snackbar.make(binding.getRoot(), R.string.edit_report_missing, Snackbar.LENGTH_LONG).show();
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setFormEnabled(boolean enabled) {
        binding.buttonSaveEditReport.setEnabled(enabled);
        binding.buttonDeleteReport.setEnabled(enabled);
        binding.fabAddLine.setEnabled(enabled);
        for (int i = 0; i < binding.containerEditChips.getChildCount(); i++) {
            View row = binding.containerEditChips.getChildAt(i);
            ItemEditReportLineBinding b = ItemEditReportLineBinding.bind(row);
            b.editLineLabel.setEnabled(enabled);
            b.editLineAmount.setEnabled(enabled);
            b.buttonRemoveLine.setEnabled(enabled);
        }
    }

    private void populateLines(@NonNull MaintenanceEvent event) {
        binding.containerEditChips.removeAllViews();
        for (EventLineItem line : EventLineItemsCodec.resolveLineItems(this, event)) {
            addLineRow(line);
        }
    }

    private void addLineRow(@NonNull EventLineItem item) {
        ItemEditReportLineBinding line = ItemEditReportLineBinding.inflate(
                LayoutInflater.from(this),
                binding.containerEditChips,
                true
        );
        line.editLineLabel.setText(item.label);
        if (item.amount != null) {
            line.editLineAmount.setText(floatFieldToText(item.amount));
        } else {
            line.editLineAmount.setText("");
        }
        line.buttonRemoveLine.setOnClickListener(v ->
                binding.containerEditChips.removeView(line.getRoot())
        );
    }

    private void showAddLineDialog() {
        DialogAddReportLineBinding dialogBinding = DialogAddReportLineBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_SoakSafe_AlertDialog)
                .setTitle(R.string.add_report_line_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.add_report_line_button, null)
                .create();
        dialog.setOnShowListener(d2 -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = dialogBinding.editNewItemName.getText() != null
                    ? dialogBinding.editNewItemName.getText().toString().trim()
                    : "";
            if (name.isEmpty()) {
                Snackbar.make(binding.getRoot(), R.string.edit_line_name_required, Snackbar.LENGTH_SHORT).show();
                return;
            }
            String amtStr = dialogBinding.editNewItemAmount.getText() != null
                    ? dialogBinding.editNewItemAmount.getText().toString().trim()
                    : "";
            Float amt = null;
            if (!amtStr.isEmpty()) {
                try {
                    amt = Float.parseFloat(amtStr);
                } catch (NumberFormatException e) {
                    Snackbar.make(binding.getRoot(), R.string.error_chemical_number, Snackbar.LENGTH_LONG).show();
                    return;
                }
            }
            addLineRow(new EventLineItem(name, amt));
            dialog.dismiss();
        }));
        dialog.show();
    }

    @NonNull
    private String floatFieldToText(float value) {
        if (value == 0f) {
            return "";
        }
        if (value == (long) value) {
            return String.format(Locale.US, "%d", (long) value);
        }
        return String.format(Locale.US, "%s", value);
    }

    private void saveEdits() {
        if (loadedEvent == null) {
            return;
        }
        List<EventLineItem> lines = readLinesFromContainer();
        if (lines == null) {
            return;
        }

        MaintenanceEvent updated = loadedEvent;
        EventLineItemsCodec.applyLinesToEvent(this, updated, lines);
        updated.setEventType("CHECKLIST_SAVED");

        maintenanceRepository.updateEvent(updated, () ->
                Snackbar.make(binding.getRoot(), R.string.report_updated, Snackbar.LENGTH_SHORT).show()
        );
    }

    /**
     * @return null if validation failed
     */
    @Nullable
    private List<EventLineItem> readLinesFromContainer() {
        List<EventLineItem> out = new ArrayList<>();
        for (int i = 0; i < binding.containerEditChips.getChildCount(); i++) {
            View row = binding.containerEditChips.getChildAt(i);
            ItemEditReportLineBinding b = ItemEditReportLineBinding.bind(row);
            String label = b.editLineLabel.getText() != null
                    ? b.editLineLabel.getText().toString().trim()
                    : "";
            if (label.isEmpty()) {
                continue;
            }
            String amtStr = b.editLineAmount.getText() != null
                    ? b.editLineAmount.getText().toString().trim()
                    : "";
            Float amt = null;
            if (!amtStr.isEmpty()) {
                try {
                    amt = Float.parseFloat(amtStr);
                } catch (NumberFormatException e) {
                    Snackbar.make(binding.getRoot(), R.string.error_chemical_number, Snackbar.LENGTH_LONG).show();
                    return null;
                }
            }
            out.add(new EventLineItem(label, amt));
        }
        return out;
    }

    private void confirmDelete() {
        DialogDeleteReportBinding sheet = DialogDeleteReportBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_SoakSafe_AlertDialog)
                .setView(sheet.getRoot())
                .create();
        sheet.buttonDeleteCancel.setOnClickListener(v -> dialog.dismiss());
        sheet.buttonDeleteConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            maintenanceRepository.deleteEvent(eventId, userId, () -> {
                Snackbar.make(binding.getRoot(), R.string.report_deleted, Snackbar.LENGTH_SHORT).show();
                finish();
            });
        });
        dialog.show();
    }
}
