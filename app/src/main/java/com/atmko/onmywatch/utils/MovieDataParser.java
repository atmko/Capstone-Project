/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.atmko.onmywatch.utils.network_utils.MovieApiConstants;
import com.google.gson.Gson;
import com.atmko.stack.Stack;
import com.atmko.onmywatch.models.CastData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.PeopleApiConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.utils.GeneralUtils.checkAndConvertNumber;
import static com.atmko.onmywatch.utils.GeneralUtils.convertTo2Sf;
import static com.atmko.onmywatch.utils.GeneralUtils.parseDateInfo;

public class MovieDataParser {
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public static List<MovieData> parseData(String returnedJSONString, Stack stack,
                                            SearchPreferences searchPreferences) {

        //skips code below if returnedJSONString null or empty
        if (returnedJSONString == null || returnedJSONString.equals("")){
            return new ArrayList<>();
        }

        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(returnedJSONString, Map.class);

        //set total pages available to searchPreferences
        //set current page in searchPreferences
        //note: GSON number format default is double
        Double totalPages = (double) returnedMap.get(ApiConstants.TOTAL_PAGES_KEY);

        searchPreferences.setTotalPages(totalPages.intValue());

        if (stack != null) {
            stack.setTotalPages(totalPages.intValue());
        }

        //use RESULTS_KEY to get results as JSONArray
        ArrayList results = (ArrayList) returnedMap.get(ApiConstants.RESULTS_KEY);

        //SearchAdapter data will be stored as ArrayList<MovieData>
        List<MovieData> movieDataList = new ArrayList<>();

        //iterate through each movie in results
        for (int index = 0; index < results.size() ; index++) {
            Map currentObject = (Map) results.get(index);//get current movie

            //create new MovieData from @param movieDataMap
            movieDataList.add(parseMovieMap(currentObject));
        }

        return movieDataList;
    }

    static MovieData parseMovieMap(Map movieDataMap) {
        return new MovieData(
                //get by keys
                checkAndConvertNumber(movieDataMap.get(ApiConstants.ID_KEY)),

                checkAndConvertNumber(movieDataMap.get(ApiConstants.VOTE_COUNT_KEY)),

                (Boolean) movieDataMap.get(MovieApiConstants.VIDEO_KEY),

                checkAndConvertNumber(
                        convertTo2Sf(((double) movieDataMap.get(ApiConstants.VOTE_AVERAGE_KEY)))),

                (String) movieDataMap.get(MovieApiConstants.TITLE_KEY),

                (Double) movieDataMap.get(ApiConstants.POPULARITY_KEY),

                (String) movieDataMap.get(ApiConstants.POSTER_PATH_KEY),

                (String) movieDataMap.get(ApiConstants.ORIG_LANG_KEY),

                (String) movieDataMap.get(MovieApiConstants.ORIG_TITLE_KEY),

                convertToGenres(((ArrayList<Map>) movieDataMap.get(ApiConstants.GENRES_KEY))),

                (String) movieDataMap.get(ApiConstants.BACKDROP_PATH_KEY),

                (Boolean) movieDataMap.get(MovieApiConstants.ADULT_KEY),

                (String) movieDataMap.get(ApiConstants.OVERVIEW_KEY),

                parseDateInfo((String) movieDataMap.get(MovieApiConstants.RELEASE_DATE_KEY))
        );
    }

    public static MovieData parseDetails(String returnedJSONString, MovieData movieData) {
        //skips code below if returnedJSONString null or empty
        if (returnedJSONString == null || returnedJSONString.equals("")){
            //return same movie data
            return movieData;
        }

        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(returnedJSONString, Map.class);

        //create new movie data
        MovieData detailsMovieData = parseMovieMap(returnedMap);

        //parse cast
        Map creditsMap = (Map) returnedMap.get(PeopleApiConstants.CREDITS_KEY);

        ArrayList<CastData> castList = new ArrayList<>();

        ArrayList<Map> castMapList = (ArrayList<Map>) creditsMap.get(PeopleApiConstants.CAST_KEY);
        for (Map castMap: castMapList) {
            String character = (String) castMap.get(PeopleApiConstants.CHARACTER_KEY);
            String creditId = checkAndConvertNumber(castMap.get(PeopleApiConstants.CREDIT_ID_KEY));
            String id = checkAndConvertNumber(castMap.get(ApiConstants.ID_KEY));
            String name = (String) castMap.get(PeopleApiConstants.NAME_KEY);
            String gender = checkAndConvertNumber(castMap.get(PeopleApiConstants.GENDER_KEY));
            String profilePath = (String) castMap.get(PeopleApiConstants.PROFILE_PATH_KEY);
            String order = checkAndConvertNumber(castMap.get(PeopleApiConstants.ORDER_KEY));

            CastData castData = new CastData(id, creditId, name, gender, character, profilePath, order);
            castList.add(castData);
        }

        detailsMovieData.setCast(castList);
        detailsMovieData.setVideos(parseVideos(((Map) returnedMap.get(ApiConstants.VIDEOS_KEY))));
        detailsMovieData.setReviews(parseReviews(((Map) returnedMap.get(ApiConstants.REVIEWS_KEY))));

        String releaseStatus = ((String) returnedMap.get(ApiConstants.RELEASE_STATUS_KEY));
        detailsMovieData.setReleaseStatus(releaseStatus);

        return detailsMovieData;
    }

    private static ArrayList<String> convertToGenres(ArrayList<Map> rawGenreArray) {
        if (rawGenreArray == null) {
            return new ArrayList<>();
        }

        ArrayList<String> genres = new ArrayList<>();

        for (Map rawGenre: rawGenreArray) {
            String genre = ((String) rawGenre.get(ApiConstants.GENRE_NAME));

            genres.add(genre);
        }

        return genres;
    }

    @SuppressWarnings("ConstantConditions")
    private static ArrayList<Map<String, String>> parseReviews(Map reviewMap) {
        //review data will be stored as Map<String, ArrayList<String>>
        ArrayList<Map<String, String>> reviews = new ArrayList<>();
        //skips code below if returnedJSONString null or empty
        if (reviewMap == null){
            return reviews;
        }

        //use RESULTS_KEY to get results as JSONArray
        ArrayList results = (ArrayList) reviewMap.get(ApiConstants.RESULTS_KEY);

        //iterate through each review in results
        for (int index = 0; index < results.size() ; index++) {
            Map currentResult = (Map) results.get(index);//get current review

            Map<String, String> newReview = new HashMap<>();

            newReview.put(ApiConstants.REVIEW_AUTHOR_KEY,
                    ((String) currentResult.get(ApiConstants.REVIEW_AUTHOR_KEY)));

            newReview.put(ApiConstants.REVIEW_CONTENT_KEY,
                    (String) currentResult.get(ApiConstants.REVIEW_CONTENT_KEY));

            reviews.add(newReview);

        }

        return reviews;
    }

    @SuppressWarnings("ConstantConditions")
    private static ArrayList<Map<String, String>> parseVideos(Map videoMap) {
        //video data will be stored as Map<String, ArrayList<String>>
        ArrayList<Map<String, String>> videos = new ArrayList<>();
        //skips code below if returnedJSONString null or empty
        if (videoMap == null){
            return videos;
        }

        //use RESULTS_KEY to get results as JSONArray
        ArrayList results = (ArrayList) videoMap.get(ApiConstants.RESULTS_KEY);

        //iterate through each video in results
        assert results != null;//null checking occurs in above if statement
        for (int index = 0; index < results.size() ; index++) {
            Map currentResult = (Map) results.get(index);//get current review

            Map<String, String> newVideo = new HashMap<>();

            newVideo.put(ApiConstants.VIDEO_PATH_KEY,
                    (String) currentResult.get(ApiConstants.VIDEO_PATH_KEY));

            newVideo.put(ApiConstants.VIDEO_SITE_KEY,
                    (String) currentResult.get(ApiConstants.VIDEO_SITE_KEY));

            newVideo.put(ApiConstants.VIDEO_TYPE_KEY,
                    (String) currentResult.get(ApiConstants.VIDEO_TYPE_KEY));

            newVideo.put(ApiConstants.VIDEO_NAME_KEY,
                    (String) currentResult.get(ApiConstants.VIDEO_NAME_KEY));

            videos.add(newVideo);

        }

        return videos;
    }

    public static String createVideoImagePath(String videoPath) {

        ANRequest request = AndroidNetworking.get(ApiConstants.VIDEO_IMAGE_URL_FORMAT)
                .addPathParameter(ApiConstants.VIDEO_IMG_KEY, videoPath)
                .build();

        return request.getUrl();

    }
}
