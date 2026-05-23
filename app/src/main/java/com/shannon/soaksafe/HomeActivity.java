package com.shannon.soaksafe;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.shannon.soaksafe.data.User;
import com.shannon.soaksafe.data.UserRepository;
import com.shannon.soaksafe.databinding.ActivityHomeBinding;
import com.shannon.soaksafe.security.BiometricLoginStore;
import com.shannon.soaksafe.security.BiometricSignInHelper;
import com.shannon.soaksafe.util.AppBarInsetsHelper;
import com.shannon.soaksafe.util.UserSessionPreferences;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private UserRepository userRepository;
    private boolean suppressBiometricSwitchCallback;

    private final ActivityResultLauncher<Intent> createAccountLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            Snackbar.make(
                                    binding.getRoot(),
                                    R.string.account_created_sign_in,
                                    Snackbar.LENGTH_LONG
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppBarInsetsHelper.applyTopWindowInsets(binding.homeScroll);

        userRepository = ((SoakSafeApplication) getApplication()).getUserRepository();

        binding.buttonSignIn.setOnClickListener(v -> attemptSignIn());
        binding.buttonCreateAccount.setOnClickListener(v ->
                createAccountLauncher.launch(new Intent(this, CreateAccountActivity.class))
        );
        binding.buttonBiometricSignIn.setOnClickListener(v -> attemptBiometricSignIn());
        binding.switchBiometricLogin.setOnCheckedChangeListener(this::onBiometricSwitchChanged);

        refreshBiometricUi();
        maybeOfferBiometricSignIn();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBiometricUi();
    }

    /**
     * When fingerprint login is enrolled, prompt once per cold start so returning users
     * do not have to hunt for the button.
     */
    private void maybeOfferBiometricSignIn() {
        if (BiometricLoginStore.readEnrollment(this) == null) {
            return;
        }
        if (BiometricSignInHelper.check(this) != BiometricSignInHelper.Availability.READY) {
            return;
        }
        binding.getRoot().post(this::attemptBiometricSignIn);
    }

    private void refreshBiometricUi() {
        BiometricSignInHelper.Availability availability = BiometricSignInHelper.check(this);
        BiometricLoginStore.Enrollment enrollment = BiometricLoginStore.readEnrollment(this);

        boolean showControls = availability == BiometricSignInHelper.Availability.READY
                || availability == BiometricSignInHelper.Availability.NONE_ENROLLED;
        binding.switchBiometricLogin.setVisibility(
                showControls ? android.view.View.VISIBLE : android.view.View.GONE
        );
        binding.switchBiometricLogin.setEnabled(
                availability == BiometricSignInHelper.Availability.READY
        );

        binding.buttonBiometricSignIn.setVisibility(
                availability == BiometricSignInHelper.Availability.READY && enrollment != null
                        ? android.view.View.VISIBLE
                        : android.view.View.GONE
        );

        if (enrollment != null && binding.editUsername.getText() != null
                && binding.editUsername.getText().length() == 0) {
            binding.editUsername.setText(enrollment.username);
        }

        suppressBiometricSwitchCallback = true;
        binding.switchBiometricLogin.setChecked(enrollment != null);
        suppressBiometricSwitchCallback = false;
    }

    private void onBiometricSwitchChanged(CompoundButton button, boolean isChecked) {
        if (suppressBiometricSwitchCallback) {
            return;
        }
        if (!isChecked) {
            BiometricLoginStore.clear(this);
            Snackbar.make(binding.getRoot(), R.string.biometric_disabled, Snackbar.LENGTH_SHORT).show();
            refreshBiometricUi();
            return;
        }
        enableBiometricAfterPasswordCheck();
    }

    private void enableBiometricAfterPasswordCheck() {
        CharSequence userText = binding.editUsername.getText();
        CharSequence pass = binding.editPassword.getText();
        String username = userText != null ? userText.toString() : "";
        String password = pass != null ? pass.toString() : "";
        if (username.trim().isEmpty() || password.isEmpty()) {
            revertBiometricSwitch();
            Snackbar.make(binding.getRoot(), R.string.biometric_enable_requires_password, Snackbar.LENGTH_LONG)
                    .show();
            return;
        }
        userRepository.tryLogin(username, password, (result, signedInUser) -> {
            if (result != UserRepository.LoginResult.SUCCESS || signedInUser == null) {
                revertBiometricSwitch();
                Snackbar.make(
                        binding.getRoot(),
                        result == UserRepository.LoginResult.EMPTY_FIELDS
                                ? R.string.error_login_empty
                                : R.string.error_invalid_credentials,
                        Snackbar.LENGTH_LONG
                ).show();
                return;
            }
            BiometricSignInHelper.authenticate(this, new BiometricSignInHelper.Callback() {
                @Override
                public void onSuccess() {
                    BiometricLoginStore.saveEnrollment(
                            HomeActivity.this,
                            signedInUser.getId(),
                            signedInUser.getUsername()
                    );
                    Snackbar.make(binding.getRoot(), R.string.biometric_enabled, Snackbar.LENGTH_LONG).show();
                    refreshBiometricUi();
                }

                @Override
                public void onFailure(@Nullable CharSequence message) {
                    revertBiometricSwitch();
                    if (message != null) {
                        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
        });
    }

    private void revertBiometricSwitch() {
        suppressBiometricSwitchCallback = true;
        binding.switchBiometricLogin.setChecked(false);
        suppressBiometricSwitchCallback = false;
    }

    private void attemptBiometricSignIn() {
        BiometricLoginStore.Enrollment enrollment = BiometricLoginStore.readEnrollment(this);
        if (enrollment == null) {
            Snackbar.make(binding.getRoot(), R.string.biometric_enable_requires_password, Snackbar.LENGTH_SHORT)
                    .show();
            return;
        }
        BiometricSignInHelper.authenticate(this, new BiometricSignInHelper.Callback() {
            @Override
            public void onSuccess() {
                userRepository.loadUserById(enrollment.userId, user -> {
                    if (user == null) {
                        BiometricLoginStore.clear(HomeActivity.this);
                        refreshBiometricUi();
                        Snackbar.make(binding.getRoot(), R.string.biometric_account_missing, Snackbar.LENGTH_LONG)
                                .show();
                        return;
                    }
                    completeSignIn(user);
                });
            }

            @Override
            public void onFailure(@Nullable CharSequence message) {
                if (message != null) {
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void attemptSignIn() {
        CharSequence userText = binding.editUsername.getText();
        CharSequence pass = binding.editPassword.getText();
        String username = userText != null ? userText.toString() : "";
        String password = pass != null ? pass.toString() : "";

        userRepository.tryLogin(username, password, (result, signedInUser) -> {
            switch (result) {
                case SUCCESS:
                    if (signedInUser != null) {
                        completeSignIn(signedInUser);
                    }
                    break;
                case INVALID_CREDENTIALS:
                    Snackbar.make(
                            binding.getRoot(),
                            R.string.error_invalid_credentials,
                            Snackbar.LENGTH_LONG
                    ).show();
                    break;
                case EMPTY_FIELDS:
                    Snackbar.make(
                            binding.getRoot(),
                            R.string.error_login_empty,
                            Snackbar.LENGTH_LONG
                    ).show();
                    break;
                default:
                    break;
            }
        });
    }

    private void completeSignIn(@NonNull User signedInUser) {
        if (binding.switchBiometricLogin.isChecked()) {
            BiometricLoginStore.saveEnrollment(
                    this,
                    signedInUser.getId(),
                    signedInUser.getUsername()
            );
        }
        String display = signedInUser.getFullName();
        if (display == null || display.trim().isEmpty()) {
            display = signedInUser.getUsername();
        }
        UserSessionPreferences.saveLastSignedInUser(HomeActivity.this, signedInUser.getId(), display);
        Intent maintenance = new Intent(this, MaintenanceDetailsActivity.class);
        maintenance.putExtra(MaintenanceDetailsActivity.EXTRA_USERNAME, signedInUser.getUsername());
        maintenance.putExtra(MaintenanceDetailsActivity.EXTRA_USER_ID, signedInUser.getId());
        startActivity(maintenance);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
