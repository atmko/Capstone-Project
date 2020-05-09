/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import android.annotation.SuppressLint;
import android.content.Context;

import com.atmko.onmywatch.R;
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
 * scheduled media model class
 */

@Parcel
public class ScheduledMedia {
    public static final int SOURCE_TMDB = 0;
    public static final int SOURCE_TRAKT = 1;

    public static final String NO_DATES = "No Dates";

    private static final int YEARS_CONVERSION = 365;
    private static final int MONTHS_CONVERSION = 30;
    private static final int WEEKS_CONVERSION = 7;

    String mAirDate;
    String mAirDateIso;

    public ScheduledMedia() {
    }

    public boolean hasNonEmptyDate() {
        return getBestAvailableDateString() != null && !getBestAvailableDateString().equals("");
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

    public long getTimestamp() {
        if (getBestLocalAirDate() != null) {
            return getBestLocalAirDate().getTime();

        } else {
            return Long.MAX_VALUE;
        }
    }

    public boolean isInFuture() {
        return DateInject.getInstance().currentDate().getTime() < getTimestamp();
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
    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    public String getCountdown(Context context) {
        if (mAirDate == null && mAirDateIso == null) return NO_DATES;

        long timeDifference = getBestTimeDifference();

        boolean inFuture = timeDifference >= 0;

        if (!inFuture) return NO_DATES;
        int daysValue = Math.abs(Long.valueOf(TimeUnit.MILLISECONDS.toDays(timeDifference)).intValue());
        int yearsValue =  Math.abs(((Double) Math.floor(daysValue / YEARS_CONVERSION)).intValue());
        int monthsValue =  Math.abs(((Double) Math.floor(daysValue / MONTHS_CONVERSION)).intValue());
        int weeksValue =  Math.abs(((Double) Math.floor(daysValue / WEEKS_CONVERSION)).intValue());
        int hoursValue =  Math.abs(Long.valueOf(TimeUnit.MILLISECONDS.toHours(timeDifference)).intValue());
        int minutesValue =  Math.abs(Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(timeDifference)).intValue());
        int secondsValue =  Math.abs(Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(timeDifference)).intValue());

        String countdownFormat;
        if (yearsValue >= 1) {
            if (yearsValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_years);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_year);
            }

            return String.format(countdownFormat, yearsValue);
        }

        if (monthsValue >= 1) {
            if (monthsValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_months);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_month);
            }

            return String.format(countdownFormat, monthsValue);
        }
        if (weeksValue >= 1) {
            if (weeksValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_weeks);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_week);
            }

            return String.format(countdownFormat, weeksValue);
        }

        if (daysValue >= 1) {
            if (daysValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_days);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_day);
            }

            return String.format(countdownFormat, daysValue);
        }

        if (hoursValue >= 1) {
            if (hoursValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_hours);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_hour);
            }

            return String.format(countdownFormat, hoursValue);
        }

        if (minutesValue >= 1) {
            if (minutesValue > 1) {
                //plural and in future
                countdownFormat = context.getString(R.string.countdown_minutes);
            } else {
                //singular and in future
                countdownFormat = context.getString(R.string.countdown_minute);
            }

            return String.format(countdownFormat, minutesValue);
        }

        if (secondsValue > 1) {
            //plural and in future
            countdownFormat = context.getString(R.string.countdown_seconds);
        } else {
            //singular and in future
            countdownFormat = context.getString(R.string.countdown_second);
        }

        return String.format(countdownFormat, secondsValue);
    }

    //returns time in millis till nex air date. Uses air date if air date iso not available else returns Long.MAX_VALUE
    private long getBestTimeDifference() {
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

    public static class DateFormatException extends Exception {
        static final String ERROR_MESSAGE = "Date format does not match: \"" + ISO_DATE_FORMAT + "\" or \"" + ApiConstants.DATE_FORMAT + "\"";
        DateFormatException() {
            super(ERROR_MESSAGE);
        }
    }
}