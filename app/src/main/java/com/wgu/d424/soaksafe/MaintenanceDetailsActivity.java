package com.wgu.d424.soaksafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.wgu.d424.soaksafe.data.MaintenanceChecklist;
import com.wgu.d424.soaksafe.data.MaintenanceRepository;
import com.wgu.d424.soaksafe.databinding.ActivityMaintenanceDetailsBinding;
import com.wgu.d424.soaksafe.databinding.ItemMaintenanceChipRowBinding;
import com.wgu.d424.soaksafe.util.AppBarInsetsHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
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

        if (userId <= 0L) {
            setChipsEnabled(false);
            return;
        }

        maintenanceRepository.saveDateMillis(userId, todayMillis, () -> {
            savedDateMillisUtc = todayMillis;
            binding.textMaintenanceDateValue.setText(formatDisplayDate(todayMillis));
        });

        reloadChecklist();
    }

    private void inflateMaintenanceChips() {
        for (int i = 0; i < TASK_LABELS.length; i++) {
            ItemMaintenanceChipRowBinding chip = ItemMaintenanceChipRowBinding.inflate(
                    getLayoutInflater(),
                    binding.containerMaintenanceChips,
                    true
            );
            chip.textChipLabel.setText(TASK_LABELS[i]);
            chip.editChipAmount.setVisibility(android.view.View.GONE);
            String label = getString(TASK_LABELS[i]);
            chip.checkboxChip.setContentDescription(label);
            taskChipBindings[i] = chip;
        }
        for (int i = 0; i < CHEM_LABELS.length; i++) {
            ItemMaintenanceChipRowBinding chip = ItemMaintenanceChipRowBinding.inflate(
                    getLayoutInflater(),
                    binding.containerMaintenanceChips,
                    true
            );
            chip.textChipLabel.setText(CHEM_LABELS[i]);
            chip.editChipAmount.setVisibility(android.view.View.VISIBLE);
            chip.editChipAmount.setEnabled(false);
            chip.editChipAmount.setAlpha(0.55f);
            String label = getString(CHEM_LABELS[i]);
            chip.checkboxChip.setContentDescription(label);
            wireChemicalChip(chip);
            chemChipBindings[i] = chip;
        }
    }

    private void wireChemicalChip(@NonNull ItemMaintenanceChipRowBinding chip) {
        chip.checkboxChip.setOnCheckedChangeListener((v, checked) -> {
            chip.editChipAmount.setEnabled(checked);
            chip.editChipAmount.setAlpha(checked ? 1f : 0.55f);
            if (!checked) {
                chip.editChipAmount.setText("");
            }
        });
    }

    private void setChipsEnabled(boolean enabled) {
        for (ItemMaintenanceChipRowBinding chip : taskChipBindings) {
            if (chip != null) {
                chip.checkboxChip.setEnabled(enabled);
            }
        }
        for (ItemMaintenanceChipRowBinding chip : chemChipBindings) {
            if (chip != null) {
                chip.checkboxChip.setEnabled(enabled);
                boolean on = chip.checkboxChip.isChecked();
                chip.editChipAmount.setEnabled(enabled && on);
                chip.editChipAmount.setAlpha((enabled && on) ? 1f : 0.55f);
            }
        }
        binding.buttonSaveMaintenance.setEnabled(enabled);
    }

    private void reloadChecklist() {
        if (userId <= 0L) {
            return;
        }
        maintenanceRepository.loadChecklist(userId, this::applyChecklistToUi);
    }

    private void applyChecklistToUi(@NonNull MaintenanceChecklist row) {
        ItemMaintenanceChipRowBinding v = taskChipBindings[0];
        ItemMaintenanceChipRowBinding s = taskChipBindings[1];
        ItemMaintenanceChipRowBinding w = taskChipBindings[2];
        ItemMaintenanceChipRowBinding b = taskChipBindings[3];
        if (v != null) {
            v.checkboxChip.setChecked(row.isVacuum());
        }
        if (s != null) {
            s.checkboxChip.setChecked(row.isCleanSkimmer());
        }
        if (w != null) {
            w.checkboxChip.setChecked(row.isAddWater());
        }
        if (b != null) {
            b.checkboxChip.setChecked(row.isBrushWalls());
        }

        applyChemicalChipFromValue(chemChipBindings[0], row.getChlorine());
        applyChemicalChipFromValue(chemChipBindings[1], row.getPhUp());
        applyChemicalChipFromValue(chemChipBindings[2], row.getPhDown());
        applyChemicalChipFromValue(chemChipBindings[3], row.getNoPhos());
    }

    private void applyChemicalChipFromValue(
            @Nullable ItemMaintenanceChipRowBinding chip,
            float storedValue
    ) {
        if (chip == null) {
            return;
        }
        chip.checkboxChip.setOnCheckedChangeListener(null);
        boolean on = storedValue > 0f;
        chip.checkboxChip.setChecked(on);
        chip.editChipAmount.setEnabled(on);
        chip.editChipAmount.setAlpha(on ? 1f : 0.55f);
        chip.editChipAmount.setText(on ? floatFieldToText(storedValue) : "");
        wireChemicalChip(chip);
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
        boolean vacuum = taskChipBindings[0] != null && taskChipBindings[0].checkboxChip.isChecked();
        boolean skimmer = taskChipBindings[1] != null && taskChipBindings[1].checkboxChip.isChecked();
        boolean water = taskChipBindings[2] != null && taskChipBindings[2].checkboxChip.isChecked();
        boolean brush = taskChipBindings[3] != null && taskChipBindings[3].checkboxChip.isChecked();

        Float ch = readChemAmount(chemChipBindings[0]);
        Float up = readChemAmount(chemChipBindings[1]);
        Float down = readChemAmount(chemChipBindings[2]);
        Float np = readChemAmount(chemChipBindings[3]);
        if (ch == null || up == null || down == null || np == null) {
            Snackbar.make(binding.getRoot(), R.string.error_chemical_number, Snackbar.LENGTH_LONG).show();
            return;
        }

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
                () -> Snackbar.make(binding.getRoot(), R.string.maintenance_saved, Snackbar.LENGTH_SHORT).show()
        );
    }

    /**
     * @return null if checkbox on but amount invalid; 0f if checkbox off
     */
    @Nullable
    private Float readChemAmount(@Nullable ItemMaintenanceChipRowBinding chip) {
        if (chip == null) {
            return 0f;
        }
        if (!chip.checkboxChip.isChecked()) {
            return 0f;
        }
        return parseChemical(chip.editChipAmount.getText());
    }

    @Nullable
    private Float parseChemical(@Nullable CharSequence text) {
        if (text == null) {
            return 0f;
        }
        String t = text.toString().trim();
        if (t.isEmpty()) {
            return 0f;
        }
        try {
            return Float.parseFloat(t);
        } catch (NumberFormatException e) {
            return null;
        }
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

    @NonNull
    private CharSequence formatDisplayDate(@Nullable Long millisUtc) {
        if (millisUtc == null) {
            return displayDateFormat.format(new Date(System.currentTimeMillis()));
        }
        return displayDateFormat.format(new Date(millisUtc));
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
        binding.buttonBottomShare.setOnClickListener(v -> {
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_checklist_chooser_title));
            Intent chooser = Intent.createChooser(send, getString(R.string.share_checklist_chooser_title));
            startActivity(chooser);
        });
        binding.buttonBottomDocument.setOnClickListener(v -> {
            Intent report = new Intent(this, MaintenanceReportActivity.class);
            report.putExtra(MaintenanceReportActivity.EXTRA_USER_ID, userId);
            startActivity(report);
        });
    }
}
