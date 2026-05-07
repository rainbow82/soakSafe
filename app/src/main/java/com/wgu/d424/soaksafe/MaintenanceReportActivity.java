package com.wgu.d424.soaksafe;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wgu.d424.soaksafe.data.MaintenanceEvent;
import com.wgu.d424.soaksafe.data.MaintenanceRepository;
import com.wgu.d424.soaksafe.databinding.ActivityMaintenanceReportBinding;
import com.wgu.d424.soaksafe.report.MaintenanceEventAdapter;
import com.wgu.d424.soaksafe.report.MaintenanceEventUiModel;
import com.wgu.d424.soaksafe.report.MaintenanceReportSearchFilter;
import com.wgu.d424.soaksafe.report.ReportDetailLine;
import com.wgu.d424.soaksafe.util.AppBarInsetsHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MaintenanceReportActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "extra_user_id";

    private final SimpleDateFormat dateTimeFormat =
            new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);

    private final List<MaintenanceEvent> allEvents = new ArrayList<>();

    private ActivityMaintenanceReportBinding binding;
    private MaintenanceRepository maintenanceRepository;
    private MaintenanceEventAdapter adapter;
    private MaintenanceReportSearchFilter searchFilter;
    private long userId = -1L;

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

        adapter = new MaintenanceEventAdapter();
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

        maintenanceRepository.loadEvents(userId, rows -> {
            allEvents.clear();
            allEvents.addAll(rows);
            applySearchFilter();
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

        adapter.setRows(mapRows(filtered));

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

    @NonNull
    private List<MaintenanceEventUiModel> mapRows(@NonNull List<MaintenanceEvent> rows) {
        List<MaintenanceEventUiModel> mapped = new ArrayList<>();
        for (MaintenanceEvent row : rows) {
            String time = dateTimeFormat.format(new Date(row.getEventTimeMillis()));
            mapped.add(new MaintenanceEventUiModel(time, buildDetailRows(row)));
        }
        return mapped;
    }

    @NonNull
    private List<ReportDetailLine> buildDetailRows(@NonNull MaintenanceEvent row) {
        List<ReportDetailLine> lines = new ArrayList<>();
        if (row.isVacuum()) {
            lines.add(ReportDetailLine.taskDone(getString(R.string.task_vacuum)));
        }
        if (row.isCleanSkimmer()) {
            lines.add(ReportDetailLine.taskDone(getString(R.string.task_clean_skimmer)));
        }
        if (row.isAddWater()) {
            lines.add(ReportDetailLine.taskDone(getString(R.string.task_add_water)));
        }
        if (row.isBrushWalls()) {
            lines.add(ReportDetailLine.taskDone(getString(R.string.task_brush_walls)));
        }
        if (row.getChlorine() > 1f) {
            lines.add(ReportDetailLine.chemical(
                    getString(R.string.chemical_chlorine),
                    formatChem(row.getChlorine())
            ));
        }
        if (row.getPhUp() > 1f) {
            lines.add(ReportDetailLine.chemical(
                    getString(R.string.chemical_ph_up),
                    formatChem(row.getPhUp())
            ));
        }
        if (row.getPhDown() > 1f) {
            lines.add(ReportDetailLine.chemical(
                    getString(R.string.chemical_ph_down),
                    formatChem(row.getPhDown())
            ));
        }
        if (row.getNoPhos() > 1f) {
            lines.add(ReportDetailLine.chemical(
                    getString(R.string.chemical_no_phos),
                    formatChem(row.getNoPhos())
            ));
        }
        return lines;
    }

    @NonNull
    private static String formatChem(float value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
