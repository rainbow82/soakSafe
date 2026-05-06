package com.wgu.d424.soaksafe.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class TimeUtil {

    private TimeUtil() {
    }

    /**
     * Start of the local calendar day for the instant, in epoch millis (same zone as device).
     * Used as part of the database primary key for “completed on this day”.
     */
    public static long startOfLocalDayMillis(long instantMillis) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(instantMillis), zone);
        return date.atStartOfDay(zone).toInstant().toEpochMilli();
    }
}
