/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

import com.atmko.onmywatch.BuildConfig;

public class ApiConstants {
    //api key
    static final String API_KEY = BuildConfig.apiKey;

    public static final String MEDIA_TYPE_MOVIE = "movie";
    public static final String MEDIA_TYPE_TV = "tv";

    //URLS
    public static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String VIDEO_IMAGE_URL_FORMAT = "https://img.youtube.com/vi/{video_id}/sddefault.jpg";
    public static final String YOUTUBE_INTENT_BASE_URL = "https://www.youtube.com/watch?v=";

    //PLACEHOLDER KEYS
    static final String API_KEY_KEY = "api_key";
    public static final String VIDEO_IMG_KEY = "video_id";
    public static final String VIDEO_IMG_SIZE = "img_size";

    //IMAGE VALUES
    public static final String POSTER_IMAGE_SIZE = "/w185";
    public static final String SPOTLIGHT_POSTER_IMAGE_SIZE = "/w342";
    public static final String BACKDROP_IMAGE_SIZE = "/w780";
    public static final Double POSTER_ASPECT_RATIO = 1.5;

    //RESULTS KEYS
    public static final String RESULTS_KEY = "results";

    //for paging
    public static int RESULTS_PER_PAGE = 20;
    public static final String TOTAL_PAGES_KEY = "total_pages";

    //for videos
    public static final String VIDEOS_KEY = "videos";
    public static final String VIDEO_TYPE_KEY = "type";
    public static final String VIDEO_PATH_KEY = "key";
    public static final String VIDEO_NAME_KEY = "name";
    public static final String VIDEO_SITE_KEY = "site";

    //for reviews
    public static final String REVIEW_AUTHOR_KEY = "author";
    public static final String REVIEW_CONTENT_KEY = "content";

    //DETAILS KEYS
    public static final String RELEASE_STATUS_KEY = "status";
    public static final String GENRES_KEY = "genres";
    public static final String GENRE_ID_KEY = "id";
    public static final String GENRE_NAME = "name";

}
