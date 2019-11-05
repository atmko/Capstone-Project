/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.firebase.firestore.Query;

import static com.atmko.onmywatch.models.MediaData.WATCH_STATUS_KEY;
import static com.atmko.onmywatch.utils.network_utils.ApiConstants.ID_KEY;

/*
 * SeriesData firebase Dao
 */

public class FirebaseSeriesDataDao {

    private static final String SERIES_COLLECTION_PATH = "series";

    public static Query getSeriesById(String seriesId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, seriesId);
    }

    public static Query getSeriesByWatchStatus(int watchStatus) {
        return MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereEqualTo(WATCH_STATUS_KEY, watchStatus);
    }
}
