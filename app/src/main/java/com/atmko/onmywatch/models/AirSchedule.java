/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import android.annotation.SuppressLint;

import com.atmko.onmywatch.utils.network_utils.ApiConstants;

import org.parceler.Parcel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static com.atmko.onmywatch.utils.GeneralUtils.ISO_DATE_FORMAT;
import static com.atmko.onmywatch.utils.GeneralUtils.parseIsoDate;

/*
 * class that calculates amount of time till next airing
 */

@Parcel
public class AirSchedule {
    private static final int NUMBER_OF_WEEKDAYS = 7;

    private static final String SUNDAY = "Sunday";
    private static final String MONDAY = "Monday";
    private static final String TUESDAY = "Tuesday";
    private static final String WEDNESDAY = "Wednesday";
    private static final String THURSDAY = "Thursday";
    private static final String FRIDAY = "Friday";
    private static final String SATURDAY = "Saturday";

    private static final String TIME_SUFFIX_DAYS = " day(s)";
    private static final String TIME_SUFFIX_HOURS = " hour(s)";
    private static final String TIME_SUFFIX_MINUTES = " minute(s)";

    public String day;
    public String time;
    public String timezoneId;

    public AirSchedule() {
    }

    public AirSchedule(String day, String time, String timezoneId) {
        this.day = day;
        this.time = time;
        this.timezoneId = timezoneId;
    }

    //gets the time till next air date in days, hours, or minutes
    public String getCountdown(String airDate) {
        //if days till next episode is greater than the number weekdays, express countdown in
        TimeZone mediaTimeZone = TimeZone.getTimeZone(timezoneId);
        Calendar mediaTimeZoneCalender = Calendar.getInstance(mediaTimeZone);

        int daysToNextEpisode = getDaysToNextEpisode(airDate, mediaTimeZoneCalender.getTime().getTime());
        long timeDifference = getTimeTillWeeklyAirtime(mediaTimeZoneCalender);
        int daysTillWeeklyAirtime = Long.valueOf(TimeUnit.MILLISECONDS.toDays(timeDifference)).intValue();

        if (daysToNextEpisode > daysTillWeeklyAirtime) {
            return daysToNextEpisode + TIME_SUFFIX_DAYS;

        } else {
            if (daysTillWeeklyAirtime < 1) {
                int hoursValue = Long.valueOf(TimeUnit.MILLISECONDS.toHours(timeDifference)).intValue();

                if (hoursValue < 1) {
                    int minutesValue = Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(timeDifference)).intValue();
                    return minutesValue + TIME_SUFFIX_MINUTES;

                } else {
                    return hoursValue + TIME_SUFFIX_HOURS;
                }

            } else {
                return daysTillWeeklyAirtime + " day(s)";
            }
        }
    }

    //returns a value associating "day" property to corresponding Calender value
    private int getWeekdayValue(String dayString) {
        switch (dayString) {
            case SUNDAY :
                return Calendar.SUNDAY;

            case MONDAY :
                return Calendar.MONDAY;

            case TUESDAY :
                return Calendar.TUESDAY;

            case WEDNESDAY :
                return Calendar.WEDNESDAY;

            case THURSDAY :
                return Calendar.THURSDAY;

            case FRIDAY :
                return Calendar.FRIDAY;

            case SATURDAY :
                return Calendar.SATURDAY;

            default:
                return 0;
        }
    }

    //get timestamp value to next episode by advancing calender to next week's release schedule
    private long getTimeTillWeeklyAirtime(Calendar mediaTimeZoneCalender) {
        //get days difference between current week day and release week day
        int currentDayOfWeekValue = mediaTimeZoneCalender.get(Calendar.DAY_OF_WEEK);
        int releaseDayOfWeekValue = getWeekdayValue(day);
        int daysDifference = getTimeDifferenceInDays(currentDayOfWeekValue, releaseDayOfWeekValue);

        //create release calender and advance calender by days difference
        Calendar releaseTimeZoneCalender = Calendar.getInstance(mediaTimeZoneCalender.getTimeZone());
        releaseTimeZoneCalender.add(Calendar.DAY_OF_MONTH, daysDifference);

        //create date format for parsing iso strings
        //TODO: local format not used. Using API date format
        @SuppressLint("SimpleDateFormat")
        DateFormat isoDateFormat = new SimpleDateFormat(ISO_DATE_FORMAT);

        //get advanced release calender's year, month and date
        String releaseYear = String.valueOf(releaseTimeZoneCalender.get(Calendar.YEAR));
        //add 1 to adjust for Calender's month zero index
        String releaseMonth = String.valueOf(releaseTimeZoneCalender.get(Calendar.MONTH) + 1);
        //correct to 2 figures
        if (releaseMonth.length() == 1) releaseMonth = "0" + releaseMonth;

        String releaseDayOfMonth = String.valueOf(releaseTimeZoneCalender.get(Calendar.DAY_OF_MONTH));
        //correct to 2 figures
        if (releaseDayOfMonth.length() == 1) releaseDayOfMonth = "0" + releaseDayOfMonth;

        long timezoneOffset = releaseTimeZoneCalender.get(Calendar.ZONE_OFFSET);

        //create iso date using year, month, day and episode air time
        String releaseIsoFormat =
                parseIsoDate(releaseYear, releaseMonth, releaseDayOfMonth, time, timezoneOffset);

        //return the difference between release timestamp and current timestamp
        try {
            Date releaseDate = isoDateFormat.parse(releaseIsoFormat);
            long currentTimestamp = mediaTimeZoneCalender.getTime().getTime();
            long releaseTimeStamp = releaseDate.getTime();

            return releaseTimeStamp - currentTimestamp;
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    //TODO: improve accuracy
    //returns the number of days till next episode (doesn't take weekly airtime into account so accuracy is limited)
    private int getDaysToNextEpisode(String airDateString, long currentTimestamp) {
        //TODO: local format not used. Using API date format
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ApiConstants.DATE_FORMAT);
        try {
            Date airDate = simpleDateFormat.parse(airDateString);
            long timeDifference = airDate.getTime() - currentTimestamp;

            return Long.valueOf(TimeUnit.MILLISECONDS.toDays(timeDifference)).intValue();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    //returns the number of days between two given days of the week
    private int getTimeDifferenceInDays(int currentDayValue, int releaseDayValue) {
        if (currentDayValue <= releaseDayValue) {
            return releaseDayValue - currentDayValue;

        } else {
            //normalize count
            int normalizer = NUMBER_OF_WEEKDAYS - currentDayValue;
            return normalizer + releaseDayValue;
        }
    }
}