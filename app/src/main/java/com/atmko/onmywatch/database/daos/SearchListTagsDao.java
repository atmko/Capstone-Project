/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.atmko.onmywatch.models.SearchListTag;

import java.util.List;

@Dao
public interface SearchListTagsDao {
    @Insert()
    void addTag(SearchListTag tag);

    //alternate method without live data
    @Query("SELECT * FROM search_list_tags WHERE tag LIKE '%'||:formattedTag||'%' ORDER BY tag ASC LIMIT 4")
    List<String> getTagsLikeAlt(String formattedTag);

    //alternate method without live data
    @Query("SELECT * FROM search_list_tags WHERE tag = :tag")
    SearchListTag getTagAlt(String tag);

    @Delete
    void deleteTag(SearchListTag searchTag);
}