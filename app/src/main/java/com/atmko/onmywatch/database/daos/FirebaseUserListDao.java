/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;
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

    public static Task<Void> addUserListBatch(List<Map<String, Object>> userListMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> userListMap: userListMaps) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(USER_LISTS_PATH)
                    .document();

            batch.set(documentReference, userListMap);
        }

        return batch.commit();
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

    public static Task<Void> updateUserListBatch(List<String> batchDocumentIds, List<Map<String,
            Object>> userListMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (int i = 0; i < batchDocumentIds.size(); i++) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(USER_LISTS_PATH)
                    .document(batchDocumentIds.get(i));

            batch.update(documentReference, userListMaps.get(i));
        }

        return batch.commit();
    }

    public static Task<Void> deleteUserList(String documentId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .document(documentId)
                .delete();
    }
}
