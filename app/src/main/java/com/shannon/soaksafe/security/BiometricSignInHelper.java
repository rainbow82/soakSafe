package com.shannon.soaksafe.security;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.shannon.soaksafe.R;

import java.util.concurrent.Executor;

/**
 * Wraps {@link BiometricPrompt} for optional fingerprint / device-biometric sign-in.
 */
public final class BiometricSignInHelper {

    public enum Availability {
        READY,
        NO_HARDWARE,
        NONE_ENROLLED,
        UNAVAILABLE
    }

    public interface Callback {
        void onSuccess();

        void onFailure(@Nullable CharSequence message);
    }

    private BiometricSignInHelper() {
    }

    private static int authenticators() {
        return BiometricManager.Authenticators.BIOMETRIC_WEAK;
    }

    @NonNull
    public static Availability check(@NonNull android.content.Context context) {
        BiometricManager manager = BiometricManager.from(context);
        return switch (manager.canAuthenticate(authenticators())) {
            case BiometricManager.BIOMETRIC_SUCCESS -> Availability.READY;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NONE_ENROLLED;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                 BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Availability.NO_HARDWARE;
            default -> Availability.UNAVAILABLE;
        };
    }

    public static void authenticate(
            @NonNull FragmentActivity activity,
            @NonNull Callback callback
    ) {
        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricPrompt prompt = new BiometricPrompt(
                activity,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result
                    ) {
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED
                                || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                                || errorCode == BiometricPrompt.ERROR_CANCELED) {
                            callback.onFailure(null);
                        } else {
                            callback.onFailure(errString);
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        callback.onFailure(activity.getString(R.string.biometric_not_recognized));
                    }
                }
        );
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(activity.getString(R.string.biometric_prompt_title))
                .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
                .setNegativeButtonText(activity.getString(android.R.string.cancel))
                .setAllowedAuthenticators(authenticators())
                .build();
        prompt.authenticate(info);
    }
}
