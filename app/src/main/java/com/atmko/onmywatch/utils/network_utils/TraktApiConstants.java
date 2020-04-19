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
    public static final String API_KEY = BuildConfig.traktApiKey;

    public static final String MEDIA_TYPE_SHOW = "show";

    public static final String TRAKT_ID_KEY = "trakt_id";

    //REQUEST HEADER KEYS
    public static final String API_KEY_KEY = "trakt-api-key";
    public static final String API_VERSION_KEY = "trakt-api-version";
    public static final String API_VERSION = "2";

    //TRAKT EXTERNAL KEYS
    public static final String TRAKT_KEY = "trakt";
    public static final String IDS_KEY = "ids";

    //ERROR CODES
    public static final int TOO_MANY_REQUESTS = 429;
}
