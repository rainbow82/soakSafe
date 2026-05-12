package com.shannon.soaksafe.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Remembers the last signed-in user so the home screen can offer PDF export without re-entering
 * credentials. Cleared on logout.
 */
public final class UserSessionPreferences {

    private static final String PREF = "soaksafe_session";
    private static final String KEY_USER_ID = "last_user_id";
    private static final String KEY_DISPLAY = "last_display_name";

    private UserSessionPreferences() {
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void saveLastSignedInUser(
            @NonNull Context context,
            long userId,
            @NonNull String displayNameOrUsername
    ) {
        prefs(context).edit()
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_DISPLAY, displayNameOrUsername.trim())
                .apply();
    }

    public static long getLastUserId(@NonNull Context context) {
        return prefs(context).getLong(KEY_USER_ID, -1L);
    }

    @NonNull
    public static String getLastDisplayName(@NonNull Context context) {
        return prefs(context).getString(KEY_DISPLAY, "");
    }

    public static void clear(@NonNull Context context) {
        prefs(context).edit().clear().apply();
    }
}
