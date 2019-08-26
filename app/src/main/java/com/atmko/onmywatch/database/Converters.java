package com.atmko.onmywatch.database;

import android.util.SparseBooleanArray;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.Arrays;

public class Converters {
    private static final String ORIGIN_SEPARATOR = ",";

    @TypeConverter
    public static String sparseBooleanArrayToString(SparseBooleanArray sparseBooleanArray) {
        String convertedString = "";

        for (int index = 0; index < sparseBooleanArray.size(); index++) {
            int conditionKey = sparseBooleanArray.keyAt(index);
            boolean conditionValue = sparseBooleanArray.valueAt(index);

            if (!(index == sparseBooleanArray.size() - 1)) {
                convertedString +=
                        String.valueOf(conditionKey) + ":" + String.valueOf(conditionValue) + ",";

            } else {
                convertedString += String.valueOf(conditionKey) + ":" + String.valueOf(conditionValue);

            }
        }

        return convertedString;
    }

    @TypeConverter
    public static SparseBooleanArray stringToSparseBooleanArray(String stringValues) {
        SparseBooleanArray conditionValues = new SparseBooleanArray();

        String[] splitValueStrings = stringValues.split(",");

        for (String splitValueString : splitValueStrings) {
            String[] keyValueSplit = splitValueString.split(":");

            int key = Integer.valueOf(keyValueSplit[0]);
            boolean value = Boolean.valueOf(keyValueSplit[1]);

            conditionValues.put(key, value);
        }

        return conditionValues;
    }

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
