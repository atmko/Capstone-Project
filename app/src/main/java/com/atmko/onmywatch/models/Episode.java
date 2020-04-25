/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import org.parceler.Parcel;

import java.util.Date;

/*
 * episode model class
 */

@Parcel
public class Episode extends ScheduledMedia {
    public String parentMediaId;
    public int seasonNumber;
    public int episodeNumber;
    public long timestamp;
    public int source;

    public Episode() {
    }

    public Episode(String parentMediaId, int seasonNumber, int episodeNumber, int source, String airDate) {
        this.parentMediaId = parentMediaId;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
        this.source = source;
        try {
            setAirDate(airDate);
        } catch (DateFormatException e) {
            e.printStackTrace();
        }

        if (getBestLocalAirDate() != null) {
            timestamp = getBestLocalAirDate().getTime();

        } else {
            timestamp = Long.MAX_VALUE;
        }
    }

    public boolean isInFuture() {
        return new Date().getTime() < timestamp;
    }
}