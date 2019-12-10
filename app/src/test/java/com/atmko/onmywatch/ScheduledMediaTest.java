/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import com.atmko.onmywatch.models.ScheduledMedia;
import com.atmko.onmywatch.utils.GeneralUtils;
import com.atmko.onmywatch.utils.GeneralUtils.DateInject;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.atmko.onmywatch.models.ScheduledMedia.DATE_TBD;
import static com.atmko.onmywatch.models.ScheduledMedia.TIME_SUFFIX_DAYS;
import static com.atmko.onmywatch.models.ScheduledMedia.TIME_SUFFIX_HOURS;
import static com.atmko.onmywatch.models.ScheduledMedia.TIME_SUFFIX_MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ScheduledMediaTest {
    private static final long CURRENT_TIMESTAMP_STUB = 1575566616000L;
    private DateInject dateInject;

    private DateInject getDateInject() {
        if (dateInject != null) return dateInject;

        Calendar testCalender = Calendar.getInstance();
        testCalender.setTimeInMillis(CURRENT_TIMESTAMP_STUB);

        dateInject = new DateInject();
        dateInject.custom = testCalender.getTime();
        return dateInject;
    }

    @Test
    public void getCountdownTest() {
        ScheduledMedia releaseSchedule = new ScheduledMedia(getDateInject());
        try {
            releaseSchedule.setAirDate("2019-12-09T05:00:00.000Z");
            String daysAnswer = 3 + TIME_SUFFIX_DAYS;
            assertEquals(daysAnswer, releaseSchedule.getCountdown());

            releaseSchedule.setAirDate("2019-12-05T21:00:00.000Z");
            String hoursAnswer = 3 + TIME_SUFFIX_HOURS;
            assertEquals(hoursAnswer, releaseSchedule.getCountdown());

            releaseSchedule.setAirDate("2019-12-05T18:00:00.000Z");
            String minutesAnswer = 36 + TIME_SUFFIX_MINUTES;
            assertEquals(minutesAnswer, releaseSchedule.getCountdown());

        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void pastWithoutNextEpisodeTest() {
        ScheduledMedia releaseSchedule = new ScheduledMedia(getDateInject());
        try {
            releaseSchedule.setAirDate("2019-12-05T16:00:00.000Z");
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
            fail();
        }

        assertEquals(DATE_TBD, releaseSchedule.getCountdown());
    }

    @Test
    public void getBestAvailableDateStringTest() {
        ScheduledMedia releaseSchedule = new ScheduledMedia(getDateInject());
        try {
            releaseSchedule.setAirDate("2019-12-05");
            String bestDateAvailable = "2019-12-05";
            assertEquals(bestDateAvailable, releaseSchedule.getBestAvailableDateString());

            releaseSchedule.setAirDate("2019-12-05T16:00:00.000+0000");
            bestDateAvailable = "2019-12-05T16:00:00.000+0000";
            assertEquals(bestDateAvailable, releaseSchedule.getBestAvailableDateString());

            releaseSchedule.setAirDate("2019-12-05");
            bestDateAvailable = "2019-12-05T16:00:00.000+0000";
            assertEquals(bestDateAvailable, releaseSchedule.getBestAvailableDateString());
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void getBestLocalAirDateTest() {
        ScheduledMedia releaseSchedule = new ScheduledMedia(getDateInject());
        try {
            releaseSchedule.setAirDate("2019-12-05");
            Date bestDateAvailable = new SimpleDateFormat(ApiConstants.DATE_FORMAT).parse("2019-12-05");
            assertEquals(bestDateAvailable, releaseSchedule.getBestLocalAirDate());

            releaseSchedule.setAirDate("2019-12-05T16:00:00.000+0000");
            bestDateAvailable = new SimpleDateFormat(GeneralUtils.ISO_DATE_FORMAT).parse("2019-12-05T16:00:00.000+0000");
            assertEquals(bestDateAvailable, releaseSchedule.getBestLocalAirDate());

            releaseSchedule.setAirDate("2019-12-05");
            bestDateAvailable = new SimpleDateFormat(GeneralUtils.ISO_DATE_FORMAT).parse("2019-12-05T16:00:00.000+0000");
            assertEquals(bestDateAvailable, releaseSchedule.getBestLocalAirDate());
        } catch (ScheduledMedia.DateFormatException e) {
            fail();
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void setAirDateCorrectFormatTest() {
        ScheduledMedia releaseSchedule = new ScheduledMedia(getDateInject());
        try {
            releaseSchedule.setAirDate("2019-12-05");
            assertEquals("2019-12-05", releaseSchedule.getBestAvailableDateString());
            releaseSchedule.setAirDate("2019-12-05T16:00:00.000Z");
            assertEquals("2019-12-05T16:00:00.000+0000", releaseSchedule.getBestAvailableDateString());
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void setAirDateIncorrectFormatTest() {
        ScheduledMedia releaseSchedule = new ScheduledMedia(getDateInject());
        try {
            releaseSchedule.setAirDate("");
            fail();
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        try {
            releaseSchedule.setAirDate("2019p");
            fail();
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        try {
            releaseSchedule.setAirDate("2019-12-05T16:00:00.000B");
            fail();
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }
    }
}