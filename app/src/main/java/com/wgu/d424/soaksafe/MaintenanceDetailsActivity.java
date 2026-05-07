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

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.wgu.d424.soaksafe.data.MaintenanceChecklist;
import com.wgu.d424.soaksafe.data.MaintenanceRepository;
import com.wgu.d424.soaksafe.summary.BriefMaintenanceSummary;
import com.wgu.d424.soaksafe.summary.MaintenanceSummaryStrategy;
import com.wgu.d424.soaksafe.summary.VerboseMaintenanceSummary;
import com.wgu.d424.soaksafe.databinding.ActivityMaintenanceDetailsBinding;
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

        binding.textMaintenanceDateValue.setOnClickListener(v -> {
            if (userId > 0L) {
                showDatePicker();
            }
        });

        binding.buttonSaveChemicals.setOnClickListener(v -> {
            if (userId <= 0L) {
                return;
            }
            saveChemicalsFromFields();
        });

        if (userId <= 0L) {
            binding.textMaintenanceDateValue.setText(R.string.maintenance_date_none);
            setChecklistEnabled(false);
            return;
        }

        wireTaskCheckboxes();

        maintenanceRepository.getSavedDateMillis(userId, millis -> {
            savedDateMillisUtc = millis;
            binding.textMaintenanceDateValue.setText(formatDisplayDate(millis));
        });

        reloadChecklist();
    }

    private void wireTaskCheckboxes() {
        binding.checkboxVacuum.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userId <= 0L) {
                return;
            }
            maintenanceRepository.setVacuum(userId, isChecked, () -> { });
        });
        binding.checkboxCleanSkimmer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userId <= 0L) {
                return;
            }
            maintenanceRepository.setCleanSkimmer(userId, isChecked, () -> { });
        });
        binding.checkboxAddWater.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userId <= 0L) {
                return;
            }
            maintenanceRepository.setAddWater(userId, isChecked, () -> { });
        });
        binding.checkboxBrushWalls.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (userId <= 0L) {
                return;
            }
            maintenanceRepository.setBrushWalls(userId, isChecked, () -> { });
        });
    }

    private void setChecklistEnabled(boolean enabled) {
        binding.checkboxVacuum.setEnabled(enabled);
        binding.checkboxCleanSkimmer.setEnabled(enabled);
        binding.checkboxAddWater.setEnabled(enabled);
        binding.checkboxBrushWalls.setEnabled(enabled);
        binding.editChlorine.setEnabled(enabled);
        binding.editPhUp.setEnabled(enabled);
        binding.editPhDown.setEnabled(enabled);
        binding.editNoPhos.setEnabled(enabled);
        binding.buttonSaveChemicals.setEnabled(enabled);
    }

    private void reloadChecklist() {
        if (userId <= 0L) {
            return;
        }
        maintenanceRepository.loadChecklist(userId, this::applyChecklistToUi);
    }

    private void applyChecklistToUi(@NonNull MaintenanceChecklist row) {
        binding.checkboxVacuum.setOnCheckedChangeListener(null);
        binding.checkboxCleanSkimmer.setOnCheckedChangeListener(null);
        binding.checkboxAddWater.setOnCheckedChangeListener(null);
        binding.checkboxBrushWalls.setOnCheckedChangeListener(null);

        binding.checkboxVacuum.setChecked(row.isVacuum());
        binding.checkboxCleanSkimmer.setChecked(row.isCleanSkimmer());
        binding.checkboxAddWater.setChecked(row.isAddWater());
        binding.checkboxBrushWalls.setChecked(row.isBrushWalls());

        binding.editChlorine.setText(floatFieldToText(row.getChlorine()));
        binding.editPhUp.setText(floatFieldToText(row.getPhUp()));
        binding.editPhDown.setText(floatFieldToText(row.getPhDown()));
        binding.editNoPhos.setText(floatFieldToText(row.getNoPhos()));

        wireTaskCheckboxes();
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

    private void saveChemicalsFromFields() {
        Float ch = parseChemical(binding.editChlorine.getText());
        Float up = parseChemical(binding.editPhUp.getText());
        Float down = parseChemical(binding.editPhDown.getText());
        Float np = parseChemical(binding.editNoPhos.getText());
        if (ch == null || up == null || down == null || np == null) {
            Snackbar.make(binding.getRoot(), R.string.error_chemical_number, Snackbar.LENGTH_LONG).show();
            return;
        }
        maintenanceRepository.saveChemicals(userId, ch, up, down, np, () ->
                Snackbar.make(binding.getRoot(), R.string.chemicals_saved, Snackbar.LENGTH_SHORT).show()
        );
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
                showChecklistSummarySnackbar(new BriefMaintenanceSummary())
        );
        binding.buttonBottomShare.setOnClickListener(v -> shareChecklistBrief());
        binding.buttonBottomDocument.setOnClickListener(v ->
                showMaintenanceSummaryDialog(new VerboseMaintenanceSummary())
        );
    }

    /**
     * Polymorphism: any {@link MaintenanceSummaryStrategy} can be passed; the UI does not depend
     * on concrete {@link BriefMaintenanceSummary} vs {@link VerboseMaintenanceSummary}.
     */
    private void showChecklistSummarySnackbar(@NonNull MaintenanceSummaryStrategy strategy) {
        if (userId <= 0L) {
            return;
        }
        maintenanceRepository.loadChecklist(userId, checklist -> {
            String text = strategy.summarize(checklist);
            Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.bottomActionBar)
                    .show();
        });
    }

    private void showMaintenanceSummaryDialog(@NonNull MaintenanceSummaryStrategy strategy) {
        if (userId <= 0L) {
            return;
        }
        maintenanceRepository.loadChecklist(userId, checklist -> {
            String message = strategy.summarize(checklist);
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.maintenance_summary_dialog_title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    private void shareChecklistBrief() {
        if (userId <= 0L) {
            return;
        }
        MaintenanceSummaryStrategy strategy = new BriefMaintenanceSummary();
        maintenanceRepository.loadChecklist(userId, checklist -> {
            String text = strategy.summarize(checklist);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, text);
            Intent chooser = Intent.createChooser(send, getString(R.string.share_checklist_chooser_title));
            if (send.resolveActivity(getPackageManager()) != null) {
                startActivity(chooser);
            } else {
                Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.bottomActionBar)
                        .show();
            }
        });
    }
}
