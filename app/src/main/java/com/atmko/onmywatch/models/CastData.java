/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.room.Ignore;

import com.atmko.onmywatch.utils.network_utils.ApiConstants;

import org.parceler.Parcel;

//TODO: access is weaker to accommodate parceler library
@SuppressWarnings("WeakerAccess")
@Parcel
public class CastData {
    String mId;
    String mCreditId;
    String mName;
    String mGender;
    String mCharacter;
    String mProfilePath;
    double mPopularity;
    String mOrder;

    //constructor for parceler
    @Ignore
    public CastData() {

    }

    public CastData(String id, String creditId, String name, String gender, String character,
                    String profilePath, String order) {
        this.mId = id;
        this.mCreditId = creditId;
        this.mName = name;
        this.mGender = gender;
        this.mCharacter = character;
        if (profilePath == null) {
            this.mProfilePath = null;
        } else {
            this.mProfilePath = ApiConstants.IMAGE_BASE_URL +
                    ApiConstants.POSTER_IMAGE_SIZE +
                    profilePath;
        }        this.mOrder = order;
    }

    public String getId() {
        return mId;
    }

    public void setId(String id) {
        this.mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getGender() {
        return mGender;
    }

    public void setGender(String gender) {
        this.mGender = gender;
    }

    public String getCharacter() {
        return mCharacter;
    }

    public void setCharacter(String character) {
        this.mCharacter = character;
    }

    public String getProfilePath() {
        return mProfilePath;
    }

    public void setProfilePath(String profilePath) {
        this.mProfilePath = profilePath;
    }

    public double getPopularity() {
        return mPopularity;
    }

    public void setPopularity(double popularity) {
        this.mPopularity = popularity;
    }

    public String getOrder() {
        return mOrder;
    }

    public void setOrder(String order) {
        this.mOrder = order;
    }
}
