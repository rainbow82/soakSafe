package com.shannon.soaksafe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.shannon.soaksafe.data.MaintenanceCustomLinesCodec;
import com.shannon.soaksafe.data.MaintenanceEvent;
import com.shannon.soaksafe.data.MaintenanceRepository;
import com.shannon.soaksafe.databinding.ActivityMaintenanceDetailsBinding;
import com.shannon.soaksafe.databinding.DialogAddCustomMaintenanceItemBinding;
import com.shannon.soaksafe.databinding.DialogChemicalAmountBinding;
import com.shannon.soaksafe.databinding.DialogSearchMaintenanceReportsBinding;
import com.shannon.soaksafe.databinding.ItemMaintenanceChipRowBinding;
import com.shannon.soaksafe.report.MaintenanceEventAdapter;
import com.shannon.soaksafe.report.MaintenanceReportSearchFilter;
import com.shannon.soaksafe.report.ReportEventRowsFactory;
import com.shannon.soaksafe.util.AppBarInsetsHelper;
import com.shannon.soaksafe.util.UserSessionPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MaintenanceDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_USERNAME = "extra_username";
    public static final String EXTRA_USER_ID = "extra_user_id";

    private static final int[] TASK_LABELS = {
            R.string.task_vacuum,
            R.string.task_clean_skimmer,
            R.string.task_add_water,
            R.string.task_brush_walls
    };

    private static final int[] CHEM_LABELS = {
            R.string.chemical_chlorine,
            R.string.chemical_ph_up,
            R.string.chemical_ph_down,
            R.string.chemical_no_phos
    };

    private ActivityMaintenanceDetailsBinding binding;
    private MaintenanceRepository maintenanceRepository;
    private long userId = -1L;
    @Nullable
    private Long savedDateMillisUtc;

    private final ItemMaintenanceChipRowBinding[] taskChipBindings = new ItemMaintenanceChipRowBinding[4];
    private final ItemMaintenanceChipRowBinding[] chemChipBindings = new ItemMaintenanceChipRowBinding[4];
    private final float[] chemStoredAmounts = new float[4];
    private final List<CustomLineVm> customLines = new ArrayList<>();

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

        long todayMillis = System.currentTimeMillis();
        savedDateMillisUtc = todayMillis;
        binding.textMaintenanceDateValue.setText(formatDisplayDate(todayMillis));

        userId = getIntent().getLongExtra(EXTRA_USER_ID, -1L);
        maintenanceRepository = ((SoakSafeApplication) getApplication()).getMaintenanceRepository();

        binding.textMaintenanceDateValue.setClickable(false);
        binding.textMaintenanceDateValue.setFocusable(false);

        inflateMaintenanceChips();

        binding.buttonSaveMaintenance.setOnClickListener(v -> {
            if (userId <= 0L) {
                return;
            }
            saveMaintenanceFromChips();
        });

        binding.fabAddCustomItem.setOnClickListener(v -> showAddCustomItemDialog());

        if (userId <= 0L) {
            setChipsEnabled(false);
            binding.fabAddCustomItem.setVisibility(android.view.View.GONE);
            binding.buttonBottomSearch.setEnabled(false);
            binding.buttonBottomSearch.setAlpha(0.4f);
            binding.buttonBottomDocument.setEnabled(false);
            binding.buttonBottomDocument.setAlpha(0.4f);
            binding.buttonBottomPdf.setEnabled(false);
            binding.buttonBottomPdf.setAlpha(0.4f);
            return;
        }

        String usernameExtra = getIntent().getStringExtra(EXTRA_USERNAME);
        if (usernameExtra != null && !usernameExtra.trim().isEmpty()) {
            UserSessionPreferences.saveLastSignedInUser(this, userId, usernameExtra.trim());
        }

        maintenanceRepository.saveDateMillis(userId, todayMillis, () -> {
            savedDateMillisUtc = todayMillis;
            binding.textMaintenanceDateValue.setText(formatDisplayDate(todayMillis));
        });

        maintenanceRepository.loadChecklist(userId, row -> applyCustomLinesFromStored(row.getCustomLinesJson()));
    }

    private static final class CustomLineVm {
        @NonNull
        final ItemMaintenanceChipRowBinding binding;
        @NonNull
        final String baseLabel;
        @Nullable
        final Float amount;

        CustomLineVm(
                @NonNull ItemMaintenanceChipRowBinding binding,
                @NonNull String baseLabel,
                @Nullable Float amount
        ) {
            this.binding = binding;
            this.baseLabel = baseLabel;
            this.amount = amount;
        }
    }

    private void inflateMaintenanceChips() {
        customLines.clear();
        Arrays.fill(chemStoredAmounts, 0f);
        for (int i = 0; i < TASK_LABELS.length; i++) {
            ItemMaintenanceChipRowBinding chip = ItemMaintenanceChipRowBinding.inflate(
                    getLayoutInflater(),
                    binding.containerMaintenanceChips,
                    true
            );
            chip.textChipLabel.setText(TASK_LABELS[i]);
            chip.buttonChipToggle.setActivated(false);
            updateToggleVisuals(chip);
            taskChipBindings[i] = chip;
            wireTaskChip(i);
        }
        for (int i = 0; i < CHEM_LABELS.length; i++) {
            ItemMaintenanceChipRowBinding chip = ItemMaintenanceChipRowBinding.inflate(
                    getLayoutInflater(),
                    binding.containerMaintenanceChips,
                    true
            );
            chip.textChipLabel.setText(CHEM_LABELS[i]);
            chip.buttonChipToggle.setActivated(false);
            updateToggleVisuals(chip);
            refreshChemicalLabel(chip, i);
            chemChipBindings[i] = chip;
            wireChemicalChip(i);
        }
    }

    private void applyCustomLinesFromStored(@Nullable String json) {
        for (MaintenanceCustomLinesCodec.Entry e : MaintenanceCustomLinesCodec.decode(json)) {
            addCustomMaintenanceRow(e.label, e.selected, e.amount);
        }
    }

    private void addCustomMaintenanceRow(
            @NonNull String baseLabel,
            boolean selected,
            @Nullable Float amount
    ) {
        ItemMaintenanceChipRowBinding chip = ItemMaintenanceChipRowBinding.inflate(
                getLayoutInflater(),
                binding.containerMaintenanceChips,
                true
        );
        CustomLineVm vm = new CustomLineVm(chip, baseLabel, amount);
        chip.buttonChipToggle.setActivated(selected);
        refreshCustomLineUi(vm);
        wireCustomLine(vm);
        customLines.add(vm);
    }

    private void refreshCustomLineUi(@NonNull CustomLineVm vm) {
        if (vm.amount != null && vm.amount > 0f) {
            vm.binding.textChipLabel.setText(getString(
                    R.string.maintenance_item_with_amount,
                    vm.baseLabel,
                    floatFieldToText(vm.amount)
            ));
        } else {
            vm.binding.textChipLabel.setText(vm.baseLabel);
        }
        updateToggleVisuals(vm.binding);
    }

    private void wireCustomLine(@NonNull CustomLineVm vm) {
        vm.binding.buttonChipToggle.setOnClickListener(v -> {
            vm.binding.buttonChipToggle.setActivated(!vm.binding.buttonChipToggle.isActivated());
            updateToggleVisuals(vm.binding);
        });
    }

    private void showAddCustomItemDialog() {
        DialogAddCustomMaintenanceItemBinding dialogBinding =
                DialogAddCustomMaintenanceItemBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_custom_maintenance_item_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.add_report_line_button, null)
                .create();
        dialog.setOnShowListener(d2 -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = dialogBinding.editCustomItemName.getText() != null
                    ? dialogBinding.editCustomItemName.getText().toString().trim()
                    : "";
            if (name.isEmpty()) {
                Snackbar.make(binding.getRoot(), R.string.edit_line_name_required, Snackbar.LENGTH_SHORT).show();
                return;
            }
            String amtStr = dialogBinding.editCustomItemAmount.getText() != null
                    ? dialogBinding.editCustomItemAmount.getText().toString().trim()
                    : "";
            Float amount = null;
            if (!amtStr.isEmpty()) {
                try {
                    float parsed = Float.parseFloat(amtStr);
                    if (parsed <= 0f) {
                        Snackbar.make(binding.getRoot(), R.string.error_chemical_amount_required,
                                Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    amount = parsed;
                } catch (NumberFormatException e) {
                    Snackbar.make(binding.getRoot(), R.string.error_chemical_number, Snackbar.LENGTH_LONG).show();
                    return;
                }
            }
            // Include on Save → report / edit: task-only rows must be selected or they are omitted from
            // line_items_json (see MaintenanceCustomLinesCodec.selectedAsEventLineItems).
            boolean selected = true;
            addCustomMaintenanceRow(name, selected, amount);
            dialog.dismiss();
        }));
        dialog.show();
    }

    @NonNull
    private String buildCustomLinesJson() {
        List<MaintenanceCustomLinesCodec.Entry> entries = new ArrayList<>();
        for (CustomLineVm vm : customLines) {
            if (vm.baseLabel.trim().isEmpty()) {
                continue;
            }
            entries.add(new MaintenanceCustomLinesCodec.Entry(
                    vm.baseLabel.trim(),
                    vm.binding.buttonChipToggle.isActivated(),
                    vm.amount
            ));
        }
        return MaintenanceCustomLinesCodec.encode(entries);
    }

    private void wireTaskChip(int taskIndex) {
        ItemMaintenanceChipRowBinding chip = taskChipBindings[taskIndex];
        if (chip == null) {
            return;
        }
        chip.buttonChipToggle.setOnClickListener(v -> {
            chip.buttonChipToggle.setActivated(!chip.buttonChipToggle.isActivated());
            updateToggleVisuals(chip);
        });
    }

    private void wireChemicalChip(int chemIndex) {
        ItemMaintenanceChipRowBinding chip = chemChipBindings[chemIndex];
        if (chip == null) {
            return;
        }
        chip.buttonChipToggle.setOnClickListener(v -> onChemicalToggleClick(chip, chemIndex));
    }

    private void onChemicalToggleClick(@NonNull ItemMaintenanceChipRowBinding chip, int chemIndex) {
        if (chip.buttonChipToggle.isActivated()) {
            chip.buttonChipToggle.setActivated(false);
            chemStoredAmounts[chemIndex] = 0f;
            refreshChemicalLabel(chip, chemIndex);
            updateToggleVisuals(chip);
        } else {
            showChemicalAmountDialog(chip, chemIndex);
        }
    }

    private void showChemicalAmountDialog(@NonNull ItemMaintenanceChipRowBinding chip, int chemIndex) {
        DialogChemicalAmountBinding dialogBinding = DialogChemicalAmountBinding.inflate(getLayoutInflater());
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.maintenance_chemical_amount_dialog_title)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(android.R.string.ok, null)
                .create();
        dialog.setOnShowListener(d2 -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            CharSequence text = dialogBinding.editChemicalAmount.getText();
            String t = text != null ? text.toString().trim() : "";
            if (t.isEmpty()) {
                Snackbar.make(binding.getRoot(), R.string.error_chemical_amount_required, Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }
            float amount;
            try {
                amount = Float.parseFloat(t);
            } catch (NumberFormatException e) {
                Snackbar.make(binding.getRoot(), R.string.error_chemical_number, Snackbar.LENGTH_LONG).show();
                return;
            }
            if (amount <= 0f) {
                Snackbar.make(binding.getRoot(), R.string.error_chemical_amount_required, Snackbar.LENGTH_SHORT)
                        .show();
                return;
            }
            chemStoredAmounts[chemIndex] = amount;
            chip.buttonChipToggle.setActivated(true);
            refreshChemicalLabel(chip, chemIndex);
            updateToggleVisuals(chip);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void updateToggleVisuals(@NonNull ItemMaintenanceChipRowBinding chip) {
        boolean on = chip.buttonChipToggle.isActivated();
        chip.buttonChipToggle.setImageResource(on ? R.drawable.ic_check_24 : R.drawable.ic_add_24);
        chip.buttonChipToggle.setContentDescription(
                getString(on ? R.string.chip_added_content_description : R.string.chip_add_item_content_description)
        );
    }

    private void refreshChemicalLabel(@NonNull ItemMaintenanceChipRowBinding chip, int chemIndex) {
        String base = getString(CHEM_LABELS[chemIndex]);
        if (chip.buttonChipToggle.isActivated() && chemStoredAmounts[chemIndex] > 0f) {
            chip.textChipLabel.setText(getString(
                    R.string.maintenance_item_with_amount,
                    base,
                    floatFieldToText(chemStoredAmounts[chemIndex])
            ));
        } else {
            chip.textChipLabel.setText(base);
        }
    }

    private void setChipsEnabled(boolean enabled) {
        for (ItemMaintenanceChipRowBinding chip : taskChipBindings) {
            if (chip != null) {
                chip.buttonChipToggle.setEnabled(enabled);
            }
        }
        for (ItemMaintenanceChipRowBinding chip : chemChipBindings) {
            if (chip != null) {
                chip.buttonChipToggle.setEnabled(enabled);
            }
        }
        for (CustomLineVm vm : customLines) {
            vm.binding.buttonChipToggle.setEnabled(enabled);
        }
        binding.buttonSaveMaintenance.setEnabled(enabled);
        binding.fabAddCustomItem.setEnabled(enabled);
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

    private void saveMaintenanceFromChips() {
        boolean vacuum = taskChipBindings[0] != null && taskChipBindings[0].buttonChipToggle.isActivated();
        boolean skimmer = taskChipBindings[1] != null && taskChipBindings[1].buttonChipToggle.isActivated();
        boolean water = taskChipBindings[2] != null && taskChipBindings[2].buttonChipToggle.isActivated();
        boolean brush = taskChipBindings[3] != null && taskChipBindings[3].buttonChipToggle.isActivated();

        float ch = chemAmountForSave(0);
        float up = chemAmountForSave(1);
        float down = chemAmountForSave(2);
        float np = chemAmountForSave(3);

        maintenanceRepository.saveFullChecklist(
                userId,
                vacuum,
                skimmer,
                water,
                brush,
                ch,
                up,
                down,
                np,
                buildCustomLinesJson(),
                () -> Snackbar.make(binding.getRoot(), R.string.maintenance_saved, Snackbar.LENGTH_SHORT).show()
        );
    }

    private float chemAmountForSave(int chemIndex) {
        ItemMaintenanceChipRowBinding chip = chemChipBindings[chemIndex];
        if (chip == null || !chip.buttonChipToggle.isActivated()) {
            return 0f;
        }
        return chemStoredAmounts[chemIndex];
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
        UserSessionPreferences.clear(this);
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @NonNull
    private CharSequence formatDisplayDate(@Nullable Long millisUtc) {
        if (millisUtc == null) {
            return displayDateFormat.format(new Date(System.currentTimeMillis()));
        }
        return displayDateFormat.format(new Date(millisUtc));
    }

    /**
     * Straddles the blue / gray seam: layout bottom sits on the seam, then we translate down by
     * half the FAB height so the center is on the seam (Material FAB Coordinator behavior otherwise
     * fights a single margin-only solution).
     */
    private void showMaintenanceReportSearchModal() {
        if (userId <= 0L) {
            Snackbar.make(binding.getRoot(), R.string.report_no_events, Snackbar.LENGTH_SHORT).show();
            return;
        }
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        DialogSearchMaintenanceReportsBinding sheet =
                DialogSearchMaintenanceReportsBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());
        dialog.setOnShowListener(d -> {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);
        MaintenanceReportSearchFilter searchFilter = new MaintenanceReportSearchFilter(this);
        List<MaintenanceEvent> allEvents = new ArrayList<>();
        MaintenanceEventAdapter adapter = new MaintenanceEventAdapter(eventId -> {
            dialog.dismiss();
            Intent edit = new Intent(MaintenanceDetailsActivity.this, EditMaintenanceReportActivity.class);
            edit.putExtra(EditMaintenanceReportActivity.EXTRA_EVENT_ID, eventId);
            edit.putExtra(EditMaintenanceReportActivity.EXTRA_USER_ID, userId);
            startActivity(edit);
        });
        sheet.recyclerModalReportSearch.setLayoutManager(new LinearLayoutManager(this));
        sheet.recyclerModalReportSearch.setAdapter(adapter);

        Runnable applyFilter = () -> {
            String query = sheet.editModalReportSearch.getText() != null
                    ? sheet.editModalReportSearch.getText().toString()
                    : "";
            List<MaintenanceEvent> filtered = new ArrayList<>();
            for (MaintenanceEvent row : allEvents) {
                if (searchFilter.matches(row, query)) {
                    filtered.add(row);
                }
            }
            adapter.setRows(ReportEventRowsFactory.toUiModels(this, filtered, dateTimeFormat, query));

            if (allEvents.isEmpty()) {
                sheet.textModalSearchEmpty.setText(R.string.report_no_events);
                sheet.textModalSearchEmpty.setVisibility(View.VISIBLE);
                sheet.recyclerModalReportSearch.setVisibility(View.GONE);
            } else if (filtered.isEmpty()) {
                sheet.textModalSearchEmpty.setText(R.string.report_no_search_results);
                sheet.textModalSearchEmpty.setVisibility(View.VISIBLE);
                sheet.recyclerModalReportSearch.setVisibility(View.GONE);
            } else {
                sheet.textModalSearchEmpty.setVisibility(View.GONE);
                sheet.recyclerModalReportSearch.setVisibility(View.VISIBLE);
            }
        };

        sheet.editModalReportSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applyFilter.run();
            }
        });

        maintenanceRepository.loadEvents(userId, rows -> {
            allEvents.clear();
            allEvents.addAll(rows);
            applyFilter.run();
        });

        dialog.show();
    }

    private void applyFabBottomMargin(int barHeightFallbackPx) {
        int barH = binding.bottomActionBar.getHeight();
        if (barH <= 0) {
            barH = barHeightFallbackPx;
        }
        int fabH = binding.fabAddCustomItem.getHeight();
        if (fabH <= 0) {
            fabH = (int) (56 * getResources().getDisplayMetrics().density + 0.5f);
        }
        CoordinatorLayout.LayoutParams lp =
                (CoordinatorLayout.LayoutParams) binding.fabAddCustomItem.getLayoutParams();
        lp.setBehavior(null);
        lp.bottomMargin = barH;
        binding.fabAddCustomItem.setLayoutParams(lp);
        binding.fabAddCustomItem.setTranslationY(fabH / 2f);
        // Draw above bottom_action_bar (translation straddles the seam; bar must not paint over us).
        binding.fabAddCustomItem.bringToFront();
        float z = 12f * getResources().getDisplayMetrics().density;
        ViewCompat.setElevation(binding.fabAddCustomItem, z);
        ViewCompat.setTranslationZ(binding.fabAddCustomItem, z);
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
        int fabBarFallback = getResources().getDimensionPixelSize(R.dimen.fab_fallback_bottom_bar_height);
        Runnable syncFabMargin = () -> applyFabBottomMargin(fabBarFallback);
        binding.bottomActionBar.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, ob, ob2) -> syncFabMargin.run());
        binding.fabAddCustomItem.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, ob, ob2) -> syncFabMargin.run());
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionBar, (v, windowInsets) -> {
            Insets nav = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    barBottomBase + nav.bottom
            );
            v.post(syncFabMargin);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(binding.getRoot());
        binding.fabAddCustomItem.post(syncFabMargin);

        binding.buttonBottomSearch.setOnClickListener(v -> showMaintenanceReportSearchModal());
        binding.buttonBottomDocument.setOnClickListener(v -> {
            Intent report = new Intent(this, MaintenanceReportActivity.class);
            report.putExtra(MaintenanceReportActivity.EXTRA_USER_ID, userId);
            startActivity(report);
        });
        binding.buttonBottomPdf.setOnClickListener(v -> {
            if (userId <= 0L) {
                return;
            }
            Intent report = new Intent(this, MaintenanceReportActivity.class);
            report.putExtra(MaintenanceReportActivity.EXTRA_USER_ID, userId);
            report.putExtra(MaintenanceReportActivity.EXTRA_OPEN_PDF_WHEN_READY, true);
            startActivity(report);
        });
    }
}
