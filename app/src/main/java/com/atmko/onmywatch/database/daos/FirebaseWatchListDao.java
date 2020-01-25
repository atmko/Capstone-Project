/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.WatchListModel;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.atmko.onmywatch.database.FirebaseDatabase.getFirstDocument;
import static com.atmko.onmywatch.models.ListModel.ITEM_COUNT_KEY;
import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;

/*
 * WatchList firebase Dao
 */

public class FirebaseWatchListDao implements WatchListsDao {

    private static final String WATCH_LISTS_PATH = "watch_lists";

    @Override
    public void addList(WatchListModel watchListModel) {
        DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                .collection(WATCH_LISTS_PATH)
                .document();

        watchListModel.setUniqueExternalId(documentReference.getId());

        documentReference.set(watchListModel.parseListModelToDataMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    public static void addWatchListBatch(List<Map<String, Object>> watchListMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> watchListMap: watchListMaps) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(WATCH_LISTS_PATH)
                    .document();

            batch.set(documentReference, watchListMap);
        }

        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    @Override
    public LiveData<List<WatchListModel>> getAllLists() {
        final MutableLiveData<List<WatchListModel>> lists = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(WATCH_LISTS_PATH);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<WatchListModel> listModels = new ArrayList<>();

                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                if (snapshots.getDocuments().size() != 0) {
                    List<DocumentSnapshot> listDocuments = snapshots.getDocuments();

                    for (DocumentSnapshot documentSnapshot: listDocuments) {
                        listModels.add(parseWatchListModel(documentSnapshot));
                    }
                }

                //set lists
                lists.setValue(listModels);
            }
        });

        return lists;
    }

    @Override
    public List<WatchListModel> getAllListsAlt() {
        List<WatchListModel> lists = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(WATCH_LISTS_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                WatchListModel list = parseWatchListModel(documentSnapshot);
                lists.add(list);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return lists;
    }

    @Override
    public WatchListModel getListByNameAlt(String name) {
        WatchListModel listModel = null;

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(WATCH_LISTS_PATH)
                .whereEqualTo(LIST_NAME_KEY, name)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            DocumentSnapshot documentSnapshot = getFirstDocument(snapshots);

            if (documentSnapshot != null) {
                listModel = parseWatchListModel(documentSnapshot);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return listModel;
    }

    @Override
    public LiveData<List<WatchListModel>> getListsWithNameLike(String name) {
        return null;
    }

    @Override
    public void updateListConfiguration(WatchListModel watchListModel) {
        MasterActivity.getUserDbHomeReference()
                .collection(WATCH_LISTS_PATH)
                .document(watchListModel.getUniqueExternalId())
                .update(watchListModel.parseListModelToDataMap())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    public static void updateWatchListBatch(List<String> batchDocumentIds, List<Map<String,
            Object>> watchListMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (int i = 0; i < batchDocumentIds.size(); i++) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(WATCH_LISTS_PATH)
                    .document(batchDocumentIds.get(i));

            batch.update(documentReference, watchListMaps.get(i));
        }

        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    @Override
    public void deleteList(WatchListModel watchListModel) {
        MasterActivity.getUserDbHomeReference()
                .collection(WATCH_LISTS_PATH)
                .document(watchListModel.getUniqueExternalId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    //TODO: list name and list count are never null when retrieved from the database
    @SuppressWarnings("ConstantConditions")
    private static WatchListModel parseWatchListModel(DocumentSnapshot document) {
        String listName = document.getString(LIST_NAME_KEY);
        int listCount = ((Long) document.get(ITEM_COUNT_KEY)).intValue();

        WatchListModel watchListModel = new WatchListModel(listName, listCount);
        watchListModel.setUniqueExternalId(document.getId());

        return watchListModel;
    }
}
