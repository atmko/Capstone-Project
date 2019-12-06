/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import com.atmko.onmywatch.utils.GeneralUtils;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;

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
    static final String TIME_SUFFIX_DAYS = " day(s)";
    static final String TIME_SUFFIX_HOURS = " hour(s)";
    static final String TIME_SUFFIX_MINUTES = " minute(s)";
    static final String DATE_TBD = "Date TBD";

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

    public void setAirDate(@NonNull String airDate) throws DateFormatException {
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

        int daysValue = Long.valueOf(TimeUnit.MILLISECONDS.toDays(timeDifference)).intValue();

        if (daysValue < 1) {
            int hoursValue = Long.valueOf(TimeUnit.MILLISECONDS.toHours(timeDifference)).intValue();

            if (hoursValue < 1) {
                int minutesValue = Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(timeDifference)).intValue();

                if (minutesValue < 1) {
                    return DATE_TBD;

                } else {
                    return minutesValue + TIME_SUFFIX_MINUTES;
                }

            } else {
                return hoursValue + TIME_SUFFIX_HOURS;
            }

        } else {
            return daysValue + TIME_SUFFIX_DAYS;
        }
    }

    //returns time in millis till nex air date. Uses air date if air date iso not available else returns 0
    private long getBestTimeDifference() {
        long timeDifference = 0;

        if (mAirDateIso != null) {
            try {
                timeDifference = getTimeDifferenceViaUtcTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }

        } else {
            try {
                timeDifference = getTimeToAirDate();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return timeDifference;
    }

    //returns time in millis till next episode
    private Long getTimeDifferenceViaUtcTime() throws ParseException {
        return convertAirDateIso(mAirDateIso).getTime() - new Date().getTime();
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