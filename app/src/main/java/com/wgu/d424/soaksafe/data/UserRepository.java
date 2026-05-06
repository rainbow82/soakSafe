package com.wgu.d424.soaksafe.data;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {

    public interface Callback<T> {
        void onResult(@Nullable T result);
    }

    public enum LoginResult {
        SUCCESS,
        INVALID_CREDENTIALS,
        EMPTY_FIELDS
    }

    public interface LoginCallback {
        void onLogin(@NonNull LoginResult result, @Nullable User user);
    }

    public enum RegisterResult {
        SUCCESS,
        USERNAME_TAKEN,
        EMPTY_FIELDS
    }

    public interface RegisterCallback {
        void onRegister(@NonNull RegisterResult result);
    }

    private final UserDao userDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public UserRepository(@NonNull UserDao userDao) {
        this.userDao = userDao;
    }

    public void tryLogin(
            @NonNull String username,
            @NonNull String password,
            @NonNull LoginCallback callback
    ) {
        String u = username.trim();
        String p = password;
        if (u.isEmpty() || p.isEmpty()) {
            callback.onLogin(LoginResult.EMPTY_FIELDS, null);
            return;
        }
        executor.execute(() -> {
            User row = userDao.getByUsernameSync(u);
            LoginResult result;
            if (row == null || !row.getPassword().equals(p)) {
                result = LoginResult.INVALID_CREDENTIALS;
                mainHandler.post(() -> callback.onLogin(result, null));
            } else {
                result = LoginResult.SUCCESS;
                mainHandler.post(() -> callback.onLogin(result, row));
            }
        });
    }

    public void registerUser(
            @NonNull String fullName,
            @NonNull String username,
            @NonNull String password,
            @NonNull RegisterCallback callback
    ) {
        String name = fullName.trim();
        String u = username.trim();
        String p = password;
        if (name.isEmpty() || u.isEmpty() || p.isEmpty()) {
            callback.onRegister(RegisterResult.EMPTY_FIELDS);
            return;
        }
        executor.execute(() -> {
            RegisterResult result;
            if (userDao.countByUsername(u) > 0) {
                result = RegisterResult.USERNAME_TAKEN;
            } else {
                User user = new User();
                user.setFullName(name);
                user.setUsername(u);
                user.setPassword(p);
                userDao.insert(user);
                result = RegisterResult.SUCCESS;
            }
            mainHandler.post(() -> callback.onRegister(result));
        });
    }
}
