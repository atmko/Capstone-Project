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
    public static Episode longToEpisode(long airDateTimestamp) {
        Episode episode = new Episode();
        Date date = new Date(airDateTimestamp);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        String airDateString = GeneralUtils.parseIsoDateFromCalender(calendar);

        try {
            episode.setAirDate(airDateString);
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        return episode;
    }

    @TypeConverter
    public static long episodeToLong(Episode episode) {
        if (episode == null) return 0;
        return episode.getBestLocalAirDate() == null ? 0
                : episode.getBestLocalAirDate().getTime();
    }

//    @TypeConverter
//    public static Episode stringToEpisode(String string) {
//        String[] strings = string.split(",");
//
//        String parentMediaId = !(strings[0].split(":")[1].equals(NULL_STRING))? strings[0].split(":")[1]: null;
//        int seasonNumber = Integer.valueOf(strings[1].split(":")[1]);
//        int episodeNumber = Integer.valueOf(strings[2].split(":")[1]);
//        String airDate = !(strings[3].split(":")[1]).equals(NULL_STRING)? strings[3].split(":")[1]: null;
//
//        return new Episode(parentMediaId, episodeNumber, airDate);
//    }
//
//    @TypeConverter
//    public static String episodeToString(Episode episode) {
//        String parentMediaId = episode != null? episode.parentMediaId: NULL_STRING;
////        int seasonNumber = episode != null? episode.seasonNumber: 0;
//        int episodeNumber = episode != null? episode.episodeNumber: 0;
//        String airDate = episode != null? episode.getBestAvailableDateString(): NULL_STRING;
//
//        return "parent_media_id" +
//                ":" +
//                parentMediaId +
//                "," +
////                "season_number" +
////                ":" +
////                seasonNumber +
////                "," +
//                "episode_number" +
//                ":" +
//                episodeNumber +
//                "," +
//                "air_date" +
//                ":" +
//                airDate;
//    }

    @TypeConverter
    public static String searchTagsToString(List<SearchMediaTag> searchTags) {
        StringBuilder stringOfTags = new StringBuilder();
        for (SearchTag searchTag : searchTags) {
            stringOfTags.append(searchTag.mTag);
            stringOfTags.append(" ");
        }

        return stringOfTags.toString();
    }

    @TypeConverter
    public static List<SearchMediaTag> stringToSearchTags(String tagString) {
        List<SearchMediaTag> searchTags = new ArrayList<>();

        String trimmedString = tagString.trim();

        if (trimmedString.contains(" ")) {
            String[] strings = tagString.split(" ");
            for (String tag : strings) {
                searchTags.add(new SearchMediaTag(tag));
            }
        }

        return searchTags;
    }
}
