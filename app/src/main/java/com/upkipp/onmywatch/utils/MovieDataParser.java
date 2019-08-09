/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.upkipp.onmywatch.utils;

import android.util.SparseArray;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.google.gson.Gson;
import com.atmko.stack.Stack;
import com.upkipp.onmywatch.models.CastData;
import com.upkipp.onmywatch.models.MovieData;
import com.upkipp.onmywatch.utils.network_utils.ApiConstants;
import com.upkipp.onmywatch.utils.network_utils.PeopleApiConstants;

import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieDataParser {
    //check for int/double errors
    private static String checkAndConvertNumber(Object number) {
        if (number != null) {
            return String.valueOf(number);
        } else {
            return String.valueOf((Object) null);
        }
    }

    private static double convertTo2Sf(Double number) {
        return Math.round(number * 10) / 10.0;
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public static List<MovieData> parseData(String returnedJSONString, Stack stack,
                                            SearchPreferences searchPreferences) throws JSONException {

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
        stack.setTotalPages(totalPages.intValue());

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
        MovieData movieData =
                new MovieData(
                        //get by keys
                        checkAndConvertNumber(movieDataMap.get(ApiConstants.MOVIE_ID_KEY)),

                        checkAndConvertNumber(movieDataMap.get(ApiConstants.VOTE_COUNT_KEY)),

                        (Boolean) movieDataMap.get(ApiConstants.VIDEO_KEY),

                        checkAndConvertNumber(
                                convertTo2Sf(((double) movieDataMap.get(ApiConstants.VOTE_AVERAGE_KEY)))),

                        (String) movieDataMap.get(ApiConstants.MOVIE_TITLE_KEY),

                        (Double) movieDataMap.get(ApiConstants.POPULARITY_KEY),

                        (String) movieDataMap.get(ApiConstants.POSTER_PATH_KEY),

                        (String) movieDataMap.get(ApiConstants.ORIG_LANG_KEY),

                        (String) movieDataMap.get(ApiConstants.ORIG_TITLE_KEY),

                        //GSON numbers default to doubles
                        convertGenreIdsToIntegers(
                                (ArrayList<Double>) movieDataMap.get(ApiConstants.GENRE_IDS_KEY)),

                        (String) movieDataMap.get(ApiConstants.BACKDROP_PATH_KEY),

                        (Boolean) movieDataMap.get(ApiConstants.ADULT_KEY),

                        (String) movieDataMap.get(ApiConstants.OVERVIEW_KEY),

                        parseDateInfo((String) movieDataMap.get(ApiConstants.RELEASE_DATE_KEY))
                );

        return movieData;
    }


    public static MovieData parseDetails(String returnedJSONString, MovieData movieData) {
        //skips code below if returnedJSONString null or empty
        if (returnedJSONString == null || returnedJSONString.equals("")){
            return movieData;
        }

        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(returnedJSONString, Map.class);

        //parse cast
        Map creditsMap = (Map) returnedMap.get(PeopleApiConstants.CREDITS_KEY);

        ArrayList<CastData> castList = new ArrayList<>();

        ArrayList<Map> castMapList = (ArrayList<Map>) creditsMap.get(PeopleApiConstants.CAST_KEY);
        for (Map castMap: castMapList) {
            String character = (String) castMap.get(PeopleApiConstants.CHARACTER_KEY);
            String creditId = checkAndConvertNumber(castMap.get(PeopleApiConstants.CREDIT_ID_KEY));
            String id = checkAndConvertNumber(castMap.get(PeopleApiConstants.ID_KEY));
            String name = (String) castMap.get(PeopleApiConstants.NAME_KEY);
            String gender = checkAndConvertNumber(castMap.get(PeopleApiConstants.GENDER_KEY));
            String profilePath = (String) castMap.get(PeopleApiConstants.PROFILE_PATH_KEY);
            String order = checkAndConvertNumber(castMap.get(PeopleApiConstants.ORDER_KEY));

            CastData castData = new CastData(id, creditId, name, gender, character, profilePath, order);
            castList.add(castData);
        }

        movieData.setCast(castList);

        return movieData;
    }

    private static String parseDateInfo(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy");

        Date date = null;

        try {
            date = dateFormat.parse ( dateString );
        } catch (ParseException e) {
            e.printStackTrace();
        }

        try {
            return dateFormat.format(date);
        } catch (NullPointerException e) {
            return dateString;
        }
    }

    private static ArrayList<Integer> convertGenreIdsToIntegers(List<Double> genreDoubleIds) {
        ArrayList<Integer> genreIntegerIds = new ArrayList<>();

        if (genreDoubleIds != null) {
            for (Double genreId : genreDoubleIds) {
                genreIntegerIds.add(genreId.intValue());
            }
        }

        return genreIntegerIds;
    }

    @SuppressWarnings("ConstantConditions")
    public static ArrayList<Map<String, String>> parseReviews(String returnedJSONString) {
        //review data will be stored as Map<String, ArrayList<String>>
        ArrayList<Map<String, String>> reviews = new ArrayList<>();
        //skips code below if returnedJSONString null or empty
        if (returnedJSONString == null || returnedJSONString.equals("")){
            return reviews;
        }

        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(returnedJSONString, Map.class);

        //use RESULTS_KEY to get results as JSONArray
        ArrayList results = (ArrayList) returnedMap.get(ApiConstants.RESULTS_KEY);

//        iterate through each review in results
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
    public static ArrayList<Map<String, String>> parseVideos(String returnedJSONString) {
        //video data will be stored as Map<String, ArrayList<String>>
        ArrayList<Map<String, String>> videos = new ArrayList<>();
        //skips code below if returnedJSONString null or empty
        if (returnedJSONString == null || returnedJSONString.equals("")){
            return videos;
        }

        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(returnedJSONString, Map.class);

        //use RESULTS_KEY to get results as JSONArray
        ArrayList results = (ArrayList) returnedMap.get(ApiConstants.RESULTS_KEY);

//        iterate through each video in results
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

    public static String getGenreById(int id) {
        SparseArray<String> genreList = new SparseArray();
        genreList.put(28, "Action");
        genreList.put(12, "Adventure");
        genreList.put(16, "Animation");
        genreList.put(35, "Comedy");
        genreList.put(80, "Crime");
        genreList.put(99, "Documentary");
        genreList.put(18, "Drama");
        genreList.put(10751, "Family");
        genreList.put(14, "Fantasy");
        genreList.put(36, "History");
        genreList.put(27, "Horror");
        genreList.put(10402, "Music");
        genreList.put(9648, "Mystery");
        genreList.put(10749, "Romance");
        genreList.put(878, "Science Fiction");
        genreList.put(10770, "TV Movie");
        genreList.put(53, "Thriller");
        genreList.put(10752, "War");
        genreList.put(37, "Western");

        return genreList.get(id);

    }

}
