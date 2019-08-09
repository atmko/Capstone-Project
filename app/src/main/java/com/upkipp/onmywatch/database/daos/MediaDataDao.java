package com.upkipp.onmywatch.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.upkipp.onmywatch.models.ListCounts;
import com.upkipp.onmywatch.models.MovieData;

import java.util.List;

@Dao
public interface MediaDataDao {
//    @Query("SELECT COUNT(*) FROM (SELECT * FROM movies UNION SELECT * FROM series) WHERE watch_status = :watchStatus")
//    LiveData<Integer> getAgnosticWatchStatusCount(int watchStatus);

    @Query("SELECT (SELECT COUNT(*) FROM movies WHERE watch_status = :watchStatus) mMoviesCount, (SELECT COUNT(*) FROM series WHERE watch_status = :watchStatus) mSeriesCount FROM (SELECT * FROM movies UNION SELECT * FROM series)")
    LiveData<ListCounts> getAgnosticWatchStatusCount(int watchStatus);


}
