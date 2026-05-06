package com.wgu.d424.soaksafe;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.wgu.d424.soaksafe.data.User;
import com.wgu.d424.soaksafe.data.UserRepository;
import com.wgu.d424.soaksafe.databinding.ActivityHomeBinding;
import com.wgu.d424.soaksafe.util.AppBarInsetsHelper;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;
    private UserRepository userRepository;

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
        AppBarInsetsHelper.applyTopWindowInsets(binding.getRoot());

        userRepository = ((SoakSafeApplication) getApplication()).getUserRepository();

        binding.buttonSignIn.setOnClickListener(v -> attemptSignIn());

        binding.buttonCreateAccount.setOnClickListener(v ->
                createAccountLauncher.launch(new Intent(this, CreateAccountActivity.class))
        );
    }

    private void attemptSignIn() {
        CharSequence userText = binding.editUsername.getText();
        CharSequence pass = binding.editPassword.getText();
        String username = userText != null ? userText.toString() : "";
        String password = pass != null ? pass.toString() : "";

        userRepository.tryLogin(username, password, (result, signedInUser) -> {
            switch (result) {
                case SUCCESS:
                    if (signedInUser == null) {
                        break;
                    }
                    Intent maintenance = new Intent(this, MaintenanceDetailsActivity.class);
                    maintenance.putExtra(
                            MaintenanceDetailsActivity.EXTRA_USERNAME,
                            signedInUser.getUsername()
                    );
                    maintenance.putExtra(
                            MaintenanceDetailsActivity.EXTRA_USER_ID,
                            signedInUser.getId()
                    );
                    startActivity(maintenance);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
}
