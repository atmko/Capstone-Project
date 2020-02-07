/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.atmko.onmywatch.models.SearchMediaTag;

import java.util.List;

@Dao
public interface SearchMediaTagsDao {
    @Insert()
    void addTag(SearchMediaTag tag);

    //alternate method without live data
    @Query("SELECT * FROM search_media_tags WHERE tag LIKE '%'||:formattedTag||'%' ORDER BY tag ASC LIMIT 4")
    List<String> getTagsLikeAlt(String formattedTag);

    //alternate method without live data
    @Query("SELECT * FROM search_media_tags WHERE tag = :tag")
    SearchMediaTag getTagAlt(String tag);

    @Delete
    void deleteTag(SearchMediaTag searchTag);
}