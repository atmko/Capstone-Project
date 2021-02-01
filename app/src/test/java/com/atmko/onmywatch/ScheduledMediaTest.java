/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import com.atmko.onmywatch.models.ScheduledMedia;
import com.atmko.onmywatch.models.TimeMetricsGenerator;
import com.atmko.onmywatch.utils.GeneralUtils;
import com.atmko.onmywatch.utils.GeneralUtils.DateInject;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;

import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.atmko.onmywatch.models.ScheduledMedia.NO_DATES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ScheduledMediaTest {
    private static final long CURRENT_TIMESTAMP_STUB = 1575566616000L;
    private static Map<String, String> METRICS_MAP() {
        return new HashMap<String, String>() {{
            put(TimeMetricsGenerator.YEAR_KEY, "%d year");
            put(TimeMetricsGenerator.MONTH_KEY, "%d month");
            put(TimeMetricsGenerator.WEEK_KEY, "%d week");
            put(TimeMetricsGenerator.DAY_KEY, "%d day");
            put(TimeMetricsGenerator.HOUR_KEY, "%d hour");
            put(TimeMetricsGenerator.MINUTE_KEY, "%d minute");
            put(TimeMetricsGenerator.SECOND_KEY, "%d second");
            put(TimeMetricsGenerator.YEARS_KEY, "%d years");
            put(TimeMetricsGenerator.MONTHS_KEY, "%d months");
            put(TimeMetricsGenerator.WEEKS_KEY, "%d weeks");
            put(TimeMetricsGenerator.DAYS_KEY, "%d days");
            put(TimeMetricsGenerator.HOURS_KEY, "%d hours");
            put(TimeMetricsGenerator.MINUTES_KEY, "%d minutes");
            put(TimeMetricsGenerator.SECONDS_KEY, "%d seconds");
        }};
    }

    @Before
    public void setDateInject()  {
        Calendar testCalender = Calendar.getInstance();
        testCalender.setTimeInMillis(CURRENT_TIMESTAMP_STUB);

        DateInject.custom = testCalender.getTime();
    }

    @Test
    public void getCountdownTest() {
        ScheduledMedia releaseSchedule = new ScheduledMedia();
        try {
            TimeMetricsGenerator metrics = new TimeMetricsGenerator(METRICS_MAP());

            releaseSchedule.setAirDate("2019-12-09T05:00:00.000Z");
            String daysAnswer = 3 + " days";
            assertEquals(daysAnswer, releaseSchedule.getCountdown(metrics));

            releaseSchedule.setAirDate("2019-12-05T21:00:00.000Z");
            String hoursAnswer = 3 + " hours";
            assertEquals(hoursAnswer, releaseSchedule.getCountdown(metrics));

            releaseSchedule.setAirDate("2019-12-05T18:00:00.000Z");
            String minutesAnswer = 36 + " minutes";
            assertEquals(minutesAnswer, releaseSchedule.getCountdown(metrics));

        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void pastWithoutNextEpisodeTest() {
        ScheduledMedia releaseSchedule = new ScheduledMedia();
        try {
            releaseSchedule.setAirDate("2019-12-05T16:00:00.000Z");
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
            fail();
        }

        TimeMetricsGenerator metrics = new TimeMetricsGenerator(METRICS_MAP());
        assertEquals(NO_DATES, releaseSchedule.getCountdown(metrics));
    }

    @Test
    public void getBestAvailableDateStringTest() {
        ScheduledMedia releaseSchedule = new ScheduledMedia();
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
        ScheduledMedia releaseSchedule = new ScheduledMedia();
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
        ScheduledMedia releaseSchedule = new ScheduledMedia();
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

//    @Test
    @SuppressWarnings("unused")
    public void setAirDateIncorrectFormatTest() {
        ScheduledMedia releaseSchedule = new ScheduledMedia();
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