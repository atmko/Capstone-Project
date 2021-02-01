package com.atmko.onmywatch.models;

import android.content.Context;

import com.atmko.onmywatch.R;

import java.util.HashMap;
import java.util.Map;

public class TimeMetricsGenerator {

    public static final String YEAR_KEY = "year";
    public static final String MONTH_KEY = "month";
    public static final String WEEK_KEY = "week";
    public static final String DAY_KEY = "day";
    public static final String HOUR_KEY = "hour";
    public static final String MINUTE_KEY = "minute";
    public static final String SECOND_KEY = "second";
    public static final String YEARS_KEY = "years";
    public static final String MONTHS_KEY = "months";
    public static final String WEEKS_KEY = "weeks";
    public static final String DAYS_KEY = "days";
    public static final String HOURS_KEY = "hours";
    public static final String MINUTES_KEY = "minutes";
    public static final String SECONDS_KEY = "seconds";

    private Map<String, String> mTimeMetrics;

    public TimeMetricsGenerator(Map<String, String> timeMetrics) {
        mTimeMetrics = timeMetrics;
    }

    public TimeMetricsGenerator(final Context context) {
        mTimeMetrics =  new HashMap<String, String>() {{
            put("year", context.getString(R.string.countdown_year));
            put("month", context.getString(R.string.countdown_month));
            put("week", context.getString(R.string.countdown_week));
            put("day", context.getString(R.string.countdown_day));
            put("hour", context.getString(R.string.countdown_hour));
            put("minute", context.getString(R.string.countdown_minute));
            put("second", context.getString(R.string.countdown_second));
            put("years", context.getString(R.string.countdown_years));
            put("months", context.getString(R.string.countdown_months));
            put("weeks", context.getString(R.string.countdown_weeks));
            put("days ", context.getString(R.string.countdown_days));
            put("hours", context.getString(R.string.countdown_hours));
            put("minutes", context.getString(R.string.countdown_minutes));
            put("seconds", context.getString(R.string.countdown_seconds));
        }};
    }

    public String getYearMetric() {
        return mTimeMetrics.get(YEAR_KEY);
    }

    public String getMonthMetric() {
        return mTimeMetrics.get(MONTH_KEY);
    }

    public String getWeekMetric() {
        return mTimeMetrics.get(WEEK_KEY);
    }

    public String getDayMetric() {
        return mTimeMetrics.get(DAY_KEY);
    }

    public String getHourMetric() {
        return mTimeMetrics.get(HOUR_KEY);
    }

    public String getMinuteMetric() {
        return mTimeMetrics.get(MINUTE_KEY);
    }

    public String getSecondMetric() {
        return mTimeMetrics.get(SECOND_KEY);
    }

    public String getYearsMetric() {
        return mTimeMetrics.get(YEARS_KEY);
    }

    public String getMonthsMetric() {
        return mTimeMetrics.get(MONTHS_KEY);
    }

    public String getWeeksMetric() {
        return mTimeMetrics.get(WEEKS_KEY);
    }

    public String getDaysMetric() {
        return mTimeMetrics.get(DAYS_KEY);
    }

    public String getHoursMetric() {
        return mTimeMetrics.get(HOURS_KEY);
    }

    public String getMinutesMetric() {
        return mTimeMetrics.get(MINUTES_KEY);
    }

    public String getSecondsMetric() {
        return mTimeMetrics.get(SECONDS_KEY);
    }
}
