package com.wgu.d424.soaksafe.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

/**
 * One row per user: boolean task flags and chemical measurements (SQLite REAL columns).
 */
@Entity(
        tableName = "maintenance_checklist",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        )
)
public class MaintenanceChecklist {

    @PrimaryKey
    private long userId;

    private boolean vacuum;

    @ColumnInfo(name = "clean_skimmer")
    private boolean cleanSkimmer;

    @ColumnInfo(name = "add_water")
    private boolean addWater;

    @ColumnInfo(name = "brush_walls")
    private boolean brushWalls;

    private float chlorine;

    @ColumnInfo(name = "ph_up")
    private float phUp;

    @ColumnInfo(name = "ph_down")
    private float phDown;

    @ColumnInfo(name = "no_phos")
    private float noPhos;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public boolean isVacuum() {
        return vacuum;
    }

    public void setVacuum(boolean vacuum) {
        this.vacuum = vacuum;
    }

    public boolean isCleanSkimmer() {
        return cleanSkimmer;
    }

    public void setCleanSkimmer(boolean cleanSkimmer) {
        this.cleanSkimmer = cleanSkimmer;
    }

    public boolean isAddWater() {
        return addWater;
    }

    public void setAddWater(boolean addWater) {
        this.addWater = addWater;
    }

    public boolean isBrushWalls() {
        return brushWalls;
    }

    public void setBrushWalls(boolean brushWalls) {
        this.brushWalls = brushWalls;
    }

    public float getChlorine() {
        return chlorine;
    }

    public void setChlorine(float chlorine) {
        this.chlorine = chlorine;
    }

    public float getPhUp() {
        return phUp;
    }

    public void setPhUp(float phUp) {
        this.phUp = phUp;
    }

    public float getPhDown() {
        return phDown;
    }

    public void setPhDown(float phDown) {
        this.phDown = phDown;
    }

    public float getNoPhos() {
        return noPhos;
    }

    public void setNoPhos(float noPhos) {
        this.noPhos = noPhos;
    }
}
