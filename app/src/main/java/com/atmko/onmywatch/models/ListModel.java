/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;

import java.util.HashMap;
import java.util.Map;

abstract public class ListModel {
    public static final String LIST_NAME_KEY = "name";
    public static final String ITEM_COUNT_KEY = "item_count";
    public static final String DOCUMENT_ID_KEY = "document_id";

    String mDocumentId;
    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "id", index = true) String mName;
    @ColumnInfo(name = "item_count") int mItemCount;

    public String getDocumentId() {
        return mDocumentId;
    }

    public void setDocumentId(String id) {
        mDocumentId = id;
    }

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