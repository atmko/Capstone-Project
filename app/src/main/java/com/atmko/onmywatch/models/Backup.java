package com.atmko.onmywatch.models;

import org.parceler.Parcel;

import java.util.Date;

@Parcel
public class Backup {
    public static final String TIMESTAMP_KEY = "timestamp";
    String mFileName;
    public long mTimestamp;

    //constructor for parceler
    public Backup() {
    }

    public Backup(String fileName, long timestamp) {
        this.mFileName = fileName;
        this.mTimestamp = timestamp;
    }

    public String getFileName() {
        return mFileName;
    }

    public String getTimeString() {
        return new Date(mTimestamp).toString();
    }
}
