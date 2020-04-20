package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(primaryKeys = {"tag"})
public abstract class SearchTag {
    @NonNull
    @ColumnInfo(name = "tag")  public String mTag = "";

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof SearchTag) {
            return ((SearchTag) obj).mTag.equals(this.mTag);

        } else {
            return super.equals(obj);
        }
    }
}
