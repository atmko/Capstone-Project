/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils.network_utils;

import com.androidnetworking.core.MainThreadExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AppExecutors {

    //singleton variables
    private static final Object LOCK = new Object();
    private static AppExecutors sInstance;
    private final Executor diskIO;
    private final Executor mainThread;

    private AppExecutors(Executor diskIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.mainThread = mainThread;
    }

    public static AppExecutors getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                sInstance = new AppExecutors(Executors.newSingleThreadExecutor(),
                        new MainThreadExecutor());
            }
        }
        return sInstance;
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

}
