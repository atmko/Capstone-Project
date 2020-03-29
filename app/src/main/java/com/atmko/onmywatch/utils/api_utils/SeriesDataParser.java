/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.api_utils;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.atmko.onmywatch.models.CastData;
import com.atmko.onmywatch.models.Episode;
import com.atmko.onmywatch.models.Review;
import com.atmko.onmywatch.models.ScheduledMedia;
import com.atmko.onmywatch.models.Season;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.UpdateNotifierService;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;
import com.atmko.stack.Stack;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.utils.GeneralUtils.checkAndConvertInteger;
import static com.atmko.onmywatch.utils.GeneralUtils.checkAndConvertNumber;
import static com.atmko.onmywatch.utils.GeneralUtils.convertTo2Sf;
import static com.atmko.onmywatch.utils.api_utils.SeriesApiConstants.FIRST_AIR_DATE_KEY;

public class SeriesDataParser {
    public static List<SeriesData> parseData(String returnedJSONString, Stack stack,
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

    static SeriesData parseTvMap(Map seriesDataMap) {
        Double popularity = (Double) seriesDataMap.get(ApiConstants.POPULARITY_KEY);
        popularity = popularity == null ? 0 : popularity;

        return new SeriesData(
                //get by keys
                checkAndConvertInteger(seriesDataMap.get(ApiConstants.ID_KEY)),

                checkAndConvertNumber(seriesDataMap.get(ApiConstants.VOTE_COUNT_KEY)),

                checkAndConvertNumber(
                        convertTo2Sf(((double) seriesDataMap.get(ApiConstants.VOTE_AVERAGE_KEY)))),

                (String) seriesDataMap.get(SeriesApiConstants.NAME_KEY),

                popularity,

                (String) seriesDataMap.get(ApiConstants.POSTER_PATH_KEY),

                (String) seriesDataMap.get(ApiConstants.ORIG_LANG_KEY),

                (String) seriesDataMap.get(SeriesApiConstants.ORIG_NAME_KEY),

                (ArrayList<String>) seriesDataMap.get(SeriesApiConstants.ORIGIN_COUNTRY_KEY),

                convertToGenres(((ArrayList<Map>) seriesDataMap.get(ApiConstants.GENRES_KEY))),

                (String) seriesDataMap.get(ApiConstants.BACKDROP_PATH_KEY),

                (String) seriesDataMap.get(ApiConstants.OVERVIEW_KEY),

                (String) seriesDataMap.get(FIRST_AIR_DATE_KEY)
        );
    }

    public static SeriesData parseDetails(String returnedJSONString, SeriesData seriesData) {
        if (UpdateNotifierService.sActionMode.equals(UpdateNotifierService.ACTION_TESTING)) return seriesData;

        //skips code below if returnedJSONString null or empty
        if (returnedJSONString == null || returnedJSONString.equals("")){
            //return same series data
            return seriesData;
        }

        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(returnedJSONString, Map.class);

        //create new series data
        SeriesData detailsSeriesData = parseTvMap(returnedMap);

        //get rating(maturity rating)
        ArrayList<String> countriesOfOrigin = detailsSeriesData.getCountryOfOrigin();
        String fallbackLocale = countriesOfOrigin.size() >= 1? countriesOfOrigin.get(0) :
                ApiConstants.FALLBACK_LOCALE;
        String contentRating = getContentRating(returnedMap, ApiConstants.USER_LOCALE,
                fallbackLocale);

        //set local date as release date if exists
        if (contentRating != null) {
            detailsSeriesData.setMaturityRating(contentRating);
        }

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

        detailsSeriesData.setCast(castList);
        detailsSeriesData.setVideos(parseVideos(((Map) returnedMap.get(ApiConstants.VIDEOS_KEY))));
        detailsSeriesData.setReviews(parseReviews(((Map) returnedMap.get(ApiConstants.REVIEWS_KEY))));

        String releaseStatus = ((String) returnedMap.get(ApiConstants.RELEASE_STATUS_KEY));
        detailsSeriesData.setReleaseStatus(ApiConstants.TextReplacement.replaceText(releaseStatus));

        Map nextEpisodeToAirMap = ((Map) returnedMap.get(SeriesApiConstants.NEXT_EPISODE_TO_AIR_KEY));

        if (nextEpisodeToAirMap != null) {
            String nextEpisodeAirDate = ((String) nextEpisodeToAirMap.get(TraktApiConstants.FIRST_AIRED_KEY));

            if (nextEpisodeAirDate != null) {
                Episode nextEpisode = EpisodeParser.parseTraktEpisode(seriesData.getId(), returnedMap);
                try {
                    nextEpisode.setAirDate(nextEpisodeAirDate);
                    detailsSeriesData.setNextEpisodeToAir(nextEpisode);
                } catch (ScheduledMedia.DateFormatException e) {
                    e.printStackTrace();
                }
            }
        } else {
            detailsSeriesData.setNextEpisodeToAir(seriesData.getNextEpisodeToAir());
        }

        //preserve the overwritten watch status, user rating, trakt id and unique external id
        detailsSeriesData.setWatchStatus(seriesData.getWatchStatus());
        detailsSeriesData.setUserRating(seriesData.getUserRating());
        detailsSeriesData.setTraktId(seriesData.getTraktId());
        detailsSeriesData.setUniqueExternalId(seriesData.getUniqueExternalId());
        detailsSeriesData.searchTags = seriesData.searchTags;

        return detailsSeriesData;
    }

    //TODO: release dates map contains varying object types
    @SuppressWarnings("unchecked")
    private static String getContentRating(Map<String, Object> detailsMap, String userLocale,
                                           String countryOfOrigin) {
        String contentRating = null;

        Map<String, ArrayList> contentRatingsMap = (Map<String, ArrayList>) detailsMap.get(SeriesApiConstants.CONTENT_RATINGS_KEY);
        if (contentRatingsMap == null) return null;

        ArrayList<Map> ratingResults = (ArrayList<Map>) contentRatingsMap.get(ApiConstants.RESULTS_KEY);
        if (ratingResults == null) return null;

        //iterate through locales
        //locale counter tracks if we are in fallback (value of 2)
        int localeCounter = 0;
        for (Map localeMap: ratingResults) {
            String countryIso = (String) localeMap.get(ApiConstants.COUNTRY_ISO_KEY);
            if (countryIso == null) continue;

            //continue if wrong locale
            if (!countryIso.equals(userLocale) && !countryIso.equals(countryOfOrigin)) continue;

            contentRating = (String) localeMap.get(SeriesApiConstants.RATING_KEY);

            localeCounter += 1;

            //if this is user locale return, otherwise if this is fallback locale return
            if (countryIso.equals(userLocale) || localeCounter >= 2) return contentRating;
        }

        return contentRating;
    }

    public static String parseAndGetTraktId(String returnedJSONString) {
        //skips code below if returnedJSONString null or empty
        if (returnedJSONString == null || returnedJSONString.equals("")){
            //return same series data
            return null;
        }

        Gson gson = new Gson();
        List returnedList = gson.fromJson(returnedJSONString, List.class);

        String traktId = null;

        if (returnedList.size() != 0) {
            try {
                Map firstResult = ((Map) returnedList.get(0));
                //TODO: null pointer exception caught in try block
                //noinspection ConstantConditions
                traktId = checkAndConvertInteger(
                        ((Map) ((Map) firstResult.get(TraktApiConstants.MEDIA_TYPE_SHOW))
                                .get(TraktApiConstants.IDS_KEY))
                                .get(TraktApiConstants.TRAKT_KEY));

            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        return traktId;
    }

    public static SeriesData parseTraktNextEpisodeDetails(String returnedJSONString, SeriesData seriesData) {
        if (UpdateNotifierService.sActionMode.equals(UpdateNotifierService.ACTION_TESTING)) return seriesData;

        //skips code below if returnedJSONString null or empty
        if (returnedJSONString == null || returnedJSONString.equals("")){
            //return same series data
            return seriesData;
        }

        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(returnedJSONString, Map.class);

        if (returnedMap != null) {
            String nextEpisodeAirDate = ((String) returnedMap.get(TraktApiConstants.FIRST_AIRED_KEY));

            if (nextEpisodeAirDate != null) {
                Episode nextEpisode = EpisodeParser.parseTraktEpisode(seriesData.getId(), returnedMap);
                try {
                    nextEpisode.setAirDate(nextEpisodeAirDate);
                    seriesData.setNextEpisodeToAir(nextEpisode);
                } catch (ScheduledMedia.DateFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        return seriesData;
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
    private static ArrayList<Review> parseReviews(Map reviewMap) {
        //review data will be stored as Map<String, ArrayList<String>>
        ArrayList<Review> reviews = new ArrayList<>();
        //skips code below if returnedJSONString null or empty
        if (reviewMap == null){
            return reviews;
        }

        //use RESULTS_KEY to get results as JSONArray
        ArrayList results = (ArrayList) reviewMap.get(ApiConstants.RESULTS_KEY);

        //iterate through each review in results
        for (int index = 0; index < results.size() ; index++) {
            Map currentResult = (Map) results.get(index);//get current review

            Review newReview = new Review(
                    ((String) currentResult.get(ApiConstants.REVIEW_AUTHOR_KEY)),
                    (String) currentResult.get(ApiConstants.REVIEW_CONTENT_KEY)
            );

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

    public static class SeasonParser {
        public static List<Season> parseTraktSeasons(String mediaId, String returnedJSONString) {
            List<Season> seasons = new ArrayList<>();

            //skips code below if returnedJSONString null or empty
            if (returnedJSONString == null || returnedJSONString.equals("")) {
                return seasons;
            }

            Gson gson = new Gson();
            List returnedList = gson.fromJson(returnedJSONString, List.class);

            for (Object seasonMapObject : returnedList) {
                seasons.add(parseTraktSeason(mediaId, (Map) seasonMapObject));
            }

            return seasons;
        }

        static Season parseTraktSeason(String mediaId, Map seasonMap) {
            Double episodeNumberDouble = ((Double) seasonMap.get("number"));
            int seasonNumber = episodeNumberDouble != null ? episodeNumberDouble.intValue() : 0;
            String firstAired = ((String) seasonMap.get("first_aired"));

            Season season = new Season(mediaId, seasonNumber, firstAired);

            Double airedEpisodesDouble = ((Double) seasonMap.get("aired_episodes"));
            season.episodesAired = airedEpisodesDouble != null ? airedEpisodesDouble.intValue() : 0;

            return season;
        }
    }

    public static class EpisodeParser {
        public static List<Episode> parseTraktEpisodes(String mediaId, String returnedJSONString) {
            List<Episode> episodes = new ArrayList<>();

            //skips code below if returnedJSONString null or empty
            if (returnedJSONString == null || returnedJSONString.equals("")) {
                return episodes;
            }

            Gson gson = new Gson();
            List returnedList = gson.fromJson(returnedJSONString, List.class);

            for (Object episodeMapObject : returnedList) {
                episodes.add(parseTraktEpisode(mediaId, ((Map) episodeMapObject)));
            }

            return episodes;
        }

        static Episode parseTraktEpisode(String mediaId, Map episodeMap) {
            Double seasonNumberDouble = ((Double) episodeMap.get("season"));
            Double episodeNumberDouble = ((Double) episodeMap.get("number"));
            int seasonNumber = seasonNumberDouble != null ? seasonNumberDouble.intValue() : 0;
            int episodeNumber = episodeNumberDouble != null ? episodeNumberDouble.intValue() : 0;
            String firstAired = ((String) episodeMap.get("first_aired"));

            return new Episode(
                    mediaId,
                    seasonNumber,
                    episodeNumber,
                    firstAired
            );
        }
    }
}
