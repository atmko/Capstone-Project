/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

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
    public int getSeriesCount() {
        return mSeriesCount;
    }
}
