/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.Ignore;

import com.atmko.onmywatch.utils.network_utils.ApiConstants;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.List;

@Parcel
public class PersonData {

    String mId;//
    String mProfilePath;
    ArrayList<MediaData> mKnownFor;
    String mName;
    double mPopularity;//
    boolean mAdult;

    //constructor for parceler
    @Ignore
    public PersonData() {

    }

    @Ignore
    public PersonData(@NonNull String id, String profilePath, ArrayList<MediaData> knownFor,
                      String name, double popularity, boolean adult) {

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

    public String getId() {
        return mId;
    }

    public String getProfilePath() {
        return mProfilePath;
    }

    public List<MediaData> getKnownFor() {
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
