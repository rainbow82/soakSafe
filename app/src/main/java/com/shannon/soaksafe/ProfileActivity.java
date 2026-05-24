package com.shannon.soaksafe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.shannon.soaksafe.data.User;
import com.shannon.soaksafe.data.UserRepository;
import com.shannon.soaksafe.databinding.ActivityProfileBinding;
import com.shannon.soaksafe.security.BiometricLoginStore;
import com.shannon.soaksafe.util.AppBarInsetsHelper;
import com.shannon.soaksafe.util.ProfileImagePicker;
import com.shannon.soaksafe.util.ProfileImageStore;
import com.shannon.soaksafe.util.UserSessionPreferences;

public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USERNAME = "extra_username";
    public static final String EXTRA_PROFILE_IMAGE_CHANGED = "extra_profile_image_changed";

    private ActivityProfileBinding binding;
    private UserRepository userRepository;
    private ProfileImagePicker profileImagePicker;
    private long userId = -1L;
    @Nullable
    private Uri pendingProfileUri;
    private boolean removeProfilePhoto;
    private boolean profileImageChanged;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        AppBarInsetsHelper.applyTopWindowInsets(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.profile_title);
        }

        userRepository = ((SoakSafeApplication) getApplication()).getUserRepository();
        userId = getIntent().getLongExtra(EXTRA_USER_ID, -1L);
        if (userId <= 0L) {
            Snackbar.make(binding.getRoot(), R.string.error_profile_not_found, Snackbar.LENGTH_LONG).show();
            finish();
            return;
        }

        profileImagePicker = new ProfileImagePicker(
                this,
                binding.profilePhotoSection.imageProfilePhoto,
                uri -> {
                    pendingProfileUri = uri;
                    removeProfilePhoto = false;
                    profileImageChanged = true;
                    updatePhotoActions(true);
                }
        );
        binding.profilePhotoSection.buttonChangeProfilePhoto.setOnClickListener(v -> profileImagePicker.launch());
        binding.profilePhotoSection.buttonRemoveProfilePhoto.setOnClickListener(v -> {
            pendingProfileUri = null;
            removeProfilePhoto = true;
            profileImageChanged = true;
            ProfileImageStore.bindPlaceholder(binding.profilePhotoSection.imageProfilePhoto);
            updatePhotoActions(false);
        });

        binding.buttonSaveProfile.setOnClickListener(v -> attemptSaveProfile());
        loadProfile();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadProfile() {
        userRepository.loadUserById(userId, user -> {
            if (user == null) {
                Snackbar.make(binding.getRoot(), R.string.error_profile_not_found, Snackbar.LENGTH_LONG).show();
                finish();
                return;
            }
            binding.editUsername.setText(user.getUsername());
            binding.editPoolSize.setText(String.valueOf(user.getPoolSizeGallons()));
            int poolTypeId = user.isPoolSaltWater() ? R.id.button_pool_salt : R.id.button_pool_fresh;
            binding.togglePoolType.check(poolTypeId);
            profileImagePicker.bindExisting(userId);
            updatePhotoActions(ProfileImageStore.hasImage(this, userId));
        });
    }

    private void updatePhotoActions(boolean hasPhoto) {
        binding.profilePhotoSection.buttonChangeProfilePhoto.setText(
                hasPhoto ? R.string.profile_photo_change : R.string.profile_photo_add
        );
        binding.profilePhotoSection.buttonRemoveProfilePhoto.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
    }

    private void attemptSaveProfile() {
        CharSequence user = binding.editUsername.getText();
        CharSequence size = binding.editPoolSize.getText();
        String username = user != null ? user.toString() : "";
        String sizeStr = size != null ? size.toString().trim() : "";

        int poolSizeGallons;
        try {
            poolSizeGallons = Integer.parseInt(sizeStr);
        } catch (NumberFormatException e) {
            Snackbar.make(binding.getRoot(), R.string.error_pool_size_invalid, Snackbar.LENGTH_LONG).show();
            return;
        }

        int checkedType = binding.togglePoolType.getCheckedButtonId();
        if (checkedType == View.NO_ID) {
            Snackbar.make(binding.getRoot(), R.string.error_pool_size_invalid, Snackbar.LENGTH_LONG).show();
            return;
        }
        boolean poolSaltWater = checkedType == R.id.button_pool_salt;

        userRepository.updateProfile(
                userId,
                username,
                poolSizeGallons,
                poolSaltWater,
                (result, updatedUser) -> {
                    switch (result) {
                        case SUCCESS:
                            if (updatedUser != null) {
                                if (!applyProfilePhotoChanges()) {
                                    return;
                                }
                                onProfileSaved(updatedUser);
                            }
                            break;
                        case USERNAME_TAKEN:
                            Snackbar.make(
                                    binding.getRoot(),
                                    R.string.error_username_taken,
                                    Snackbar.LENGTH_LONG
                            ).show();
                            break;
                        case EMPTY_USERNAME:
                            Snackbar.make(
                                    binding.getRoot(),
                                    R.string.error_profile_username_empty,
                                    Snackbar.LENGTH_LONG
                            ).show();
                            break;
                        case INVALID_POOL_SIZE:
                            Snackbar.make(
                                    binding.getRoot(),
                                    R.string.error_pool_size_invalid,
                                    Snackbar.LENGTH_LONG
                            ).show();
                            break;
                        case USER_NOT_FOUND:
                            Snackbar.make(
                                    binding.getRoot(),
                                    R.string.error_profile_not_found,
                                    Snackbar.LENGTH_LONG
                            ).show();
                            finish();
                            break;
                        default:
                            break;
                    }
                }
        );
    }

    private boolean applyProfilePhotoChanges() {
        if (!profileImageChanged) {
            return true;
        }
        if (removeProfilePhoto) {
            ProfileImageStore.delete(this, userId);
            return true;
        }
        if (pendingProfileUri != null) {
            if (!ProfileImageStore.saveFromUri(this, userId, pendingProfileUri)) {
                Snackbar.make(binding.getRoot(), R.string.profile_photo_save_failed, Snackbar.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    private void onProfileSaved(@NonNull User user) {
        UserSessionPreferences.saveLastSignedInUser(this, user.getId(), user.getUsername());
        if (BiometricLoginStore.isEnabled(this)
                && BiometricLoginStore.getEnrolledUserId(this) == user.getId()) {
            BiometricLoginStore.saveEnrollment(this, user.getId(), user.getUsername());
        }
        Snackbar.make(binding.getRoot(), R.string.profile_saved, Snackbar.LENGTH_SHORT).show();
        Intent data = new Intent();
        data.putExtra(EXTRA_USERNAME, user.getUsername());
        data.putExtra(EXTRA_PROFILE_IMAGE_CHANGED, profileImageChanged);
        setResult(RESULT_OK, data);
        finish();
    }
}
