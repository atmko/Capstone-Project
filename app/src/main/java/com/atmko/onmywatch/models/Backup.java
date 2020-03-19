package com.atmko.onmywatch.models;

import org.parceler.Parcel;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Parcel
public class Backup {
    public static final int BACKUP_LIMIT = 10;

    public static final String FILE_NAME_KEY = "file_name";
    public static final String TIMESTAMP_KEY = "timestamp";
    private String mFileName;
    private long mTimestamp;

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

    public Map<String, Object> parseBackupToDataMap() {
        Map<String, Object> firebaseBackupMap = new HashMap<>();
        firebaseBackupMap.put(TIMESTAMP_KEY, mTimestamp);
        return firebaseBackupMap;
    }
}
