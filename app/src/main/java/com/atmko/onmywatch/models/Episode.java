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
    }
}