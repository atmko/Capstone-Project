/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.api_utils;

import com.atmko.onmywatch.models.MediaData;
import com.atmko.stack.Stack;
import com.google.gson.Gson;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.PersonData;
import com.atmko.onmywatch.models.SeriesData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.utils.GeneralUtils.checkAndConvertInteger;

public class PersonDataParser {
    public static List<PersonData> parseData(String returnedJSONString, Stack stack,
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

        //SearchAdapter data will be stored as ArrayList<PeopleData>
        List<PersonData> personDataList = new ArrayList<>();

        //iterate through each people in results
        for (int index = 0; index < results.size() ; index++) {
            Map currentObject = (Map) results.get(index);//get current people

            //create new PeopleData from currentObject
            personDataList.add(parsePeopleSearchMap(currentObject));
        }

        return personDataList;
    }

    private static PersonData parsePeopleMap(Map peopleDataMap) {
        //create new PeopleData from peopleDataMap
        return new PersonData(
                //get by keys
                checkAndConvertInteger(peopleDataMap.get(ApiConstants.ID_KEY)),

                (String) (peopleDataMap.get(PeopleApiConstants.PROFILE_PATH_KEY)),

                (String) peopleDataMap.get(PeopleApiConstants.NAME_KEY),

                (Double) peopleDataMap.get(PeopleApiConstants.POPULARITY_KEY),

                (boolean) peopleDataMap.get(PeopleApiConstants.ADULT_KEY)
        );
    }

    private static PersonData parsePeopleSearchMap(Map peopleDataMap) {
        List<List> knownForList =
                parseKnownForObjects((List<Map>) peopleDataMap.get(PeopleApiConstants.KNOWN_FOR_KEY));

        //create new PeopleData from peopleDataMap
        return new PersonData(
                //get by keys
                checkAndConvertInteger(peopleDataMap.get(ApiConstants.ID_KEY)),

                (String) (peopleDataMap.get(PeopleApiConstants.PROFILE_PATH_KEY)),

                knownForList,

                (String) peopleDataMap.get(PeopleApiConstants.NAME_KEY),

                (Double) peopleDataMap.get(PeopleApiConstants.POPULARITY_KEY),

                (boolean) peopleDataMap.get(PeopleApiConstants.ADULT_KEY)
        );
    }

    @SuppressWarnings("unchecked")
    public static PersonData parseDetails(String returnedJSONString, PersonData personData) {
        //skips code below if returnedJSONString null or empty
        if (returnedJSONString == null || returnedJSONString.equals("")){
            //return same series data
            return personData;
        }

        Gson gson = new Gson();
        Map returnedMap = gson.fromJson(returnedJSONString, Map.class);

        //create new person data
        PersonData detailsPersonData = parsePeopleMap(returnedMap);

        detailsPersonData.setOverview((String) returnedMap.get(PeopleApiConstants.BIOGRAPHY_KEY));
        detailsPersonData.setDateOfBirth((String) returnedMap.get(PeopleApiConstants.BIRTHDAY_KEY));
        detailsPersonData.setProfession((String) returnedMap.get(PeopleApiConstants.KNOWN_FOR_DEPARTMENT_KEY));

        Map combinedCreditsMap = (Map) returnedMap.get(PeopleApiConstants.COMBINED_CREDITS_KEY);

        if (combinedCreditsMap != null) {
            List<List> knownForList =
                    parseKnownForObjects(((List<Map>) combinedCreditsMap.get(PeopleApiConstants.CAST_KEY)));

            detailsPersonData.setKnownForMovies((List<MovieData>) knownForList.get(0));
            detailsPersonData.setKnownForSeries((List<SeriesData>) knownForList.get(1));
        }

        return detailsPersonData;
    }

    private static ArrayList<List> parseKnownForObjects(List<Map> mediaList) {
        ArrayList<List> knownForList = new ArrayList<>();

        ArrayList<MediaData> knownForMovies = new ArrayList<>();
        ArrayList<MediaData> knownForSeries = new ArrayList<>();

        if (mediaList == null) {
            return knownForList;
        }

        for (Map media: mediaList) {
            try {
                String mediaType = ((String) media.get(PeopleApiConstants.MEDIA_TYPE_KEY));
                if (mediaType == null) continue;
                if (mediaType.equals(ApiConstants.MEDIA_TYPE_MOVIE)) {
                    MovieData movieData = MovieDataParser.parseMovieMap(media);
                    knownForMovies.add(movieData);

                } else if (mediaType.equals(ApiConstants.MEDIA_TYPE_TV)) {
                    SeriesData seriesData = SeriesDataParser.parseTvMap(media);
                    knownForSeries.add(seriesData);
                }

            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        knownForList.add(knownForMovies);
        knownForList.add(knownForSeries);

        return knownForList;
    }
}
