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
        void onRegister(@NonNull RegisterResult result, @Nullable User user);
    }

    public enum UpdateProfileResult {
        SUCCESS,
        USERNAME_TAKEN,
        EMPTY_USERNAME,
        INVALID_POOL_SIZE,
        USER_NOT_FOUND
    }

    public interface UpdateProfileCallback {
        void onUpdate(@NonNull UpdateProfileResult result, @Nullable User user);
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

    public void loadUserById(long userId, @NonNull Callback<User> callback) {
        runInBackground(() -> {
            User row = userId > 0L ? userDao.getByIdSync(userId) : null;
            runOnMainThread(() -> callback.onResult(row));
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
            callback.onRegister(RegisterResult.EMPTY_FIELDS, null);
            return;
        }
        runInBackground(() -> {
            RegisterResult result;
            User created = null;
            if (userDao.countByUsername(u) > 0) {
                result = RegisterResult.USERNAME_TAKEN;
            } else {
                User user = new User();
                user.setFullName(name);
                user.setUsername(u);
                user.setPassword(PasswordHasher.hash(p));
                user.setPoolSizeGallons(poolSizeGallons);
                user.setPoolSaltWater(poolSaltWater);
                long id = userDao.insert(user);
                user.setId(id);
                created = user;
                result = RegisterResult.SUCCESS;
            }
            User finalCreated = created;
            RegisterResult finalResult = result;
            runOnMainThread(() -> callback.onRegister(finalResult, finalCreated));
        });
    }

    public void updateProfile(
            long userId,
            @NonNull String username,
            int poolSizeGallons,
            boolean poolSaltWater,
            boolean poolAboveGround,
            @NonNull UpdateProfileCallback callback
    ) {
        String u = username.trim();
        if (u.isEmpty()) {
            callback.onUpdate(UpdateProfileResult.EMPTY_USERNAME, null);
            return;
        }
        if (poolSizeGallons <= 0) {
            callback.onUpdate(UpdateProfileResult.INVALID_POOL_SIZE, null);
            return;
        }
        runInBackground(() -> {
            User row = userId > 0L ? userDao.getByIdSync(userId) : null;
            if (row == null) {
                runOnMainThread(() -> callback.onUpdate(UpdateProfileResult.USER_NOT_FOUND, null));
                return;
            }
            UpdateProfileResult result;
            User updated = null;
            if (!row.getUsername().equalsIgnoreCase(u) && userDao.countByUsernameForOtherUser(u, userId) > 0) {
                result = UpdateProfileResult.USERNAME_TAKEN;
            } else {
                userDao.updateProfile(userId, u, poolSizeGallons, poolSaltWater, poolAboveGround);
                row.setUsername(u);
                row.setPoolSizeGallons(poolSizeGallons);
                row.setPoolSaltWater(poolSaltWater);
                row.setPoolAboveGround(poolAboveGround);
                updated = row;
                result = UpdateProfileResult.SUCCESS;
            }
            User finalUpdated = updated;
            UpdateProfileResult finalResult = result;
            runOnMainThread(() -> callback.onUpdate(finalResult, finalUpdated));
        });
    }
}
