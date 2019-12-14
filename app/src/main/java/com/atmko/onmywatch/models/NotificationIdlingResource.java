/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

//code provided by udacity
//used in testing processes that happen off UI thread
import androidx.annotation.Nullable;
import androidx.test.espresso.IdlingResource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationIdlingResource implements IdlingResource {
    private static final Object LOCK = new Object();
    private static NotificationIdlingResource sInstance;
    private int idleCountLimit;

    public static NotificationIdlingResource getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                sInstance = new NotificationIdlingResource();
            }
        }

        return sInstance;
    }

    public static NotificationIdlingResource getNotificationIdlingResource() {
        return sInstance;
    }

    @Nullable
    private volatile ResourceCallback mCallback;

    // Idleness is controlled with this boolean.
    private AtomicBoolean mIsIdleNow = new AtomicBoolean(true);
    private AtomicInteger mIdleCounter;

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isIdleNow() {
        return mIsIdleNow.get();
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        mCallback = callback;
    }

    /**
     * Sets the new idle state, if isIdleNow is true, it pings the {@link ResourceCallback}.
     * @param isIdleNow false if there are pending operations, true if idle.
     */
    public void setIdleState(boolean isIdleNow) {

        mIsIdleNow.set(isIdleNow);
        if (isIdleNow && mCallback != null) {
            //noinspection ConstantConditions
            mCallback.onTransitionToIdle();
        }
    }

    public void addToIdleCounter() {
        mIdleCounter.addAndGet(1);

        if (mIdleCounter.intValue() == idleCountLimit) setIdleState(true);
    }

    public void setIdleCountLimit(int idleCountLimit) {
        this.idleCountLimit = idleCountLimit;
        this.mIdleCounter = new AtomicInteger(0);
    }
}