/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import org.parceler.Parcel;

/*
 * episode model class
 */

@Parcel
public class Episode {
    public String airDate;

    public Episode() {
    }

    public Episode(String airDate) {
        this.airDate = airDate;
    }
}
