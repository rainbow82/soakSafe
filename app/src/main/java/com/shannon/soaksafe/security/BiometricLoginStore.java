package com.shannon.soaksafe.security;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Stores which local account may use fingerprint sign-in. Does not store passwords.
 * Uses app-private {@link SharedPreferences} (not encrypted): the biometric prompt is the
 * gate; only user id and username are stored so the app can load the account after auth.
 */
public final class BiometricLoginStore {

    private static final String PREF = "soaksafe_biometric_login";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";

    private BiometricLoginStore() {
    }

    @NonNull
    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static void saveEnrollment(
            @NonNull Context context,
            long userId,
            @NonNull String username
    ) {
        prefs(context).edit()
                .putBoolean(KEY_ENABLED, true)
                .putLong(KEY_USER_ID, userId)
                .putString(KEY_USERNAME, username.trim())
                .commit();
    }

    public static void clear(@NonNull Context context) {
        prefs(context).edit().clear().commit();
    }

    public static long getEnrolledUserId(@NonNull Context context) {
        return prefs(context).getLong(KEY_USER_ID, -1L);
    }

    @NonNull
    public static String getEnrolledUsername(@NonNull Context context) {
        String u = prefs(context).getString(KEY_USERNAME, "");
        return u != null ? u : "";
    }

    @Nullable
    public static Enrollment readEnrollment(@NonNull Context context) {
        if (!isEnabled(context)) {
            return null;
        }
        long userId = getEnrolledUserId(context);
        String username = getEnrolledUsername(context);
        if (userId <= 0L || username.isEmpty()) {
            return null;
        }
        return new Enrollment(userId, username);
    }

    public static final class Enrollment {
        public final long userId;
        @NonNull
        public final String username;

        Enrollment(long userId, @NonNull String username) {
            this.userId = userId;
            this.username = username;
        }
    }
}
