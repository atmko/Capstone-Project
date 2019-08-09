/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.upkipp.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.Ignore;

import com.upkipp.onmywatch.utils.network_utils.ApiConstants;

import java.util.ArrayList;
import java.util.List;

public class PersonData {

    String mId;//
    String mProfilePath;
    List<Object> mKnownFor;
    String mName;
    double mPopularity;//
    boolean mAdult;

    //constructor for parceler
    @Ignore
    public PersonData() {

    }

    @Ignore
    public PersonData(@NonNull String id, String profilePath, List<Object> knownFor, String name,
                      double popularity, boolean adult) {

        this.mId = id;
        if (profilePath == null) {
            this.mProfilePath = null;
        } else {
            this.mProfilePath = ApiConstants.IMAGE_BASE_URL +
                    ApiConstants.POSTER_IMAGE_SIZE +
                    profilePath;
        }
        this.mKnownFor = knownFor;
        this.mName = name;
        this.mPopularity = popularity;



        this.mAdult = adult;
    }

//    public People(@NonNull String id, String profilePath, List<Object> knownFor, String name,
//                  double popularity, String imagePath) {
//
//        //check numbers(int / double) and JSONArray objects and replace error values;
//        //Boolean and String error already replaced by default
//        this.mId = id;
//        this.mImagePath = imagePath;
//    }

    public String getId() {
        return mId;
    }

    public String getProfilePath() {
        return mProfilePath;
    }

    public List<Object> getKnownFor() {
        return mKnownFor;
    }

    public String getName() {
        return mName;
    }

    public double getPopularity() {
        return mPopularity;
    }

    public boolean isAdult() {
        return mAdult;
    }
}
