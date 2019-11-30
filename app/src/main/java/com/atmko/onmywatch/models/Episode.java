/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import android.annotation.SuppressLint;
import android.util.Log;

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
public class Episode {
    private static final String TIME_SUFFIX_DAYS = " day(s)";
    private static final String TIME_SUFFIX_HOURS = " hour(s)";
    private static final String TIME_SUFFIX_MINUTES = " minute(s)";

    private Date mAirDate;
    private Date mAirDateIso;

    public Episode() {
    }

    //returns the local date of episode. Uses iso date if available, otherwise regular date, otherwise null
    public Date getLocalAirDate() {
        if (mAirDateIso != null) {
            //add time till next episode to current calender
            Calendar releaseCalender = Calendar.getInstance();
            releaseCalender.add(Calendar.MILLISECOND, getTimeDifferenceViaUtcTime().intValue());
            return releaseCalender.getTime();
        }

        if (mAirDate != null) {
            return mAirDate;

        } else {
            return null;
        }
    }

    //returns the most accurate available date of episode. Uses iso date if available, otherwise regular date, otherwise null
    public Date getBestAvailableDate() {
        if (mAirDateIso != null) {
            return mAirDateIso;
        }

        if (mAirDate != null) {
            return mAirDate;

        } else {
            return null;
        }
    }

    public void setAirDate(Date mAirDate) {
        this.mAirDate = mAirDate;
    }

    public void setAirDate(String airDate) throws ParseException {
        //create date format for parsing date strings
        //TODO: local format not used. Using API date format
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ApiConstants.DATE_FORMAT);

        this.mAirDate = simpleDateFormat.parse(airDate);
    }

    public void setAirDateIso(String airDateIso) throws ParseException {
        //create date format for parsing iso strings
        //TODO: local format not used. Using API date format
        @SuppressLint("SimpleDateFormat")
        DateFormat isoDateFormat = new SimpleDateFormat(ISO_DATE_FORMAT);

        this.mAirDateIso = isoDateFormat.parse(airDateIso);
    }

    //gets the time till next air date in days, hours, or minutes
    public String getCountdown() {
        if (mAirDate == null && mAirDateIso == null) return null;

        long timeDifference;

        if (mAirDateIso != null) {
            timeDifference = getTimeDifferenceViaUtcTime();

        } else {
            timeDifference = getTimeToNextEpisode();
        }

        int daysValue = Long.valueOf(TimeUnit.MILLISECONDS.toDays(timeDifference)).intValue();

        if (daysValue < 1) {
            int hoursValue = Long.valueOf(TimeUnit.MILLISECONDS.toHours(timeDifference)).intValue();

            if (hoursValue < 1) {
                int minutesValue = Long.valueOf(TimeUnit.MILLISECONDS.toMinutes(timeDifference)).intValue();
                return minutesValue + TIME_SUFFIX_MINUTES;

            } else {
                return hoursValue + TIME_SUFFIX_HOURS;
            }

        } else {
            return daysValue + TIME_SUFFIX_DAYS;
        }
    }

    //returns time in millis till next episode
    private Long getTimeDifferenceViaUtcTime() {
        return mAirDateIso.getTime() - new Date().getTime();
    }

    //returns time in millis till next episode (doesn't take timezone, hours or minutes into account so accuracy is limited)
    private long getTimeToNextEpisode() {
        return mAirDate.getTime() - new Date().getTime();
    }
}