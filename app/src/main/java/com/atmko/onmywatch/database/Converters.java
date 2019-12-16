/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database;

import androidx.room.TypeConverter;

import com.atmko.onmywatch.models.Episode;
import com.atmko.onmywatch.models.ScheduledMedia;

import java.util.ArrayList;
import java.util.Arrays;

public class Converters {
    private static final String ORIGIN_SEPARATOR = ",";

    @TypeConverter
    public static ArrayList<String> stringToOriginList(String formattedOriginString) {
        String[] originStringArray = formattedOriginString.split(ORIGIN_SEPARATOR);

        return new ArrayList<>(Arrays.asList(originStringArray));
    }

    @TypeConverter
    public static String countryOriginListToString(ArrayList<String> originCountryList) {
        String formattedOriginString = "";

        for (String originCountry: originCountryList) {
            formattedOriginString += originCountry + ORIGIN_SEPARATOR;
        }

        return formattedOriginString;
    }

    @TypeConverter
    public static Episode stringToEpisode(String airDateString) {
        Episode episode = new Episode();

        if (airDateString != null) {
            try {
                episode.setAirDate(airDateString);
            } catch (ScheduledMedia.DateFormatException e) {
                e.printStackTrace();
            }
        }

        return episode;
    }

    @TypeConverter
    public static String EpisodeToString(Episode episode) {
        return episode == null ? null : episode.getBestAvailableDateString();
    }
}
