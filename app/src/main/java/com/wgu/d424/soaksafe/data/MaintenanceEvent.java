package com.wgu.d424.soaksafe.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "maintenance_events",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index("userId")
)
public class MaintenanceEvent {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long userId;

    @NonNull
    @ColumnInfo(name = "event_type")
    private String eventType = "";

    @ColumnInfo(name = "event_time_millis")
    private long eventTimeMillis;

    private long dateMillis;
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

    @Nullable
    @ColumnInfo(name = "line_items_json")
    private String lineItemsJson;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    @NonNull
    public String getEventType() { return eventType; }
    public void setEventType(@NonNull String eventType) { this.eventType = eventType; }
    public long getEventTimeMillis() { return eventTimeMillis; }
    public void setEventTimeMillis(long eventTimeMillis) { this.eventTimeMillis = eventTimeMillis; }
    public long getDateMillis() { return dateMillis; }
    public void setDateMillis(long dateMillis) { this.dateMillis = dateMillis; }
    public boolean isVacuum() { return vacuum; }
    public void setVacuum(boolean vacuum) { this.vacuum = vacuum; }
    public boolean isCleanSkimmer() { return cleanSkimmer; }
    public void setCleanSkimmer(boolean cleanSkimmer) { this.cleanSkimmer = cleanSkimmer; }
    public boolean isAddWater() { return addWater; }
    public void setAddWater(boolean addWater) { this.addWater = addWater; }
    public boolean isBrushWalls() { return brushWalls; }
    public void setBrushWalls(boolean brushWalls) { this.brushWalls = brushWalls; }
    public float getChlorine() { return chlorine; }
    public void setChlorine(float chlorine) { this.chlorine = chlorine; }
    public float getPhUp() { return phUp; }
    public void setPhUp(float phUp) { this.phUp = phUp; }
    public float getPhDown() { return phDown; }
    public void setPhDown(float phDown) { this.phDown = phDown; }
    public float getNoPhos() { return noPhos; }
    public void setNoPhos(float noPhos) { this.noPhos = noPhos; }
    @Nullable
    public String getLineItemsJson() { return lineItemsJson; }
    public void setLineItemsJson(@Nullable String lineItemsJson) { this.lineItemsJson = lineItemsJson; }
}
