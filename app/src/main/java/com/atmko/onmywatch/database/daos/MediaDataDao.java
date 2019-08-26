package com.atmko.onmywatch.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import com.atmko.onmywatch.models.ListCounts;

@Dao
public interface MediaDataDao {
@Query("SELECT (SELECT COUNT(*) FROM movies WHERE watch_status = :watchStatus) mMoviesCount, (SELECT COUNT(*) FROM series WHERE watch_status = :watchStatus) mSeriesCount FROM (SELECT * FROM movies UNION SELECT * FROM series)")
    LiveData<ListCounts> getAgnosticWatchStatusCount(int watchStatus);
}
