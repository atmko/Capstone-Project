/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils;

import com.atmko.onmywatch.utils.network_utils.ApiConstants;

public class GeneralUtils {
    public static final int MILLISECOND_CONVERSION = 1000;

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
