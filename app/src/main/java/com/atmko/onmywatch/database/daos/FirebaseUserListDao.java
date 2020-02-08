/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.UserListModel;
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
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.Arrays;
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
        DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .document();

        userListModel.setUniqueExternalId(documentReference.getId());

        documentReference.set(userListModel.parseListModelToDataMap())
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

    public static void addUserListBatch(List<Map<String, Object>> userListMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> userListMap: userListMaps) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(USER_LISTS_PATH)
                    .document();

            batch.set(documentReference, userListMap);
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
    public LiveData<List<UserListModel>> getListsWithNameLike(String tag1, String tag2,
                                                              String tag3, String tag4, String tag5,
                                                              String tag6, String tag7) {
        //remove empty tags
        final ArrayList<String> tagList = new ArrayList<>();
        for (String tag: Arrays.asList(tag1, tag2, tag3, tag4, tag5, tag6, tag7)) {
            if (!tag.equals("")) {
                tagList.add(tag);
            }
        }

        final MutableLiveData<List<UserListModel>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<UserListModel> userListModels = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        String listName = ((String) document.getData().get(LIST_NAME_KEY));
                        if (listName == null) continue;

                        List<String> mediaTags = new ArrayList<>();
                        mediaTags.add(listName);

                        mediaTags.retainAll(tagList);

                        if (mediaTags.size() == tagList.size()) {
                            UserListModel listModel = parseUserListModel(document);
                            userListModels.add(listModel);
                        }
                    }

                    liveData.setValue(userListModels);

                } else {
                    liveData.setValue(userListModels);
                }
            }
        });

        return liveData;
    }

    @Override
    public void updateListConfiguration(UserListModel userListModel) {
        MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .document(userListModel.getUniqueExternalId())
                .update(userListModel.parseListModelToDataMap())
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

    public static void updateUserListBatch(List<String> batchDocumentIds, List<Map<String,
            Object>> userListMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (int i = 0; i < batchDocumentIds.size(); i++) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(USER_LISTS_PATH)
                    .document(batchDocumentIds.get(i));

            batch.update(documentReference, userListMaps.get(i));
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
    public void deleteList(final UserListModel userListModel) {
        MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .document(userListModel.getUniqueExternalId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        FirebaseFunctions.getInstance().getHttpsCallable("onDeleteList")
                                .call(userListModel.getName())
                                .addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
                                    @Override
                                    public void onSuccess(HttpsCallableResult httpsCallableResult) {

                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                    }
                                });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
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
