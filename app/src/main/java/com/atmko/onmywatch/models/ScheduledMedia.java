/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import android.annotation.SuppressLint;

import com.atmko.onmywatch.utils.GeneralUtils;
import com.atmko.onmywatch.utils.GeneralUtils.DateInject;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;

import org.parceler.Parcel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static com.atmko.onmywatch.utils.GeneralUtils.ISO_DATE_FORMAT;

/*
 * episode model class
 */

@Parcel
public class ScheduledMedia {
    private static final String TIME_SUFFIX_YEARS = " year(s)";
    private static final String TIME_SUFFIX_MONTHS = " month(s)";
    private static final String TIME_SUFFIX_WEEKS = " week(s)";
    public static final String TIME_SUFFIX_DAYS = " day(s)";
    public static final String TIME_SUFFIX_HOURS = " hour(s)";
    public static final String TIME_SUFFIX_MINUTES = " minute(s)";
    private static final String TIME_SUFFIX_SECONDS = " second(s)";
    public static final String NO_DATES = "No Dates";
    private static final String DATE_ERROR = "Date Error";

    private static final int YEARS_CONVERSION = 365;
    private static final int MONTHS_CONVERSION = 30;
    private static final int WEEKS_CONVERSION = 7;

    String mAirDate;
    String mAirDateIso;

    public ScheduledMedia() {
    }

    //returns the local date of episode. Uses iso date if available, otherwise regular date, otherwise null
    public Date getBestLocalAirDate() {
        if (mAirDateIso != null) {
            try {
                //get media'd release date in media's timezone
                Date releaseDate = convertAirDateIso(mAirDateIso);

                //set timestamp in local calender
                Calendar localCalender = Calendar.getInstance();
                localCalender.setTimeInMillis(releaseDate.getTime());

                //return local calender's new date
                return localCalender.getTime();

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        if (mAirDate != null) {
            try {
                return convertAirDate(mAirDate);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }

        } else {
            return null;
        }
    }

    //returns the most accurate available date of episode. Uses iso date if available, otherwise regular date, otherwise null
    public String getBestAvailableDateString() {
        if (mAirDateIso != null) {
            return mAirDateIso;
        }

        if (mAirDate != null) {
            return mAirDate;

        } else {
            return null;
        }
    }

    public void setAirDate(String airDate) throws DateFormatException, IllegalArgumentException {
        if (airDate == null || airDate.equals("")) return;

        if (airDate.length() > ApiConstants.DATE_FORMAT.length()) {
            try {
                if (airDate.contains(GeneralUtils.OFFSET_SYMBOL)) {
                    airDate = GeneralUtils.replaceOffsetSymbol(airDate);
                }

                convertAirDateIso(airDate);

            } catch (ParseException e) {
                e.printStackTrace();
                throw new DateFormatException();
            }

            mAirDateIso = airDate;

        } else {
            try {
                convertAirDate(airDate);

            } catch (ParseException e) {
                e.printStackTrace();
                throw new DateFormatException();
            }

            mAirDate = airDate;
        }
    }

    private Date convertAirDate(String dateString) throws ParseException {
        //create date format for parsing date strings
        //TODO: local format not used. Using API date format
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ApiConstants.DATE_FORMAT);
        return simpleDateFormat.parse(dateString);
    }

    private Date convertAirDateIso(String dateString) throws ParseException {
        //create date format for parsing iso strings
        //TODO: local format not used. Using API date format
        @SuppressLint("SimpleDateFormat")
        DateFormat isoDateFormat = new SimpleDateFormat(ISO_DATE_FORMAT);

        return isoDateFormat.parse(dateString);
    }

    //gets the time till next air date in days, hours, or minutes
    public String getCountdown() {
        if (mAirDate == null && mAirDateIso == null) return null;

        long timeDifference = getBestTimeDifference();

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
            return NO_DATES;
        }
    }

    //returns time in millis till nex air date. Uses air date if air date iso not available else returns Long.MAX_VALUE
    long getBestTimeDifference() {
        if (mAirDateIso != null) {
            try {
                return getTimeDifferenceViaUtcTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

        } else if (mAirDate != null){
            try {
                return getTimeToAirDate();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    //returns time in millis till next episode
    private Long getTimeDifferenceViaUtcTime() throws ParseException {
        return convertAirDateIso(mAirDateIso).getTime() - DateInject.getInstance().currentDate().getTime();
    }

    //returns time in millis till air date (doesn't take timezone, hours or minutes into account so accuracy is limited)
    private long getTimeToAirDate() throws ParseException {
        return convertAirDate(mAirDate).getTime() - new Date().getTime();
    }

    public class DateFormatException extends Exception {
        static final String ERROR_MESSAGE = "Date format does not match: \"" + ISO_DATE_FORMAT + "\" or \"" + ApiConstants.DATE_FORMAT + "\"";
        DateFormatException() {
            super(ERROR_MESSAGE);
        }
    }
}