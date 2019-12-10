/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import com.atmko.onmywatch.utils.GeneralUtils;

import org.parceler.Parcel;

/*
 * episode model class
 */

@Parcel
public class Episode extends ScheduledMedia {
    public Episode() {
    }

    public Episode(GeneralUtils.DateInject dateInject) {
        this.mDateInject = dateInject;
    }
}