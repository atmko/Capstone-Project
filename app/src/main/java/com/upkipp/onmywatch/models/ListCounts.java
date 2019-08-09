/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.upkipp.onmywatch.models;

import androidx.room.Ignore;

import com.upkipp.onmywatch.utils.network_utils.ApiConstants;

import org.parceler.Parcel;

public class ListCounts {
    int mMoviesCount;
    int mSeriesCount;

    public ListCounts(Integer moviesCount, Integer seriesCount) {
        this.mMoviesCount = moviesCount;
        this.mSeriesCount = seriesCount;
    }

    public int getMoviesCount() {
        return mMoviesCount;
    }

    public void setMoviesCount(int mMoviesCount) {
        this.mMoviesCount = mMoviesCount;
    }

    public int getSeriesCount() {
        return mSeriesCount;
    }

    public void setSeriesCount(int mSeriesCount) {
        this.mSeriesCount = mSeriesCount;
    }
}
