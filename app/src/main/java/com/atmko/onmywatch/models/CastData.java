/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.room.Ignore;

import com.atmko.onmywatch.utils.api_utils.ApiConstants;

import org.parceler.Parcel;

//TODO: access is weaker to accommodate parceler library
@Parcel
public class CastData extends PersonData{
    String mCreditId;
    String mCharacter;
    String mOrder;

    //constructor for parceler
    @Ignore
    CastData() {

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

    public String getCharacter() {
        return mCharacter;
    }
}
