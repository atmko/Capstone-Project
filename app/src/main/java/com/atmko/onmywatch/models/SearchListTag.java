package com.atmko.onmywatch.models;

import androidx.room.Entity;
import androidx.room.Ignore;

import org.parceler.Parcel;

@Parcel
@Entity(tableName = "search_list_tags")
public class SearchListTag extends SearchTag {
    //constructor for parceler
    @Ignore
    public SearchListTag() {
    }

    public SearchListTag(String tag) {
        this.mTag = tag.toLowerCase();
    }
}
