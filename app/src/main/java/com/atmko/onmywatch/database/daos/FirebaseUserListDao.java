/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.UserListModel;
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
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.atmko.onmywatch.database.FirebaseDatabase.getFirstDocument;
import static com.atmko.onmywatch.models.ListModel.ITEM_COUNT_KEY;
import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;

/*
 * UserList firebase Dao
 */

public class FirebaseUserListDao implements UserListsDao{

    public static final String USER_LISTS_PATH = "user_lists";

    @Override
    public void addList(UserListModel userListModel) {
        Task<DocumentReference> task = MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .add(userListModel.parseListModelToDataMap());

        try {
            userListModel.setUniqueExternalId(Tasks.await(task).getId());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void addUserListBatch(List<Map<String, Object>> userListMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> userListMap: userListMaps) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(USER_LISTS_PATH)
                    .document();

            batch.set(documentReference, userListMap);
        }

        try {
            Tasks.await(batch.commit());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public LiveData<List<UserListModel>> getAllLists() {
        final MutableLiveData<List<UserListModel>> lists = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<UserListModel> listModels = new ArrayList<>();

                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                if (snapshots.getDocuments().size() != 0) {
                    List<DocumentSnapshot> listDocuments = snapshots.getDocuments();

                    for (DocumentSnapshot documentSnapshot: listDocuments) {
                        listModels.add(parseUserListModel(documentSnapshot));
                    }
                }

                //set lists
                lists.setValue(listModels);
            }
        });

        return lists;
    }

    @Override
    public UserListModel getListByNameAlt(String name) {
        UserListModel listModel = null;

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .whereEqualTo(LIST_NAME_KEY, name)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            DocumentSnapshot documentSnapshot = getFirstDocument(snapshots);

            if (documentSnapshot != null) {
                listModel = parseUserListModel(documentSnapshot);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return listModel;
    }

    @Override
    public List<UserListModel> getAllListsAlt() {
        List<UserListModel> lists = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                UserListModel list = parseUserListModel(documentSnapshot);
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
    public LiveData<List<UserListModel>> getListsWithNameLike(String name) {
        return null;
    }

    @Override
    public void updateListConfiguration(UserListModel userListModel) {
        Task<Void> task = MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .document(userListModel.getUniqueExternalId())
                .update(userListModel.parseListModelToDataMap());

        try {
            Tasks.await(task);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void updateUserListBatch(List<String> batchDocumentIds, List<Map<String,
            Object>> userListMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (int i = 0; i < batchDocumentIds.size(); i++) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(USER_LISTS_PATH)
                    .document(batchDocumentIds.get(i));

            batch.update(documentReference, userListMaps.get(i));
        }

        try {
            Tasks.await(batch.commit());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteList(UserListModel userListModel) {
        Task<Void> task = MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .document(userListModel.getUniqueExternalId())
                .delete();

        Task<HttpsCallableResult> task2 = FirebaseFunctions.getInstance()
                .getHttpsCallable("onDeleteList").call(userListModel.getName());

        try {
            Tasks.await(task);
            Tasks.await(task2);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //TODO: list name and list count are never null when retrieved from the database
    @SuppressWarnings("ConstantConditions")
    public static UserListModel parseUserListModel(DocumentSnapshot document) {
        String listName = document.getString(LIST_NAME_KEY);
        int listCount = ((Long) document.get(ITEM_COUNT_KEY)).intValue();

        UserListModel userListModel = new UserListModel(listName, listCount);
        userListModel.setUniqueExternalId(document.getId());

        return userListModel;
    }
}
