package com.wgu.d424.soaksafe.util;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.AppBarLayout;

public final class AppBarInsetsHelper {

    private AppBarInsetsHelper() {
    }

    public static void applyStatusBarAndCutout(@NonNull AppBarLayout appBarLayout) {
        applyTopWindowInsets(appBarLayout);
    }

    public static void applyTopWindowInsets(@NonNull View root) {
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets topInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(0, topInsets.top, 0, 0);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
