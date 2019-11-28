/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils;

import com.atmko.onmywatch.utils.network_utils.ApiConstants;

import java.util.concurrent.TimeUnit;

public class GeneralUtils {
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final int MILLISECOND_CONVERSION = 1000;

    public static String parseIsoDate(String year, String month,
                                      String dayOfMonth, String time, long timezoneOffset) {

        return year +"-" + month + "-" + dayOfMonth + "T"
                + time + ":00.000" + convertTimeZoneOffsetToString(timezoneOffset);
    }

    private static String convertTimeZoneOffsetToString(long offset) {
        String offsetString = String.valueOf(offset);
        //get + / - symbol
        String polarValue = offsetString.substring(0, 1);

        long absoluteOffset = Math.abs(offset);

        //convert offset to hours without remainder
        long wholeHours = TimeUnit.MILLISECONDS.toHours(Math.abs(absoluteOffset));
        //convert offset to minutes without remainder
        long wholeMinutes = TimeUnit.MILLISECONDS.toMinutes(Math.abs(absoluteOffset));

        //convert whole hours to string
        String offsetHours = String.valueOf(wholeHours);

        //get minutes by subtracting whole hours in minutes from whole minutes
        long wholeHoursInMinutes = TimeUnit.HOURS.toMinutes(wholeHours);
        String offsetMinutes = String.valueOf(wholeMinutes - wholeHoursInMinutes);

        //correct to 2 figures
        if (offsetHours.length() == 1) offsetHours = "0" + offsetHours;
        if (offsetMinutes.length() == 1) offsetMinutes = "0" + offsetMinutes;

        return polarValue + offsetHours + offsetMinutes;
    }
    
    public static String convertToDisplayText(String text) {
        String displayText = "";

        String[] strings = text.split(" ");

        for (String string: strings) {
            displayText +=
                    string.replaceFirst(String.valueOf(string.charAt(0)),
                            String.valueOf(string.charAt(0)).toUpperCase()) + " ";
        }

        return displayText;
    }

    //check for int/double errors
    static String checkAndConvertNumber(Object number) {
        return String.valueOf(number);
    }

    //check for int/double errors
    static String checkAndConvertInteger(Object number) {
        return String.valueOf(((Double) number).intValue());
    }

    //Note not original code.
    //forgotten reference
    static double convertTo2Sf(Double number) {
        return Math.round(number * 10) / 10.0;
    }

    public static String parseDateToYear(String dateString) {
        try {
            String[] dateArray = separateDateToStrings(dateString);
            return dateArray[0];

        } catch (NullPointerException e) {
            return dateString;
        }
    }

    private static String[] separateDateToStrings(String dateString) {
        return dateString.split(ApiConstants.DATE_SEPARATOR);
    }

    static int[] separateDateToIntegers(String dateString) {
        String[] separatedArray = dateString.split(ApiConstants.DATE_SEPARATOR);

        int[] finalArray = new int[separatedArray.length];

        for (int i = 0; i< separatedArray.length; i++) {
            finalArray[i] = Integer.parseInt(separatedArray[i]);
        }

        return finalArray;
    }
}
