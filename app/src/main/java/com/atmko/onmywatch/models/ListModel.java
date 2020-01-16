/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    Map<String, Object> getFirebaseListModelMap(ListModel listModel) {
        Map<String, Object> mediaDataMap = new HashMap<>();

        mediaDataMap.put(LIST_NAME_KEY, listModel.getName());
        mediaDataMap.put(ITEM_COUNT_KEY, listModel.getItemCount());

        return mediaDataMap;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ListModel) {
            return ((ListModel) obj).getName().equals(this.mName);

        } else {
            return super.equals(obj);

        }
    }
}