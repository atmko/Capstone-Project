/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.firebase.firestore.DocumentReference;

/*
 * MovieData firebase Dao
 */

public class FirebaseUserDataDao {
    private static final String ADMIN_COLLECTION_TITLE = "admin";
    private static final String PROFILE_INFO_DOCUMENT_TITLE = "profile_info";

    public static DocumentReference getUserTier() {
        return MasterActivity.getUserDbHomeReference()
                .collection(ADMIN_COLLECTION_TITLE)
                .document(PROFILE_INFO_DOCUMENT_TITLE);
    }
}
