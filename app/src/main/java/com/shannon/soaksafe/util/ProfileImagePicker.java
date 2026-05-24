package com.shannon.soaksafe.util;

import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.imageview.ShapeableImageView;

/**
 * Launches the system photo picker and previews the chosen image on a circular avatar view.
 */
public final class ProfileImagePicker {

    public interface Listener {
        void onImagePicked(@NonNull Uri uri);
    }

    private final ShapeableImageView imageView;
    private final AppCompatActivity activity;
    private final ActivityResultLauncher<PickVisualMediaRequest> picker;

    public ProfileImagePicker(
            @NonNull AppCompatActivity activity,
            @NonNull ShapeableImageView imageView,
            @NonNull Listener listener
    ) {
        this.activity = activity;
        this.imageView = imageView;
        picker = activity.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        listener.onImagePicked(uri);
                        ProfileImageStore.bindUriPreview(imageView, activity, uri);
                    }
                }
        );
        imageView.setOnClickListener(v -> launch());
    }

    public void launch() {
        picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    public void bindExisting(long userId) {
        ProfileImageStore.bindAvatar(imageView, activity, userId);
    }

    public void bindUri(@Nullable Uri uri) {
        if (uri != null) {
            ProfileImageStore.bindUriPreview(imageView, activity, uri);
        } else {
            ProfileImageStore.bindPlaceholder(imageView);
        }
    }
}
