/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import org.parceler.Parcel;

/*
 * episode model class
 */

@Parcel
public class Episode extends ScheduledMedia {
    public String parentMediaId;
    public int seasonNumber;
    public int episodeNumber;
    public long timestamp;

    public Episode() {
    }

    public Episode(String parentMediaId, int seasonNumber, int episodeNumber, String airDate) {
        this.parentMediaId = parentMediaId;
        this.seasonNumber = seasonNumber;
        this.episodeNumber = episodeNumber;
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
}