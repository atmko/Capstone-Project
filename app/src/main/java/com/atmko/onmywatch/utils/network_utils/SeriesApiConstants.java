/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

/*
 * class containing api value constants for series media type
 */
public class SeriesApiConstants {
    public static final String NAME_KEY = "name";
    public static final String ORIG_NAME_KEY = "original_name";
    public static final String ORIGIN_COUNTRY_KEY = "origin_country";
    public static final String FIRST_AIR_DATE_KEY = "first_air_date";

    //RELEASE STATUS VALUES
    private static final String RELEASE_STATUS_RETURNING_SERIES = "Returning Series";
    private static final String RELEASE_STATUS_IN_PRODUCTION = "In Production";
    public static final String RELEASE_STATUS_ENDED = "Ended";
    public static final String RELEASE_STATUS_PILOT = "Pilot";

    //for seasons
    public static final String SEASONS_KEY = "seasons";
    public static final String AIR_DATE_KEY = "air_date";
    public static final String EPISODE_COUNT_KEY = "episode_count";
    public static final String SEASON_NUMBER_KEY = "season_number";

    public static class SeriesTextReplacement {
        public static final String REPLACEMENT_RETURNING_SERIES = "Running";
        static final String REPLACEMENT_IN_PRODUCTION = "Production";

        public static String replaceText(String originalText) {
            if (originalText.equals(SeriesApiConstants.RELEASE_STATUS_RETURNING_SERIES)) {
                return REPLACEMENT_RETURNING_SERIES;

            } else if (originalText.equals(SeriesApiConstants.RELEASE_STATUS_IN_PRODUCTION)) {
                return REPLACEMENT_IN_PRODUCTION;

            } else {
                return originalText;
            }
        }
    }
}
