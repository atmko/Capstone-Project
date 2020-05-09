package com.atmko.onmywatch.models;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

import com.atmko.onmywatch.R;
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
    public String getCountdown(Context context) {
        long timeDifference = timestamp - new Date().getTime();

        boolean inFuture = timeDifference >= 0;
        int daysValue = Math.abs(Long.valueOf(TimeUnit.MILLISECONDS.toDays(timeDifference)).intValue());
        int yearsValue =  Math.abs(((Double) Math.floor(daysValue / YEARS_CONVERSION)).intValue());
        int monthsValue =  Math.abs(((Double) Math.floor(daysValue / MONTHS_CONVERSION)).intValue());
        int weeksValue =  Math.abs(((Double) Math.floor(daysValue / WEEKS_CONVERSION)).intValue());
        int hoursValue =  Math.abs(Long.valueOf(TimeUnit.MILLISECONDS.toHours(timeDifference)).intValue());
        int minutesValue =  Math.abs(Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(timeDifference)).intValue());
        int secondsValue =  Math.abs(Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(timeDifference)).intValue());

        String countdownFormat;
        if (yearsValue >= 1) {
            if (yearsValue > 1 && !inFuture) {
                //plural and in past
                countdownFormat = context.getString(R.string.countdown_years_past);
            } else if (yearsValue == 1 && !inFuture) {
                //singular and in past
                countdownFormat = context.getString(R.string.countdown_year_past);
            } else if (yearsValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_years);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_year);
            }

            return String.format(countdownFormat, yearsValue);
        }

        if (monthsValue >= 1) {
            if (monthsValue > 1 && !inFuture) {
                //plural and in past
                countdownFormat = context.getString(R.string.countdown_months_past);
            } else if (monthsValue == 1 && !inFuture) {
                //singular and in past
                countdownFormat = context.getString(R.string.countdown_month_past);
            } else if (monthsValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_months);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_month);
            }

            return String.format(countdownFormat, monthsValue);
        }
        if (weeksValue >= 1) {
            if (weeksValue > 1 && !inFuture) {
                //plural and in past
                countdownFormat = context.getString(R.string.countdown_weeks_past);
            } else if (weeksValue == 1 && !inFuture) {
                //singular and in past
                countdownFormat = context.getString(R.string.countdown_week_past);
            } else if (weeksValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_weeks);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_week);
            }

            return String.format(countdownFormat, weeksValue);
        }

        if (daysValue >= 1) {
            if (daysValue > 1 && !inFuture) {
                //plural and in past
                countdownFormat = context.getString(R.string.countdown_days_past);
            } else if (daysValue == 1 && !inFuture) {
                //singular and in past
                countdownFormat = context.getString(R.string.countdown_day_past);
            } else if (daysValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_days);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_day);
            }

            return String.format(countdownFormat, daysValue);
        }

        if (hoursValue >= 1) {
            if (hoursValue > 1 && !inFuture) {
                //plural and in past
                countdownFormat = context.getString(R.string.countdown_hours_past);
            } else if (hoursValue == 1 && !inFuture) {
                //singular and in past
                countdownFormat = context.getString(R.string.countdown_hour_past);
            } else if (hoursValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_hours);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_hour);
            }

            return String.format(countdownFormat, hoursValue);
        }

        if (minutesValue >= 1) {
            if (minutesValue > 1 && !inFuture) {
                //plural and in past
                countdownFormat = context.getString(R.string.countdown_minutes_past);
            } else if (minutesValue == 1 && !inFuture) {
                //singular and in past
                countdownFormat = context.getString(R.string.countdown_minute_past);
            } else if (minutesValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_minutes);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_minute);
            }

            return String.format(countdownFormat, minutesValue);
        }

        if (secondsValue > 1 && !inFuture) {
            //plural and in past
            countdownFormat = context.getString(R.string.countdown_seconds_past);
        } else if (secondsValue == 1 && !inFuture) {
            //singular and in past
            countdownFormat = context.getString(R.string.countdown_second_past);
        } else if (secondsValue > 1) {
            //plural and in future
            countdownFormat = context.getString(R.string.countdown_seconds);
        } else {
            //singular and in future
            countdownFormat = context.getString(R.string.countdown_second);
        }

        return String.format(countdownFormat, secondsValue);
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
