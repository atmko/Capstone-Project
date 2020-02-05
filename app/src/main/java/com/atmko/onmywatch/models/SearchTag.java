package com.atmko.onmywatch.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import org.parceler.Parcel;

@Parcel
@Entity(tableName = "search_tags")
public class SearchTag {
    @PrimaryKey
    @ColumnInfo(name = "tag")
    @NonNull
    public String mTag;

    //constructor for parceler
    @Ignore
    public SearchTag() {
    }

    public SearchTag(String tag) {
        this.mTag = tag.toLowerCase();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof SearchTag) {
            return ((SearchTag) obj).mTag.equals(this.mTag);

        } else {
            return super.equals(obj);
        }
    }
}
