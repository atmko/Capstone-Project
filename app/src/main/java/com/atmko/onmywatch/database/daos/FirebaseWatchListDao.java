/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.firebase.firestore.CollectionReference;

/*
 * WatchList firebase Dao
 */

public class FirebaseWatchListDao {

    private static final String WATCH_LISTS_PATH = "watch_lists";

    public static CollectionReference getAllWatchLists() {
        return MasterActivity.getUserDbHomeReference()
                .collection(WATCH_LISTS_PATH);
    }
}
