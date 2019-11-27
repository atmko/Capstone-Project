/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

import com.atmko.onmywatch.BuildConfig;

/*
 * class containing trakt api value constants for all media types
 */

public class TraktApiConstants {
    //api key
    static final String API_KEY = BuildConfig.traktApiKey;

    public static final String MEDIA_TYPE_MOVIE = "movie";
    public static final String MEDIA_TYPE_SHOW = "show";

    //REQUEST HEADER KEYS
    static final String API_KEY_KEY = "trakt-api-key";
    static final String API_VERSION_KEY = "trakt-api-version";
    static final String API_VERSION = "2";

    //EPISODE VALUES
    public static final String NEXT_EPISODE_TO_AIR_KEY = "next_episode_to_air";
    public static final String AIR_DATE_KEY = "air_date";

    //TRAKT EXTERNAL KEYS
    public static final String TRAKT_KEY = "trakt";
    public static final String IDS_KEY = "ids";

    //TRAKT SCHEDULING KEYS
    public static final String AIRS_KEY = "airs";
    public static final String DAY_KEY = "day";
    public static final String TIME_KEY = "time";
    public static final String TIMEZONE_KEY = "timezone";

    //ERROR CODES
    public static final int TOO_MANY_REQUESTS = 429;
}
