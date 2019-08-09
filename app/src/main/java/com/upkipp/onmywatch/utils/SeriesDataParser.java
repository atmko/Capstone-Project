/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.upkipp.onmywatch.utils;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.atmko.stack.Stack;
import com.google.gson.Gson;
import com.upkipp.onmywatch.models.CastData;
import com.upkipp.onmywatch.models.Season;
import com.upkipp.onmywatch.models.SeriesData;
import com.upkipp.onmywatch.utils.network_utils.ApiConstants;
import com.upkipp.onmywatch.utils.network_utils.PeopleApiConstants;
import com.upkipp.onmywatch.utils.network_utils.TvApiConstants;

import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SeriesDataParser {
    //check for int/double errors
    private static String checkAndConvertNumber(Object number) {
        return String.valueOf(number);
    }

    private static double convertTo2Sf(Double number) {
        return Math.round(number * 10) / 10.0;
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    public static List<SeriesData> parseData(String returnedJSONString, Stack stack,
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

        //SearchAdapter data will be stored as ArrayList<TvData>
        List<SeriesData> seriesDataList = new ArrayList<>();

        //iterate through each tv series in results
        for (int index = 0; index < results.size() ; index++) {
            Map currentObject = (Map) results.get(index);//get current tv series

            //create new TvData from currentObject
            seriesDataList.add(parseTvMap(currentObject));
        }

        return seriesDataList;
    }

    static SeriesData parseTvMap(Map tvDataMap) {
        SeriesData seriesData =
                new SeriesData(
                        //get by keys
                        checkAndConvertNumber(tvDataMap.get(TvApiConstants.ID_KEY)),

                        checkAndConvertNumber(tvDataMap.get(TvApiConstants.VOTE_COUNT_KEY)),

                        checkAndConvertNumber(
                                convertTo2Sf(((double) tvDataMap.get(TvApiConstants.VOTE_AVERAGE_KEY)))),

                        (String) tvDataMap.get(TvApiConstants.NAME_KEY),

                        (Double) tvDataMap.get(TvApiConstants.POPULARITY_KEY),

                        (String) tvDataMap.get(TvApiConstants.POSTER_PATH_KEY),

                        (String) tvDataMap.get(TvApiConstants.ORIG_LANG_KEY),

                        (String) tvDataMap.get(TvApiConstants.ORIG_NAME_KEY),

                        (ArrayList<String>) tvDataMap.get(TvApiConstants.ORIGIN_COUNTRY_KEY),

                        //GSON numbers default to doubles
                        convertGenreIdsToIntegers(
                                (List<Double>) tvDataMap.get(TvApiConstants.GENRE_IDS_KEY)),

                        (String) tvDataMap.get(TvApiConstants.BACKDROP_PATH_KEY),

                        (String) tvDataMap.get(TvApiConstants.OVERVIEW_KEY),

                        parseDateInfo((String) tvDataMap.get(TvApiConstants.FIRST_AIR_DATE_KEY))
                );

        return seriesData;
    }

    public static SeriesData parseDetails(String returnedJSONString, SeriesData seriesData) {
        //skips code below if returnedJSONString null or empty
        if (returnedJSONString == null || returnedJSONString.equals("")){
            return seriesData;
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

        seriesData.setCast(castList);

        //parse seasons
        ArrayList<Season> seasonList = new ArrayList<>();

        ArrayList<Map> seasonMapList = (ArrayList<Map>) returnedMap.get(TvApiConstants.SEASONS_KEY);
        for (Map seasonMap: seasonMapList) {
            String airDate = (String) seasonMap.get(TvApiConstants.AIR_DATE_KEY);
            String episodeCount = checkAndConvertNumber(seasonMap.get(TvApiConstants.EPISODE_COUNT_KEY));
            String id = checkAndConvertNumber(seasonMap.get(TvApiConstants.ID_KEY));
            String name = (String) seasonMap.get(TvApiConstants.NAME_KEY);
            String overview = (String) seasonMap.get(TvApiConstants.OVERVIEW_KEY);
            String posterPath = (String) seasonMap.get(TvApiConstants.POSTER_PATH_KEY);
            String seasonNumber = checkAndConvertNumber(((Double) seasonMap.get(TvApiConstants.SEASON_NUMBER_KEY)).intValue());

            Season season = new Season(id, name, airDate, seasonNumber, posterPath, overview, episodeCount);
            seasonList.add(season);
        }

        seriesData.setSeasons(seasonList);

        return seriesData;
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

//    private static List<Integer> convertDoubleToInteger(Double doubleValue) {
//        List<Integer> genreIntegerIds = new ArrayList<>();
//
//        doubleValue.intValue();
//
//        return genreIntegerIds;
//    }

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
        ArrayList results = (ArrayList) returnedMap.get(TvApiConstants.RESULTS_KEY);

//        iterate through each review in results
        for (int index = 0; index < results.size() ; index++) {
            Map currentResult = (Map) results.get(index);//get current review

            Map<String, String> newReview = new HashMap<>();

            newReview.put(TvApiConstants.REVIEW_AUTHOR_KEY,
                    ((String) currentResult.get(TvApiConstants.REVIEW_AUTHOR_KEY)));

            newReview.put(TvApiConstants.REVIEW_CONTENT_KEY,
                    (String) currentResult.get(TvApiConstants.REVIEW_CONTENT_KEY));

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
        ArrayList results = (ArrayList) returnedMap.get(TvApiConstants.RESULTS_KEY);

//        iterate through each video in results
        assert results != null;//null checking occurs in above if statement
        for (int index = 0; index < results.size() ; index++) {
            Map currentResult = (Map) results.get(index);//get current review

            Map<String, String> newVideo = new HashMap<>();

            newVideo.put(TvApiConstants.VIDEO_PATH_KEY,
                    (String) currentResult.get(TvApiConstants.VIDEO_PATH_KEY));

            newVideo.put(TvApiConstants.VIDEO_SITE_KEY,
                    (String) currentResult.get(TvApiConstants.VIDEO_SITE_KEY));

            newVideo.put(TvApiConstants.VIDEO_TYPE_KEY,
                    (String) currentResult.get(TvApiConstants.VIDEO_TYPE_KEY));

            newVideo.put(TvApiConstants.VIDEO_NAME_KEY,
                    (String) currentResult.get(TvApiConstants.VIDEO_NAME_KEY));

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
