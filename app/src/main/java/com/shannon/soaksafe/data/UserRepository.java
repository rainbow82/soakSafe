package com.shannon.soaksafe.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.shannon.soaksafe.base.AsyncRepositoryBase;
import com.shannon.soaksafe.security.PasswordHasher;

public class UserRepository extends AsyncRepositoryBase {

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
        runInBackground(() -> {
            User row = userDao.getByUsernameSync(u);
            LoginResult result;
            if (row == null || !PasswordHasher.verify(row.getPassword(), p)) {
                result = LoginResult.INVALID_CREDENTIALS;
                runOnMainThread(() -> callback.onLogin(result, null));
            } else {
                if (!PasswordHasher.isModernStoredForm(row.getPassword())) {
                    userDao.updatePasswordHash(row.getId(), PasswordHasher.hash(p));
                }
                result = LoginResult.SUCCESS;
                runOnMainThread(() -> callback.onLogin(result, row));
            }
        });
    }

    public void registerUser(
            @NonNull String fullName,
            @NonNull String username,
            @NonNull String password,
            int poolSizeGallons,
            boolean poolSaltWater,
            @NonNull RegisterCallback callback
    ) {
        String name = fullName.trim();
        String u = username.trim();
        String p = password;
        if (name.isEmpty() || u.isEmpty() || p.isEmpty()) {
            callback.onRegister(RegisterResult.EMPTY_FIELDS);
            return;
        }
        runInBackground(() -> {
            RegisterResult result;
            if (userDao.countByUsername(u) > 0) {
                result = RegisterResult.USERNAME_TAKEN;
            } else {
                User user = new User();
                user.setFullName(name);
                user.setUsername(u);
                user.setPassword(PasswordHasher.hash(p));
                user.setPoolSizeGallons(poolSizeGallons);
                user.setPoolSaltWater(poolSaltWater);
                userDao.insert(user);
                result = RegisterResult.SUCCESS;
            }
            runOnMainThread(() -> callback.onRegister(result));
        });
    }
}
