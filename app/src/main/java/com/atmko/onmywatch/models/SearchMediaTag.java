package com.atmko.onmywatch.models;

import androidx.room.Entity;
import androidx.room.Ignore;

import org.parceler.Parcel;

@Parcel
@Entity(tableName = "search_media_tags")
public class SearchMediaTag extends SearchTag {
    //constructor for parceler
    @Ignore
    public SearchMediaTag() {
    }

    public SearchMediaTag(String tag) {
        this.mTag = tag.toLowerCase();
    }
}
