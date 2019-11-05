/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.UserListModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;
import static com.atmko.onmywatch.utils.network_utils.ApiConstants.ID_KEY;

/*
 * MovieDataRecords firebase Dao
 */

public class FirebaseMovieDataRecordsDao {

    private static final String MOVIE_DATA_RECORDS_COLLECTION_PATH = "movie_data_records";

    public static Task<Void> addMediaListRecords(List<UserListModel> originalContainingLists,
                                                 List<UserListModel> newContainingLists,
                                                 String mediaId) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (UserListModel userListModel : newContainingLists) {
            Map<String, Object> mediaRecordMap = new HashMap<>();

            if (!originalContainingLists.contains(userListModel)) {
                //add the media record
                mediaRecordMap.put(LIST_NAME_KEY, userListModel.getName());
                mediaRecordMap.put(ID_KEY, mediaId);

                DocumentReference documentReference =
                        MasterActivity.getUserDbHomeReference()
                                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                                .document();

                batch.set(documentReference, mediaRecordMap);
            }
        }

        return batch.commit();
    }

    public static Task<QuerySnapshot> getMediaListRecord(String listName,
                                                         String mediaId) {

        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, listName)
                .whereEqualTo(ID_KEY, mediaId)
                .get();
    }

    public static Task<Void> deleteMediaListRecord(String documentId) {
        return MasterActivity.getUserDbHomeReference().collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .document(documentId)
                .delete();
    }

    public static Query getAllListsContainingMedia(String mediaId) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, mediaId);
    }

    public static Query getAllMoviesInList(String listName) {
        return MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, listName);
    }
}