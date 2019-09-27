/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database;

import android.util.SparseBooleanArray;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;

public class Converters {
    private static final String ORIGIN_SEPARATOR = ",";

    @TypeConverter
    public static ArrayList<String> stringToOriginList(String formattedOriginString) {
        String[] originStringArray = formattedOriginString.split(ORIGIN_SEPARATOR);

        ArrayList<String> countryOfOrigin = new ArrayList<>(Arrays.asList(originStringArray));

        return countryOfOrigin;
    }

    @TypeConverter
    public static String countryOriginListToString(ArrayList<String> originCountryList) {
        String formattedOriginString = "";

        for (String originCountry: originCountryList) {
            formattedOriginString += originCountry + ORIGIN_SEPARATOR;
        }

        return formattedOriginString;
    }
}
