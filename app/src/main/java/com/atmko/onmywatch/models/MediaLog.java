package com.atmko.onmywatch.models;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.atmko.onmywatch.utils.LogUpdateReceiver;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Entity(tableName = "media_logs", primaryKeys = {"parent_id", "condition"})
public abstract class MediaLog {
    private static final int UPDATE_LOG_PREFIX = 9;

    public static final int CONDITION_UPCOMING = 1;
    public static final int CONDITION_AIRED = 2;
    public static final int CONDITION_UNDATED = 3;

    public static final String CONDITION_KEY = "condition";
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String TITLE_KEY = "title";
    public static final String POSTER_PATH_KEY = "poster_path";
    public static final String BACKDROP_PATH_KEY = "backdrop_path";
    public static final String PARENT_ID_KEY = "parent_id";

    private static final String TIME_SUFFIX_YEARS = " year(s)";
    private static final String TIME_SUFFIX_MONTHS = " month(s)";
    private static final String TIME_SUFFIX_WEEKS = " week(s)";
    private static final String TIME_SUFFIX_DAYS = " day(s)";
    private static final String TIME_SUFFIX_HOURS = " hour(s)";
    private static final String TIME_SUFFIX_MINUTES = " minute(s)";
    private static final String TIME_SUFFIX_SECONDS = " second(s)";

    private static final int YEARS_CONVERSION = 365;
    private static final int MONTHS_CONVERSION = 30;
    private static final int WEEKS_CONVERSION = 7;

    private static final String PAST_SUFFIX = " ago";

    @NonNull public String type = "";
    public int condition;
    public long timestamp;
    public String title;
    public String posterPath;
    public String backdropPath;
    @NonNull @ColumnInfo(name = "parent_id") public String parentId = "";

    @Ignore
    String mUniqueExternalId;

    //gets the time till next air date in days, hours, or minutes
    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    public String getCountdown() {
        long timeDifference = timestamp - new Date().getTime();

        boolean inFuture = timeDifference >= 0;
        int daysValue = Long.valueOf(TimeUnit.MILLISECONDS.toDays(timeDifference)).intValue();
        int yearsValue = ((Double) Math.floor(daysValue / YEARS_CONVERSION)).intValue();
        int monthsValue = ((Double) Math.floor(daysValue / MONTHS_CONVERSION)).intValue();
        int weeksValue = ((Double) Math.floor(daysValue / WEEKS_CONVERSION)).intValue();
        int hoursValue = Long.valueOf(TimeUnit.MILLISECONDS.toHours(timeDifference)).intValue();
        int minutesValue = Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(timeDifference)).intValue();
        int secondsValue = Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(timeDifference)).intValue();

        if (inFuture) {
            if (yearsValue >= 1) return yearsValue + TIME_SUFFIX_YEARS;
            if (monthsValue >= 1) return monthsValue + TIME_SUFFIX_MONTHS;
            if (weeksValue >= 1) return weeksValue + TIME_SUFFIX_WEEKS;
            if (daysValue >= 1) return daysValue + TIME_SUFFIX_DAYS;
            if (hoursValue >= 1) return hoursValue + TIME_SUFFIX_HOURS;
            if (minutesValue >= 1) return minutesValue + TIME_SUFFIX_MINUTES;
            else return secondsValue + TIME_SUFFIX_SECONDS;

        } else {
            if (yearsValue <= -1) return Math.abs(yearsValue) + TIME_SUFFIX_YEARS + PAST_SUFFIX;
            if (monthsValue <= -1) return Math.abs(monthsValue) + TIME_SUFFIX_MONTHS + PAST_SUFFIX;
            if (weeksValue <= -1) return Math.abs(weeksValue) + TIME_SUFFIX_WEEKS + PAST_SUFFIX;
            if (daysValue <= -1) return Math.abs(daysValue) + TIME_SUFFIX_DAYS + PAST_SUFFIX;
            if (hoursValue <= -1) return Math.abs(hoursValue) + TIME_SUFFIX_HOURS + PAST_SUFFIX;
            if (minutesValue <= -1) return Math.abs(minutesValue) + TIME_SUFFIX_MINUTES + PAST_SUFFIX;
            else return Math.abs(secondsValue) + TIME_SUFFIX_SECONDS + PAST_SUFFIX;
        }
    }

    //creates pending intent alarm log update
    public static PendingIntent createUpdatePendingIntent(Context context, int mediaType,
                                             String mediaId, int notifierCondition) {
        Intent intent = new Intent(context, LogUpdateReceiver.class);
        intent.putExtra(MediaData.MEDIA_TYPE_KEY, mediaType);
        intent.putExtra(ApiConstants.ID_KEY, mediaId);
        intent.putExtra(CONDITION_KEY, notifierCondition);
        return PendingIntent.getBroadcast(context, getUpdateCode(mediaId),
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //creates pending intent without notification (for canceling alarm)
    public PendingIntent createPendingIntent(Context context, String mediaId) {
        Intent intent = new Intent(context, LogUpdateReceiver.class);
        return PendingIntent.getBroadcast(context, getUpdateCode(mediaId), intent, 0);
    }

    private static int getUpdateCode(String mediaId) {
        return Integer.parseInt(UPDATE_LOG_PREFIX + mediaId);
    }

    public Map<String, Object> parseLogToDataMap() {
        Map<String, Object> seriesLogMap = new HashMap<>();
        seriesLogMap.put(CONDITION_KEY, condition);
        seriesLogMap.put(TIMESTAMP_KEY, timestamp);
        seriesLogMap.put(TITLE_KEY, title);
        seriesLogMap.put(POSTER_PATH_KEY, posterPath);
        seriesLogMap.put(BACKDROP_PATH_KEY, backdropPath);
        seriesLogMap.put(PARENT_ID_KEY, parentId);

        return seriesLogMap;
    }
}
