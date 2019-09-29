/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

import android.content.Context;
import android.widget.ImageView;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.common.ANRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.utils.SearchPreferences;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class NetworkFunctions {
    private static final String SOCKET_ADDRESS = "8.8.8.8";
    private static final int PORT_NUMBER = 53;
    private static final int TIMEOUT_MILLIS = 1500;

    //source: https://stackoverflow.com/questions/1560788/how-to-check-internet-access-on-android-inetaddress-never-times-out
    //user: Levit
    //date: Dec 5 '14
    public static boolean isOnline() {
        try {
            Socket sock = new Socket();
            SocketAddress socketAddress = new InetSocketAddress(SOCKET_ADDRESS, PORT_NUMBER);

            sock.connect(socketAddress, TIMEOUT_MILLIS);
            sock.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

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
        requestBuilder.addPathParameter(MovieApiConstants.MOVIE_ID_KEY, id);

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
