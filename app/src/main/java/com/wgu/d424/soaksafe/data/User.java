package com.wgu.d424.soaksafe.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "users",
        indices = {@Index(value = "username", unique = true)}
)
public class User {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String username = "";

    @NonNull
    private String password = "";

    @NonNull
    private String fullName = "";

    @ColumnInfo(name = "pool_size_gallons")
    private int poolSizeGallons;

    @ColumnInfo(name = "pool_salt_water")
    private boolean poolSaltWater;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NonNull String username) {
        this.username = username;
    }

    @NonNull
    public String getPassword() {
        return password;
    }

    public void setPassword(@NonNull String password) {
        this.password = password;
    }

    @NonNull
    public String getFullName() {
        return fullName;
    }

    public void setFullName(@NonNull String fullName) {
        this.fullName = fullName;
    }

    public int getPoolSizeGallons() {
        return poolSizeGallons;
    }

    public void setPoolSizeGallons(int poolSizeGallons) {
        this.poolSizeGallons = poolSizeGallons;
    }

    public boolean isPoolSaltWater() {
        return poolSaltWater;
    }

    public void setPoolSaltWater(boolean poolSaltWater) {
        this.poolSaltWater = poolSaltWater;
    }
}
