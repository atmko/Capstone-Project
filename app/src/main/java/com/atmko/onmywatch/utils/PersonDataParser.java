/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils;

import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.stack.Stack;
import com.google.gson.Gson;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.PersonData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.network_utils.PeopleApiConstants;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PersonDataParser {
    //check for int/double errors
    private static String checkAndConvertNumber(Object number) {
        return String.valueOf(number);
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
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
            personDataList.add(parsePeopleMap(currentObject));
        }

        return personDataList;
    }

    static PersonData parsePeopleMap(Map peopleDataMap) {
        //create new PeopleData from peopleDataMap
        PersonData personData =
                new PersonData(
                        //get by keys
                        checkAndConvertNumber(peopleDataMap.get(PeopleApiConstants.ID_KEY)),

                        (String) (peopleDataMap.get(PeopleApiConstants.PROFILE_PATH_KEY)),

                        parseKnownForObjects((ArrayList<Map>) peopleDataMap.get(PeopleApiConstants.KNOWN_FOR_KEY)),

                        (String) peopleDataMap.get(PeopleApiConstants.NAME_KEY),

                        (Double) peopleDataMap.get(PeopleApiConstants.POPULARITY_KEY),

                        (boolean) peopleDataMap.get(PeopleApiConstants.ADULT_KEY)
                );

        return personData;
    }

    private static ArrayList<Object> parseKnownForObjects(ArrayList<Map> mediaList) {
        ArrayList<Object> knownForList = new ArrayList<>();

        if (mediaList == null) {
            return knownForList;
        }

        for (Map media: mediaList) {
            try {
                String mediaType = ((String) media.get(PeopleApiConstants.MEDIA_TYPE_KEY));
                if (mediaType.equals(ApiConstants.MEDIA_TYPE_MOVIE)) {
                    MovieData movieData = MovieDataParser.parseMovieMap(media);
                    knownForList.add(movieData);

                } else if (mediaType.equals(ApiConstants.MEDIA_TYPE_TV)) {
                    SeriesData seriesData = SeriesDataParser.parseTvMap(media);
                    knownForList.add(seriesData);
                }

            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        return knownForList;
    }
}
