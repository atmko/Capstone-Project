/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.api_utils;

import android.content.Context;
import android.widget.ImageView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.atmko.onmywatch.R;

public class NetworkFunctions {
    //agnostic search request
    public static ANRequest agnosticSearchRequest(String urlFormat,
                                                  SearchPreferences searchPreferences,
                                                  Context context) {
        String finalUrl = ""+urlFormat;

        String[] preferenceKeyCheckList =
                context.getResources().getStringArray(R.array.preference_key_checklist);
        String[] preferenceValueList = searchPreferences.getPreferenceValueList();

        //replace format keys with related values
        for (int index = 0; index < preferenceKeyCheckList.length; index++) {
            String formattedKey = "{" + preferenceKeyCheckList[index] + "}";
            if (preferenceValueList[index] != null && urlFormat.contains(formattedKey)) {
                finalUrl = finalUrl.replace(formattedKey, preferenceValueList[index]);
            }
        }

        //build request using Fast Android Networking
        ANRequest.GetRequestBuilder requestBuilder = AndroidNetworking.get(finalUrl);

        return requestBuilder.build();
    }

    //agnostic search request
    public static ANRequest agnosticDetailRequestById(String urlFormat, String id,
                                                      SearchPreferences searchPreferences,
                                                      Context context) {
        String finalUrl = ""+urlFormat;

        String[] preferenceKeyCheckList =
                context.getResources().getStringArray(R.array.preference_key_checklist);
        String[] preferenceValueList = searchPreferences.getPreferenceValueList();

        for (int index = 0; index < preferenceKeyCheckList.length; index++) {
            String formattedKey = "{" + preferenceKeyCheckList[index] + "}";
            if (preferenceValueList[index] != null && urlFormat.contains(formattedKey)) {
                finalUrl = finalUrl.replace(formattedKey, preferenceValueList[index]);
            }
        }

        //build request using Fast Android Networking
        ANRequest.GetRequestBuilder requestBuilder = AndroidNetworking.get(finalUrl);
        //add media id path parameter
        requestBuilder.addPathParameter(ApiConstants.ID_KEY, id);

        return requestBuilder.build();
    }

    //agnostic request for trakt
    public static ANRequest traktAgnosticRequestById(String urlFormat, String id) {
        //build request using Fast Android Networking
        ANRequest.GetRequestBuilder requestBuilder = AndroidNetworking.get(urlFormat);
        requestBuilder.addHeaders(TraktApiConstants.API_KEY_KEY, TraktApiConstants.API_KEY);
        requestBuilder.addHeaders(TraktApiConstants.API_VERSION_KEY, TraktApiConstants.API_VERSION);
        //add media id path parameter
        requestBuilder.addPathParameter(ApiConstants.ID_KEY, id);

        return requestBuilder.build();
    }

    //agnostic request for trakt
    public static ANRequest traktSeasonEpisodeRequestBy(String urlFormat, String id, String seasonNumber) {
        //build request using Fast Android Networking
        ANRequest.GetRequestBuilder requestBuilder = AndroidNetworking.get(urlFormat);
        requestBuilder.addHeaders(TraktApiConstants.API_KEY_KEY, TraktApiConstants.API_KEY);
        requestBuilder.addHeaders(TraktApiConstants.API_VERSION_KEY, TraktApiConstants.API_VERSION);
        //add media id path parameter
        requestBuilder.addPathParameter(ApiConstants.ID_KEY, id);
        //add season number path parameter
        requestBuilder.addPathParameter(SeriesApiConstants.SEASON_NUM_KEY, seasonNumber);

        return requestBuilder.build();
    }

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

    public static FutureTarget loadWidgetImage(Context context, String path, int width, int height) {
        //configure glide behaviour
        RequestOptions requestOptions = new RequestOptions()
                .placeholder(R.drawable.poster_image_placeholder)
                .error(android.R.drawable.ic_menu_gallery);

        RequestBuilder builder =
                Glide.with(context)
                        .asBitmap()
                        .apply(requestOptions)
                        .load(path);

        return builder.submit(width, height);
    }
}
