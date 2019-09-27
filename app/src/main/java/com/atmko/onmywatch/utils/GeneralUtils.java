/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    //Note not original code.
    //forgotten reference
    static double convertTo2Sf(Double number) {
        return Math.round(number * 10) / 10.0;
    }

    static String parseDateInfo(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");

        Date date = null;

        try {
            date = dateFormat.parse ( dateString );

        } catch (NullPointerException e) {
            e.printStackTrace();

        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            return dateFormat.format(date);
        } catch (NullPointerException e) {
            return dateString;
        }
    }
}
