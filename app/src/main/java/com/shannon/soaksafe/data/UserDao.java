package com.shannon.soaksafe.data;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {

    @Query("SELECT COUNT(*) FROM users WHERE username = :username COLLATE NOCASE")
    int countByUsername(@NonNull String username);

    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE LIMIT 1")
    User getByUsernameSync(@NonNull String username);

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    User getByIdSync(long userId);

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(@NonNull User user);

    @Query("UPDATE users SET password = :passwordHash WHERE id = :userId")
    void updatePasswordHash(long userId, @NonNull String passwordHash);
}
