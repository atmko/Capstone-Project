/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.room.Ignore;

import com.atmko.onmywatch.utils.network_utils.ApiConstants;

import org.parceler.Parcel;

@Parcel
public class CastData {
    String id;
    String creditId;
    String name;
    String gender;
    String character;
    String profilePath;
    double popularity;
    String order;

    //constructor for parceler
    @Ignore
    public CastData() {

    }

    public CastData(String id, String creditId, String name, String gender, String character,
                    String profilePath, String order) {
        this.id = id;
        this.creditId = creditId;
        this.name = name;
        this.gender = gender;
        this.character = character;
        if (profilePath == null) {
            this.profilePath = null;
        } else {
            this.profilePath = ApiConstants.IMAGE_BASE_URL +
                    ApiConstants.POSTER_IMAGE_SIZE +
                    profilePath;
        }        this.order = order;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
    }

    public double getPopularity() {
        return popularity;
    }

    public void setPopularity(double popularity) {
        this.popularity = popularity;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }
}
