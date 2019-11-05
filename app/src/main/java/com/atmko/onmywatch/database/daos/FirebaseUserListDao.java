/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;

import java.util.Map;

/*
 * UserList firebase Dao
 */

public class FirebaseUserListDao {

    private static final String USER_LISTS_PATH = "user_lists";

    public static Task<DocumentReference> addUserList(Map<String, Object> userListMap) {
        //create new list
        return MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .add(userListMap);
    }

    public static CollectionReference getAllUserLists() {
        return MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH);
    }

    public static Task<Void> updateUserList(String documentId, Map<String, Object> userListMap) {
        return MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .document(documentId)
                .update(userListMap);
    }

    public static Task<Void> deleteUserList(String documentId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .document(documentId)
                .delete();
    }
}
