/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils;

import com.atmko.onmywatch.utils.api_utils.ApiConstants;

import java.util.Calendar;
import java.util.Date;

public class GeneralUtils {
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String OFFSET_SYMBOL = "Z";
    private static final String OFFSET_VALUE_UTC = "+0000";
    public static final int MILLISECOND_CONVERSION = 1000;

    public static boolean LOGIC_BYPASS;

    public static String replaceOffsetSymbol(String airDateString) {
       return airDateString.replace(OFFSET_SYMBOL, OFFSET_VALUE_UTC);
    }

    interface DateCustom {
        Date currentDate();
    }

    public static class DateInject implements DateCustom {
        public static Date custom;
        private static final Object LOCK = new Object();
        private static DateInject sDateInject;

        public static DateInject getInstance() {
            if (sDateInject == null) {
                synchronized (LOCK) {
                    sDateInject = new DateInject();
                }

            }

            return sDateInject;
        }

        @Override
        public Date currentDate() {
            if (custom == null) {
                return new Date();
            } else {
                return custom;
            }
        }
    }

    public static String parseIsoDateFromCalender(Calendar calendar) {
        int utcYear = calendar.get(Calendar.YEAR);
        //+1 adjustment for month zero index mapping
        String utcMonth = addLeadingZero(calendar.get(Calendar.MONTH)+1);
        String utcDay = addLeadingZero(calendar.get(Calendar.DAY_OF_MONTH));
        int utcHour = calendar.get(Calendar.HOUR_OF_DAY);
        String utcMinutes = addLeadingZero(calendar.get(Calendar.MINUTE));
        String utcSeconds = addLeadingZero(calendar.get(Calendar.SECOND));
        String utcMillis = addLeadingZero(calendar.get(Calendar.MILLISECOND));

        return utcYear + "-" + utcMonth + "-" + utcDay + "T" + utcHour + ":" + utcMinutes + ":" + utcSeconds + "." + utcMillis + "Z";
    }

    public static String convertToDisplayText(String text) {
        StringBuilder stringBuilder = new StringBuilder();

        String[] split = text.split(" ");
        for (int i = 0; i < split.length; i++) {
            String word = split[i];
            stringBuilder.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));

            if (i != split.length - 1) stringBuilder.append(" ");
        }

        return stringBuilder.toString();
    }

    //check for int/double errors
    public static String checkAndConvertNumber(Object number) {
        return String.valueOf(number);
    }

    //check for int/double errors
    public static String checkAndConvertInteger(Object number) {
        return String.valueOf(((Double) number).intValue());
    }

    //add leading zero to strings with lengths less than 2
    private static String addLeadingZero(int value) {
        return String.valueOf(value).length() < 2 ? "0"+ value : String.valueOf(value);
    }

    //Note not original code.
    //forgotten reference
    public static double convertTo2Sf(Double number) {
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

}
