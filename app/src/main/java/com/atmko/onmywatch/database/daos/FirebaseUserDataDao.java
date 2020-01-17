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
    public static DocumentReference getUserTier() {
        return MasterActivity.getUserDbHomeReference();
    }
}
