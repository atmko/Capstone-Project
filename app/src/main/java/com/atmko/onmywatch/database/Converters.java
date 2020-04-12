/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database;

import androidx.room.TypeConverter;

import com.atmko.onmywatch.models.Episode;
import com.atmko.onmywatch.models.ScheduledMedia;
import com.atmko.onmywatch.models.SearchMediaTag;
import com.atmko.onmywatch.models.SearchTag;
import com.atmko.onmywatch.utils.GeneralUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Converters {
    private static final String ORIGIN_SEPARATOR = ",";
    private static final String NULL_STRING = "//NULL//";

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
    public static ScheduledMedia longToScheduledMedia(long airDateTimestamp) {
        ScheduledMedia scheduledMedia = new ScheduledMedia();
        Date date = new Date(airDateTimestamp);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        String airDateString = GeneralUtils.parseIsoDateFromCalender(calendar);

        try {
            scheduledMedia.setAirDate(airDateString);
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        return scheduledMedia;
    }

    @TypeConverter
    public static long scheduledMediaToLong(ScheduledMedia scheduledMedia) {
        if (scheduledMedia == null) return 0;
        return scheduledMedia.getBestLocalAirDate() == null ? 0
                : scheduledMedia.getBestLocalAirDate().getTime();
    }

    @TypeConverter
    public static Episode stringToEpisode(String string) {
        if (string == null) {
            return null;
        } else {
            String[] strings = string.split(",");

            String parentMediaId = strings[0].split(":")[1];
            parentMediaId = !parentMediaId.equals(NULL_STRING)? parentMediaId: null;

            int seasonNumber = Integer.valueOf(strings[1].split(":")[1]);
            int episodeNumber = Integer.valueOf(strings[2].split(":")[1]);
            int source = Integer.valueOf(strings[3].split(":")[1]);

            String airDate = strings[4].split(":", 2)[1];
            airDate = !airDate.equals(NULL_STRING)? airDate: null;

            return new Episode(parentMediaId, seasonNumber, episodeNumber, source, airDate);
        }
    }

    @TypeConverter
    public static String episodeToString(Episode episode) {
        if (episode != null) {
            String airDate = episode.getBestAvailableDateString() != null
                    ? episode.getBestAvailableDateString() : NULL_STRING;
            return "parent_media_id" +
                    ":" +
                    episode.parentMediaId +
                    "," +
                    "season_number" +
                    ":" +
                    episode.seasonNumber +
                    "," +
                    "episode_number" +
                    ":" +
                    episode.episodeNumber +
                    "," +
                    "source" +
                    ":" +
                    episode.source +
                    "," +
                    "air_date" +
                    ":" +
                    airDate;
        } else {
            return null;
        }
    }

    @TypeConverter
    public static String searchTagsToString(List<SearchMediaTag> searchTags) {
        StringBuilder stringOfTags = new StringBuilder();
        if (searchTags != null) {
            for (SearchTag searchTag : searchTags) {
                stringOfTags.append(searchTag.mTag);
                stringOfTags.append(" ");
            }
        }

        return stringOfTags.toString();
    }

    @TypeConverter
    public static List<SearchMediaTag> stringToSearchTags(String tagString) {
        List<SearchMediaTag> searchTags = new ArrayList<>();

        String trimmedString = tagString.trim();
        String[] strings = trimmedString.split(" ");

        for (String tag : strings) {
            searchTags.add(new SearchMediaTag(tag));
        }

        return searchTags;
    }
}
