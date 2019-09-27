/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

abstract public class ListModel {
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "id", index = true) String mName;
    @ColumnInfo(name = "item_count") int mItemCount;

    @NonNull
    public String getName() {
        return mName;
    }

    public void setName(@NonNull String mName) {
        this.mName = mName;
    }

    public int getItemCount() {
        return mItemCount;
    }

    public void setItemCount(int mItemCount) {
        this.mItemCount = mItemCount;
    }
}