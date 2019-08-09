package com.upkipp.onmywatch.utils.network_utils;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.utils.SearchPreferences;

public class NetworkFunctions {
    //agnostic search request
    public static ANRequest agnosticSearchRequest(String urlFormat, SearchPreferences searchPreferences, Context context) {
        //build request using Fast Android Networking
        ANRequest.GetRequestBuilder requestBuilder = AndroidNetworking.get(urlFormat);
        requestBuilder.addQueryParameter(ApiConstants.API_KEY_KEY, ApiConstants.API_KEY);

        String[] preferenceKeyCheckList = context.getResources().getStringArray(R.array.preference_key_checklist);
        String[] preferenceValueList = searchPreferences.getPreferenceValueList();

        String url = ApiConstants.SEARCH_BASE_URL+"/?api_key="+ApiConstants.API_KEY;

        for (int index = 0; index < preferenceKeyCheckList.length; index++) {
            url += "&"+preferenceKeyCheckList[index]+"="+preferenceValueList[index];
            requestBuilder.addQueryParameter(preferenceKeyCheckList[index], preferenceValueList[index]);
        }

//        Log.d("PresetUrl", url);

        return requestBuilder.build();
    }

    //agnostic search request
    public static ANRequest agnosticDetailRequestById(String urlFormat, String id, SearchPreferences searchPreferences, Context context) {
        //build request using Fast Android Networking
        ANRequest.GetRequestBuilder requestBuilder = AndroidNetworking.get(urlFormat);
        requestBuilder.addQueryParameter(ApiConstants.API_KEY_KEY, ApiConstants.API_KEY);
        requestBuilder.addPathParameter(ApiConstants.MOVIE_ID_KEY, id);

        String[] preferenceKeyCheckList = context.getResources().getStringArray(R.array.preference_key_checklist);
        String[] preferenceValueList = searchPreferences.getPreferenceValueList();

        String workingUrl = urlFormat;
        String finalUrl = "";

        workingUrl = workingUrl.replace("{"+ApiConstants.API_KEY_KEY+"}", ApiConstants.API_KEY);
        workingUrl = workingUrl.replace("{"+ApiConstants.MOVIE_ID_KEY+"}", id);


        for (int index = 0; index < preferenceKeyCheckList.length; index++) {
            String pattern = "{"+preferenceKeyCheckList[index]+"}";
            if (urlFormat.contains(pattern)) {
                Log.d("pattern:", preferenceKeyCheckList[index]);
                Log.d("found pattern:", workingUrl);

                workingUrl = workingUrl.replace(pattern, preferenceValueList[index]);
            }
            requestBuilder.addQueryParameter(preferenceKeyCheckList[index], preferenceValueList[index]);
        }

        Log.d("finalUrl:", workingUrl);

        return requestBuilder.build();
    }

//    //preset movie search request
//    public static ANRequest buildPresetMovieSearchRequest(String urlFormat, SearchPreferences searchPreferences) {
//        //build request using Fast Android Networking
//        ANRequest request = AndroidNetworking.get(urlFormat)
//                .addQueryParameter(ApiConstants.API_KEY_KEY, ApiConstants.API_KEY)
//                .addQueryParameter(ApiConstants.LANG_KEY, searchPreferences.getLanguageValue())
//                .addQueryParameter(ApiConstants.PAGE_KEY, String.valueOf(searchPreferences.getTargetPage()))
//                .addQueryParameter(ApiConstants.REGION_KEY, String.valueOf(searchPreferences.getRegion()))
//                .build();
//
//        Log.d("PresetUrl", urlFormat
//                +"?api_key=" +ApiConstants.API_KEY
//                +"&language="+searchPreferences.getLanguageValue()
//                +"&page="+searchPreferences.getTargetPage()
//                +"&region="+searchPreferences.getRegion());
//
//        return request;
//    }
//
//    //preset tv search request
//    public static ANRequest buildPresetTvSearchRequest(String urlFormat, SearchPreferences searchPreferences) {
//        //build request using Fast Android Networking
//        ANRequest request = AndroidNetworking.get(urlFormat)
//                .addQueryParameter(ApiConstants.API_KEY_KEY, ApiConstants.API_KEY)
//                .addQueryParameter(ApiConstants.LANG_KEY, searchPreferences.getLanguageValue())
//                .addQueryParameter(ApiConstants.PAGE_KEY, String.valueOf(searchPreferences.getTargetPage()))
//                .build();
//
//        Log.d("PresetUrl", urlFormat
//                +"?api_key=" +ApiConstants.API_KEY
//                +"&language="+searchPreferences.getLanguageValue()
//                +"&page="+searchPreferences.getTargetPage());
//
//        return request;
//    }

//    //movie search request
//    public static ANRequest buildSearchMovieRequest(SearchPreferences searchPreferences, String queryString) {
//        //build request using Fast Android Networking
//        ANRequest request = AndroidNetworking.get(ApiConstants.MOVIE_SEARCH_FORMAT)
//                .addQueryParameter(ApiConstants.API_KEY_KEY, ApiConstants.API_KEY)
//                .addQueryParameter(ApiConstants.LANG_KEY, searchPreferences.getLanguageValue())
//                .addQueryParameter(ApiConstants.QUERY_KEY, queryString)
//                .addQueryParameter(ApiConstants.PAGE_KEY, String.valueOf(searchPreferences.getTargetPage()))
//                .build();
//
////                Log.d("SearchUrl", "https://api.themoviedb.org/3/search/movie"
////                +"?api_key=" + ApiConstants.API_KEY +"&language=" + searchPreferences.getLanguageValue()
////                        + "&query=" + queryString
////                +"&page="+searchPreferences.getTargetPage());
//
//        return request;
//    }

    //loads images into ImageViews using glide
    public static void loadImage(Context context, String urlString, ImageView imageView) {
        //configure glide behaviour
        RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.poster_image_placeholder)
                .error(android.R.drawable.ic_menu_gallery);

        Glide.with(context)
                .load(urlString)
                .transition(DrawableTransitionOptions.withCrossFade())
                .apply(requestOptions)
                .into(imageView);
    }

//    public static ANRequest loadVideos(String movieId, SearchPreferences searchPreferences) {
////        String videoUrl = SearchPreferences.PRESET_BASE_URL + formatANNKey(MOVIE_PARAM_KEY) + "/videos" +
////                SearchPreferences.API_QUERY_FORMAT + SearchPreferences.API_PLACEHOLDER_FORMAT;
//
//        //build request using Fast Android Networking
//        //format: "https://{base_url}/{movie_id}/videos"
//        ANRequest request = AndroidNetworking.get(ApiConstants.VIDEOS_URL_FORMAT)
//                .addPathParameter(ApiConstants.PH_MOVIE_ID_KEY, movieId)
//                .addPathParameter(ApiConstants.API_KEY_KEY, ApiConstants.API_KEY)
//                .addPathParameter(ApiConstants.LANG_KEY,
//                        searchPreferences.getLanguageValue())
//                .build();
//
//        return request;
//
//    }
//
//    public static ANRequest loadReviews(String movieId, SearchPreferences searchPreferences) {
////        String reviewUrl = SearchPreferences.PRESET_BASE_URL + formatANNKey(MOVIE_PARAM_KEY) + "/reviews" +
////                SearchPreferences.API_QUERY_FORMAT + SearchPreferences.API_PLACEHOLDER_FORMAT;
//
//        //build request using Fast Android Networking
//        //format: "https://{base_url}/{movie_id}/reviews"
//        ANRequest request = AndroidNetworking.get(ApiConstants.REVIEWS_URL_FORMAT)
//                .addPathParameter(ApiConstants.PH_MOVIE_ID_KEY, movieId)
//                .addPathParameter(ApiConstants.API_KEY_KEY, ApiConstants.API_KEY)
//                .addPathParameter(ApiConstants.LANG_KEY,
//                        searchPreferences.getLanguageValue())
//                .build();
//
//        return request;
//
//    }

//    public static String getMovieUrl(Context context, String movieId) {
//        return context.getString(R.string.movie_base_url) + "/" + movieId;
//    }
}
