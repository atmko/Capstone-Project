/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;

/*
 * UserList firebase Dao
 */

public class FirebaseUserListDao {

    private static final String USER_LISTS_PATH = "user_lists";

    public static CollectionReference getAllUserLists() {
        return MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH);
    }

    public static Task<Void> deleteUserList(String documentId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .document(documentId)
                .delete();
    }
}
