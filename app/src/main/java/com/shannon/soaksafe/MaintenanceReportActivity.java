package com.shannon.soaksafe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shannon.soaksafe.databinding.DialogPdfReadyBinding;
import com.google.android.material.snackbar.Snackbar;
import com.shannon.soaksafe.data.MaintenanceEvent;
import com.shannon.soaksafe.data.MaintenanceRepository;
import com.shannon.soaksafe.databinding.ActivityMaintenanceReportBinding;
import com.shannon.soaksafe.report.MaintenanceEventAdapter;
import com.shannon.soaksafe.report.MaintenancePdfReportGenerator;
import com.shannon.soaksafe.report.MaintenanceReportExportHelper;
import com.shannon.soaksafe.report.MaintenanceReportSearchFilter;
import com.shannon.soaksafe.report.ReportEventRowsFactory;
import com.shannon.soaksafe.util.AppBarInsetsHelper;
import com.shannon.soaksafe.util.UserSessionPreferences;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MaintenanceReportActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "extra_user_id";
    /** When true, after events load the app offers PDF share/save (e.g. from home screen shortcut). */
    public static final String EXTRA_OPEN_PDF_WHEN_READY = "extra_open_pdf_when_ready";

    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);

    private final List<MaintenanceEvent> allEvents = new ArrayList<>();

    private ActivityMaintenanceReportBinding binding;
    private MaintenanceRepository maintenanceRepository;
    private MaintenanceEventAdapter adapter;
    private MaintenanceReportSearchFilter searchFilter;
    private long userId = -1L;

    private final ExecutorService pdfExecutor = Executors.newSingleThreadExecutor();
    @Nullable
    private File pendingExportFile;

    private final ActivityResultLauncher<String> savePdfLauncher =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/pdf"), uri -> {
                if (uri == null || pendingExportFile == null) {
                    return;
                }
                try {
                    MaintenanceReportExportHelper.copyToUri(this, pendingExportFile, uri);
                    Snackbar.make(binding.getRoot(), R.string.pdf_saved, Snackbar.LENGTH_LONG).show();
                } catch (Exception e) {
                    Snackbar.make(binding.getRoot(), R.string.pdf_export_failed, Snackbar.LENGTH_LONG).show();
                }
                pendingExportFile = null;
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMaintenanceReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppBarInsetsHelper.applyTopWindowInsets(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.maintenance_report_title);
        }

        userId = getIntent().getLongExtra(EXTRA_USER_ID, -1L);
        maintenanceRepository = ((SoakSafeApplication) getApplication()).getMaintenanceRepository();
        searchFilter = new MaintenanceReportSearchFilter(this);

        adapter = new MaintenanceEventAdapter(eventId -> {
            Intent edit = new Intent(this, EditMaintenanceReportActivity.class);
            edit.putExtra(EditMaintenanceReportActivity.EXTRA_EVENT_ID, eventId);
            edit.putExtra(EditMaintenanceReportActivity.EXTRA_USER_ID, userId);
            startActivity(edit);
        });
        binding.recyclerReportEvents.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerReportEvents.setAdapter(adapter);

        binding.editReportSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applySearchFilter();
            }
        });

        if (userId <= 0L) {
            binding.textReportEmpty.setText(R.string.report_no_events);
            binding.layoutReportSearch.setEnabled(false);
            binding.editReportSearch.setEnabled(false);
            return;
        }

        loadReportEvents();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_maintenance_report, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem export = menu.findItem(R.id.action_export_pdf);
        if (export != null) {
            export.setVisible(userId > 0L);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pdfExecutor.shutdown();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId > 0L) {
            loadReportEvents();
        }
    }

    private void loadReportEvents() {
        maintenanceRepository.loadEvents(userId, rows -> {
            allEvents.clear();
            allEvents.addAll(rows);
            applySearchFilter();

            boolean fromHomePdf = getIntent().getBooleanExtra(EXTRA_OPEN_PDF_WHEN_READY, false);
            if (fromHomePdf) {
                getIntent().removeExtra(EXTRA_OPEN_PDF_WHEN_READY);
                binding.getRoot().post(() -> {
                    if (allEvents.isEmpty()) {
                        Snackbar.make(binding.getRoot(), R.string.pdf_export_no_data, Snackbar.LENGTH_LONG)
                                .show();
                    } else {
                        exportMaintenancePdf();
                    }
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_export_pdf) {
            exportMaintenancePdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void applySearchFilter() {
        String query = binding.editReportSearch.getText() != null
                ? binding.editReportSearch.getText().toString()
                : "";

        List<MaintenanceEvent> filtered = new ArrayList<>();
        for (MaintenanceEvent row : allEvents) {
            if (searchFilter.matches(row, query)) {
                filtered.add(row);
            }
        }

        adapter.setRows(ReportEventRowsFactory.toUiModels(this, filtered, dateTimeFormat, query));

        if (allEvents.isEmpty()) {
            binding.textReportEmpty.setText(R.string.report_no_events);
            binding.textReportEmpty.setVisibility(View.VISIBLE);
        } else if (filtered.isEmpty()) {
            binding.textReportEmpty.setText(R.string.report_no_search_results);
            binding.textReportEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.textReportEmpty.setVisibility(View.GONE);
        }
    }

    private void exportMaintenancePdf() {
        if (userId <= 0L) {
            return;
        }
        if (allEvents.isEmpty()) {
            Snackbar.make(binding.getRoot(), R.string.pdf_export_no_data, Snackbar.LENGTH_SHORT).show();
            return;
        }
        File out = MaintenanceReportExportHelper.pdfCacheFile(this);
        pdfExecutor.execute(() -> {
            try {
                String owner = UserSessionPreferences.getLastDisplayName(this);
                if (owner.isEmpty()) {
                    owner = getString(R.string.pdf_owner_fallback);
                }
                String ownerLine = getString(R.string.pdf_report_owner_line, owner);
                List<MaintenanceEvent> snapshot = new ArrayList<>(allEvents);
                MaintenancePdfReportGenerator.writeReport(this, snapshot, ownerLine, out);
                runOnUiThread(() -> showPdfReadyDialog(out));
            } catch (Exception e) {
                runOnUiThread(() -> Snackbar.make(
                        binding.getRoot(),
                        R.string.pdf_export_failed,
                        Snackbar.LENGTH_LONG
                ).show());
            }
        });
    }

    private void showPdfReadyDialog(@NonNull File file) {
        pendingExportFile = file;
        DialogPdfReadyBinding sheet = DialogPdfReadyBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_SoakSafe_AlertDialog)
                .setView(sheet.getRoot())
                .create();
        sheet.buttonPdfShare.setOnClickListener(v -> {
            MaintenanceReportExportHelper.sharePdf(this, file);
            dialog.dismiss();
        });
        sheet.buttonPdfSave.setOnClickListener(v -> {
            savePdfLauncher.launch("SoakSafe_maintenance_report.pdf");
            dialog.dismiss();
        });
        dialog.show();
    }
}
