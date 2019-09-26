/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppExecutors {

    //singleton variables
    private static final Object LOCK = new Object();
    private static AppExecutors sInstance;
    private final Executor diskIO;

    private AppExecutors(Executor diskIO) {
        this.diskIO = diskIO;
    }

    public static AppExecutors getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                sInstance = new AppExecutors(Executors.newSingleThreadExecutor());
            }
        }
        return sInstance;
    }

    public Executor diskIO() {
        return diskIO;
    }
}
