/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.upkipp.onmywatch.utils.network_utils;

import com.upkipp.onmywatch.BuildConfig;

public class ApiConstants {
    //api key
    public static final String API_KEY = BuildConfig.apiKey;

    //URLS
    private static final String BASE_URL = "https://api.themoviedb.org/3/movie";
    public static final String SEARCH_BASE_URL = "https://api.themoviedb.org/3/search/movie";

    public static final String SEARCH_PRESET_PREFERENCES_FORMAT = "&language={lang}&page={page}&region={region}";
    public static final String SEARCH_MANUAL_PREFERENCES_FORMAT = "&query={query}&language={lang}&page={page}&include_adult={include_adult}&region={region}&year={year}&primary_release_year={primary_release_year}";

    public static final String SEARCH_PRESET_FORMAT = BASE_URL + "/{sort}?api_key={api_key}" + SEARCH_PRESET_PREFERENCES_FORMAT;
    public static final String SEARCH_MANUAL_FORMAT = SEARCH_BASE_URL + "/?api_key={api_key}" + SEARCH_MANUAL_PREFERENCES_FORMAT;

    public static final String REVIEWS_URL_FORMAT = BASE_URL + "/{movie_id}/reviews?api_key={api_key}&language={lang}&page={page}";
    public static final String VIDEOS_URL_FORMAT = BASE_URL + "/{movie_id}/videos?api_key={api_key}&language={lang}";

    public static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String VIDEO_IMAGE_URL_FORMAT = "https://img.youtube.com/vi/{video_id}/sddefault.jpg";
    public static final String YOUTUBE_INTENT_BASE_URL = "https://www.youtube.com/watch?v=";

    //PLACEHOLDER KEYS
    public static final String API_KEY_KEY = "api_key";
    //-------------------------PH >PlaceHolder
    public static final String PH_MOVIE_ID_KEY = "movie_id";
    public static final String LANG_KEY = "lang";
    public static final String REGION_KEY = "region";
    public static final String QUERY_KEY = "query";
    public static final String PAGE_KEY = "page";
    public static final String SORT_KEY = "sort";
    public static final String VIDEO_IMG_KEY = "video_id";
    public static final String VIDEO_IMG_SIZE = "img_size";

    //IMAGE VALUES
    public static final String POSTER_IMAGE_SIZE = "/w185";
    public static final String BACKDROP_IMAGE_SIZE = "/w780";
    public static final Double POSTER_ASPECT_RATIO = 1.5;

    //RESULTS KEYS
    public static final String RESULTS_KEY = "results";
    //for movies
    public static final String VOTE_COUNT_KEY = "vote_count";
    public static final String MOVIE_ID_KEY = "id";
    public static final String VIDEO_KEY = "video";
    public static final String VOTE_AVERAGE_KEY = "vote_average";
    public static final String MOVIE_TITLE_KEY = "title";
    public static final String POPULARITY_KEY = "popularity";
    public static final String POSTER_PATH_KEY = "poster_path";
    public static final String ORIG_LANG_KEY = "original_language";
    public static final String ORIG_TITLE_KEY = "original_title";
    public static final String ORIGIN_COUNTRY_KEY = "origin_country";
    public static final String GENRE_IDS_KEY = "genre_ids";
    public static final String BACKDROP_PATH_KEY = "backdrop_path";
    public static final String ADULT_KEY = "adult";
    public static final String OVERVIEW_KEY = "overview";
    public static final String RELEASE_DATE_KEY = "release_date";

    //for paging
    public static int RESULTS_PER_PAGE = 20;
    public static final String TOTAL_PAGES_KEY = "total_pages";
    public static final String CURRENT_PAGE_KEY = "page";

    //for videos
    public static final String VIDEO_TYPE_KEY = "type";
    public static final String VIDEO_PATH_KEY = "key";
    public static final String VIDEO_NAME_KEY = "name";
    public static final String VIDEO_SITE_KEY = "site";

    //for reviews
    public static final String REVIEW_AUTHOR_KEY = "author";
    public static final String REVIEW_CONTENT_KEY = "content";

}
