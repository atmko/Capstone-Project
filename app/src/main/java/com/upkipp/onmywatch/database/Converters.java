package com.upkipp.onmywatch.database;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;

public class Converters {

    @TypeConverter
    public static ArrayList<Integer> stringToGenreList(String formattedGenreString) {
        String[] genreIdStringArray = formattedGenreString.split(",");

        ArrayList<Integer> genreIds = new ArrayList<>();

        for (String genreIdString: genreIdStringArray) {
            genreIds.add(Integer.valueOf(genreIdString));
        }

        return genreIds;
    }

    @TypeConverter
    public static String genreListToString(ArrayList<Integer> genreList) {
        String formattedGenreString = "";

        for (int genreId: genreList) {
            formattedGenreString += genreId + ",";
        }

        return formattedGenreString;
    }

    @TypeConverter
    public static ArrayList<String> stringToOriginList(String formattedOriginString) {
        String[] originStringArray = formattedOriginString.split(",");

        ArrayList<String> countryOfOrigin = new ArrayList<>();

        countryOfOrigin.addAll(Arrays.asList(originStringArray));

        return countryOfOrigin;
    }

    @TypeConverter
    public static String countryOriginListToString(ArrayList<String> originCountryList) {
        String formattedOriginString = "";

        for (String originCouuntry: originCountryList) {
            formattedOriginString += originCouuntry + ",";
        }

        return formattedOriginString;
    }
}
