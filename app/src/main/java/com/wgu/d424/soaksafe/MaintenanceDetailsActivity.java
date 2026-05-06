package com.wgu.d424.soaksafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.wgu.d424.soaksafe.data.MaintenanceRepository;
import com.wgu.d424.soaksafe.databinding.ActivityMaintenanceDetailsBinding;
import com.wgu.d424.soaksafe.ui.MaintenanceTaskAdapter;
import com.wgu.d424.soaksafe.util.AppBarInsetsHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MaintenanceDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_USERNAME = "extra_username";
    public static final String EXTRA_USER_ID = "extra_user_id";

    private static final String DATE_PICKER_TAG = "maintenance_date_picker";

    private ActivityMaintenanceDetailsBinding binding;
    private MaintenanceRepository maintenanceRepository;
    private MaintenanceTaskAdapter taskAdapter;
    private long userId = -1L;
    @Nullable
    private Long savedDateMillisUtc;

    private final SimpleDateFormat displayDateFormat =
            new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMaintenanceDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppBarInsetsHelper.applyTopWindowInsets(binding.getRoot());
        setupBottomBarAndInsets();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setTitle(R.string.maintenance_details_title);
        }

        binding.toolbar.setNavigationOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.snackbar_menu_placeholder, Snackbar.LENGTH_SHORT)
                        .show()
        );

        userId = getIntent().getLongExtra(EXTRA_USER_ID, -1L);
        maintenanceRepository = ((SoakSafeApplication) getApplication()).getMaintenanceRepository();

        binding.recyclerMaintenanceTasks.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new MaintenanceTaskAdapter(new MaintenanceTaskAdapter.Listener() {
            @Override
            public void onCompletionToggled(@NonNull String taskKey, boolean nowChecked) {
                if (userId <= 0L) {
                    return;
                }
                maintenanceRepository.setTaskCompletedToday(userId, taskKey, nowChecked, () ->
                        reloadTaskRows()
                );
            }

            @Override
            public void onDeleteLatest(@NonNull String taskKey) {
                if (userId <= 0L) {
                    return;
                }
                maintenanceRepository.deleteLatestTaskCompletion(userId, taskKey, () ->
                        reloadTaskRows()
                );
            }
        });
        binding.recyclerMaintenanceTasks.setAdapter(taskAdapter);

        binding.textMaintenanceDateValue.setOnClickListener(v -> {
            if (userId > 0L) {
                showDatePicker();
            }
        });

        if (userId <= 0L) {
            binding.textMaintenanceDateValue.setText(R.string.maintenance_date_none);
            return;
        }

        maintenanceRepository.getSavedDateMillis(userId, millis -> {
            savedDateMillisUtc = millis;
            binding.textMaintenanceDateValue.setText(formatDisplayDate(millis));
        });

        reloadTaskRows();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_maintenance_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            showProfileMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showProfileMenu() {
        PopupMenu popup = new PopupMenu(this, binding.toolbar, Gravity.END);
        popup.getMenuInflater().inflate(R.menu.menu_profile_popup, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.popup_profile) {
                Snackbar.make(binding.getRoot(), R.string.menu_profile, Snackbar.LENGTH_SHORT).show();
                return true;
            }
            if (item.getItemId() == R.id.popup_logout) {
                logoutToHome();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void logoutToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void reloadTaskRows() {
        if (userId <= 0L) {
            return;
        }
        maintenanceRepository.loadTaskRows(userId, (rows, completedToday, total) ->
                taskAdapter.setRows(rows)
        );
    }

    @NonNull
    private CharSequence formatDisplayDate(@Nullable Long millisUtc) {
        if (millisUtc == null) {
            return getString(R.string.maintenance_date_none);
        }
        return displayDateFormat.format(new Date(millisUtc));
    }

    private void showDatePicker() {
        if (userId <= 0L) {
            return;
        }
        Long initial = savedDateMillisUtc != null
                ? savedDateMillisUtc
                : MaterialDatePicker.todayInUtcMilliseconds();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.maintenance_date_picker_title)
                .setSelection(initial)
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection == null) {
                return;
            }
            maintenanceRepository.saveDateMillis(userId, selection, () -> {
                savedDateMillisUtc = selection;
                binding.textMaintenanceDateValue.setText(formatDisplayDate(selection));
                Snackbar.make(
                        binding.getRoot(),
                        R.string.maintenance_date_saved,
                        Snackbar.LENGTH_SHORT
                ).show();
            });
        });
        picker.show(getSupportFragmentManager(), DATE_PICKER_TAG);
    }

    private void setupBottomBarAndInsets() {
        int baseScrollBottom = getResources().getDimensionPixelSize(R.dimen.maintenance_scroll_bottom_padding);
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollMaintenance, (v, windowInsets) -> {
            Insets nav = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    baseScrollBottom + nav.bottom
            );
            return windowInsets;
        });
        int barBottomBase = (int) (8 * getResources().getDisplayMetrics().density + 0.5f);
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionBar, (v, windowInsets) -> {
            Insets nav = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    barBottomBase + nav.bottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());

        binding.buttonBottomSearch.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.bottom_bar_search, Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.bottomActionBar)
                        .show()
        );
        binding.buttonBottomShare.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.bottom_bar_share, Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.bottomActionBar)
                        .show()
        );
        binding.buttonBottomDocument.setOnClickListener(v ->
                Snackbar.make(binding.getRoot(), R.string.bottom_bar_document, Snackbar.LENGTH_SHORT)
                        .setAnchorView(binding.bottomActionBar)
                        .show()
        );
    }
}
