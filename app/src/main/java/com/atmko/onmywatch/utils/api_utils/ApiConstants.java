/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.api_utils;

import com.atmko.onmywatch.BuildConfig;

/*
 * class containing tmdb api value constants for all media types
 */
public class ApiConstants {
    //api key
    public static final String API_KEY = BuildConfig.tmdbApiKey;

    public static final String MEDIA_TYPE_MOVIE = "movie";
    public static final String MEDIA_TYPE_TV = "tv";

    //URLS
    public static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String VIDEO_IMAGE_URL_FORMAT = "https://img.youtube.com/vi/{video_id}/sddefault.jpg";
    public static final String YOUTUBE_INTENT_BASE_URL = "https://www.youtube.com/watch?v=";

    public static final String VIDEO_IMG_KEY = "video_id";

    //IMAGE VALUES
    public static final String POSTER_IMAGE_SIZE = "/w185";
    public static final String SPOTLIGHT_POSTER_IMAGE_SIZE = "/w342";
    public static final String BACKDROP_IMAGE_SIZE = "/w780";
    public static final Double POSTER_ASPECT_RATIO = 1.5;
    public static final Double BACKDROP_HEIGHT_FACTOR = 0.5625;

    //RESULTS KEYS
    public static final String RESULTS_KEY = "results";

    //for paging
    public static final int RESULTS_PER_PAGE = 20;
    public static final String TOTAL_PAGES_KEY = "total_pages";

    //for videos
    public static final String VIDEOS_KEY = "videos";
    public static final String VIDEO_TYPE_KEY = "type";
    public static final String VIDEO_PATH_KEY = "key";
    public static final String VIDEO_NAME_KEY = "name";
    public static final String VIDEO_SITE_KEY = "site";

    //for reviews
    public static final String REVIEWS_KEY = "reviews";
    public static final String REVIEW_AUTHOR_KEY = "author";
    public static final String REVIEW_CONTENT_KEY = "content";

    //BASIC KEYS
    public static final String ID_KEY = "id";
    public static final String VOTE_AVERAGE_KEY = "vote_average";
    public static final String VOTE_COUNT_KEY = "vote_count";
    public static final String POPULARITY_KEY = "popularity";
    public static final String POSTER_PATH_KEY = "poster_path";
    public static final String ORIG_LANG_KEY = "original_language";
    public static final String BACKDROP_PATH_KEY = "backdrop_path";
    public static final String OVERVIEW_KEY = "overview";

    static final String COUNTRY_ISO_KEY = "iso_3166_1";
    static final String USER_LOCALE = "US";
    static final String FALLBACK_LOCALE = "US";

    //DETAILS KEYS
    public static final String RELEASE_STATUS_KEY = "status";
    public static final String GENRES_KEY = "genres";
    public static final String GENRE_NAME = "name";

    //DATE VALUES
    public static final String DATE_SEPARATOR = "-";
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    //RELEASE STATUS VALUES
    public static final String RELEASE_STATUS_PLANNED = "Planned";
    public static final String RELEASE_STATUS_IN_PRODUCTION = "In Production";
    public static final String RELEASE_STATUS_CANCELED = "Canceled";

    //HEADER VALUES
    public static final String RETRY_AFTER_KEY = "Retry-After";

    //ERROR CODES
    public static final int TOO_MANY_REQUESTS = 429;

    public static class TextReplacement {
        public static final String REPLACEMENT_RETURNING_SERIES = "Running";
        public static final String REPLACEMENT_IN_PRODUCTION = "Production";

        public static String replaceText(String originalText) {
            if (SeriesApiConstants.RELEASE_STATUS_RETURNING_SERIES.equals(originalText)) {
                return REPLACEMENT_RETURNING_SERIES;

            } else if (RELEASE_STATUS_IN_PRODUCTION.equals(originalText)) {
                return REPLACEMENT_IN_PRODUCTION;

            } else {
                return originalText;
            }
        }
    }
}
