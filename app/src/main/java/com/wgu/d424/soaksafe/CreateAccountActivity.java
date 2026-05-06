package com.wgu.d424.soaksafe;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.wgu.d424.soaksafe.data.UserRepository;
import com.wgu.d424.soaksafe.databinding.ActivityCreateAccountBinding;
import com.wgu.d424.soaksafe.util.AppBarInsetsHelper;

public class CreateAccountActivity extends AppCompatActivity {

    private ActivityCreateAccountBinding binding;
    private UserRepository userRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppBarInsetsHelper.applyTopWindowInsets(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.create_account_title);
        }

        userRepository = ((SoakSafeApplication) getApplication()).getUserRepository();

        binding.buttonSaveAccount.setOnClickListener(v -> attemptCreateAccount());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void attemptCreateAccount() {
        CharSequence name = binding.editFullName.getText();
        CharSequence user = binding.editUsername.getText();
        CharSequence pass = binding.editPassword.getText();
        String fullName = name != null ? name.toString() : "";
        String username = user != null ? user.toString() : "";
        String password = pass != null ? pass.toString() : "";

        userRepository.registerUser(fullName, username, password, result -> {
            switch (result) {
                case SUCCESS:
                    setResult(RESULT_OK);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    break;
                case USERNAME_TAKEN:
                    Snackbar.make(
                            binding.getRoot(),
                            R.string.error_username_taken,
                            Snackbar.LENGTH_LONG
                    ).show();
                    break;
                case EMPTY_FIELDS:
                    Snackbar.make(
                            binding.getRoot(),
                            R.string.error_create_empty,
                            Snackbar.LENGTH_LONG
                    ).show();
                    break;
                default:
                    break;
            }
        });
    }
}
