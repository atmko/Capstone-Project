/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import org.parceler.Parcel;

import java.util.Calendar;
import java.util.TimeZone;

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

    //get ISO value to next episode by advancing calender to next week's release schedule
    public String getAirDateIso() {
        //if days till next episode is greater than the number weekdays, express countdown in
        TimeZone mediaTimeZone = TimeZone.getTimeZone(timezoneId);
        Calendar mediaTimeZoneCalender = Calendar.getInstance(mediaTimeZone);

        //get days difference between current week day and release week day
        int currentDayOfWeekValue = mediaTimeZoneCalender.get(Calendar.DAY_OF_WEEK);
        int releaseDayOfWeekValue = getWeekdayValue(day);
        int daysDifference = getTimeDifferenceInDays(
                currentDayOfWeekValue, releaseDayOfWeekValue, mediaTimeZoneCalender);

        //create release calender and advance calender by days difference
        Calendar releaseTimeZoneCalender = Calendar.getInstance(mediaTimeZoneCalender.getTimeZone());
        releaseTimeZoneCalender.add(Calendar.DAY_OF_MONTH, daysDifference);

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
        return parseIsoDate(releaseYear, releaseMonth, releaseDayOfMonth, time, timezoneOffset);
    }

    //returns the number of days between two given days of the week
    private int getTimeDifferenceInDays(int currentDayValue, int releaseDayValue,
                                        Calendar mediaTimeZoneCalender) {
        if (currentDayValue < releaseDayValue) {
            return releaseDayValue - currentDayValue;

        } else if (currentDayValue == releaseDayValue) {
            String mediaTimezoneHour = String.valueOf(mediaTimeZoneCalender.get(Calendar.HOUR_OF_DAY));
            String mediaTimezoneMinute = String.valueOf(mediaTimeZoneCalender.get(Calendar.MINUTE));

            int mediaTimezoneSimpleTime =
                    getSimpleTimeInteger(mediaTimezoneHour + ":" + mediaTimezoneMinute);
            int releaseSimpleTime = getSimpleTimeInteger(time);

            if (mediaTimezoneSimpleTime < releaseSimpleTime) {
                return 0;

            } else {
                return NUMBER_OF_WEEKDAYS;
            }

        } else {
            //normalize count
            int normalizer = NUMBER_OF_WEEKDAYS - currentDayValue;
            return normalizer + releaseDayValue;
        }
    }

    private int getSimpleTimeInteger(String timeString) {
        return Integer.parseInt(timeString.replace(":",""));
    }
} 