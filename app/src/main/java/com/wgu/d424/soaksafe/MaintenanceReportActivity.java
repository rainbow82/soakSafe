package com.wgu.d424.soaksafe;

import android.content.Intent;
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
import com.wgu.d424.soaksafe.report.MaintenanceReportSearchFilter;
import com.wgu.d424.soaksafe.report.ReportEventRowsFactory;
import com.wgu.d424.soaksafe.util.AppBarInsetsHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

        adapter.setRows(ReportEventRowsFactory.toUiModels(this, filtered, dateTimeFormat));

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
}
