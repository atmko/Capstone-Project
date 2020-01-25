/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.MediaRecord;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.atmko.onmywatch.database.FirebaseDatabase.getFirstDocument;
import static com.atmko.onmywatch.database.daos.FirebaseMovieDataDao.MOVIES_COLLECTION_PATH;
import static com.atmko.onmywatch.database.daos.FirebaseMovieDataDao.parseDataMapToMediaData;
import static com.atmko.onmywatch.database.daos.FirebaseUserListDao.USER_LISTS_PATH;
import static com.atmko.onmywatch.database.daos.FirebaseUserListDao.parseUserListModel;
import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.ID_KEY;

/*
 * MovieDataRecords firebase Dao
 */

public class FirebaseMovieDataRecordsDao implements MovieDataRecordsDao {

    private static final String MOVIE_DATA_RECORDS_COLLECTION_PATH = "movie_data_records";

    @Override
    public void addRecord(MovieDataRecord movieDataRecord) {
        DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .document();

        movieDataRecord.setUniqueExternalId(documentReference.getId());

        documentReference.set(movieDataRecord.parseListModelToDataMap())
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

    public static void addMovieDataRecordBatch(List<Map<String, Object>> recordsMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> seriesDataMap: recordsMaps) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                    .document();

            batch.set(documentReference, seriesDataMap);
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
    public MovieDataRecord getRecordByIdAlt(String mediaId, String listName) {
        MovieDataRecord record = null;

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, mediaId)
                .whereEqualTo(LIST_NAME_KEY, listName)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            DocumentSnapshot documentSnapshot = getFirstDocument(snapshots);

            if (documentSnapshot != null) {
                record = parseMediaRecord(documentSnapshot);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return record;
    }

    @Override
    public List<MovieDataRecord> getAllRecordsAlt() {
        List<MovieDataRecord> records = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                MovieDataRecord record = parseMediaRecord(documentSnapshot);
                records.add(record);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return records;
    }

    @Override
    public LiveData<List<MovieData>> getAllMoviesInList(String listId) {
        final MutableLiveData<List<MovieData>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, listId);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MediaRecord> records = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        MovieDataRecord record = parseMediaRecord(document);

                        records.add(record);
                    }
                }

                Query query = MasterActivity.getUserDbHomeReference()
                        .collection(MOVIES_COLLECTION_PATH);

                query.addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        final List<MovieData> mediaList = new ArrayList<>();

                        if (snapshots != null) {
                            List<DocumentSnapshot> documents = snapshots.getDocuments();

                            for (DocumentSnapshot document: documents) {
                                if (document.getData() == null) continue;

                                MovieData mediaData = parseDataMapToMediaData(document);

                                mediaList.add(mediaData);
                            }
                        }

                        List<MovieData> finalMediaList = new ArrayList<>();

                        List<String> mediaNames = MediaRecord.extractMediaNames(records);

                        for (MovieData mediaData: mediaList) {
                            if (mediaNames.contains(mediaData.getId())) {
                                finalMediaList.add(mediaData);
                            }
                        }

                        //set lists
                        liveData.setValue(finalMediaList);
                    }
                });
            }
        });

        return liveData;
    }

    @Override
    public List<MovieData> getAllMoviesInListAlt(String listId) {
        List<MovieData> finalMediaList = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, listId)
                .get();

        Task<QuerySnapshot> task2 = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            QuerySnapshot snapshots2 = Tasks.await(task2);

            List<MediaRecord> records = new ArrayList<>();
            List<MovieData> mediaList = new ArrayList<>();

            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                MovieDataRecord record = parseMediaRecord(documentSnapshot);
                records.add(record);
            }

            for (DocumentSnapshot documentSnapshot: snapshots2.getDocuments()) {
                MovieData mediaData = parseDataMapToMediaData(documentSnapshot);
                mediaList.add(mediaData);
            }

            List<String> mediaNames = MediaRecord.extractMediaNames(records);

            for (MovieData mediaData: mediaList) {
                if (mediaNames.contains(mediaData.getId())) {
                    finalMediaList.add(mediaData);
                }
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return finalMediaList;
    }

    @Override
    public LiveData<List<MovieData>> getMoviesWithNameLike(String listId, String mediaTitle) {
        return null;
    }

    @Override
    public List<MovieDataRecord> getAllRecordsOfListAlt(String listId) {
        List<MovieDataRecord> records = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, listId)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                MovieDataRecord record = parseMediaRecord(documentSnapshot);
                records.add(record);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return records;
    }

    @Override
    public LiveData<List<String>> getAllListNamesContainingMedia(String movieId) {
        final MutableLiveData<List<String>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, movieId);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MediaRecord> records = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        MovieDataRecord record = parseMediaRecord(document);

                        records.add(record);
                    }
                }

                List<String> listNames = MediaRecord.extractListNames(records);

                //set lists
                liveData.setValue(listNames);
            }
        });

        return liveData;
    }

    @Override
    public LiveData<List<UserListModel>> getAllListsContainingMedia(String movieId) {
        final MutableLiveData<List<UserListModel>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, movieId);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MediaRecord> records = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        MovieDataRecord record = parseMediaRecord(document);

                        records.add(record);
                    }
                }

                Query query = MasterActivity.getUserDbHomeReference()
                        .collection(USER_LISTS_PATH);

                query.addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        final List<UserListModel> lists = new ArrayList<>();

                        if (snapshots != null) {
                            List<DocumentSnapshot> documents = snapshots.getDocuments();

                            for (DocumentSnapshot document: documents) {
                                if (document.getData() == null) continue;

                                UserListModel listModel = parseUserListModel(document);

                                lists.add(listModel);
                            }
                        }

                        List<UserListModel> finalLists = new ArrayList<>();

                        List<String> listNames = MediaRecord.extractListNames(records);

                        for (UserListModel listModel: lists) {
                            if (listNames.contains(listModel.getName())) {
                                finalLists.add(listModel);
                            }
                        }

                        //set lists
                        liveData.setValue(finalLists);
                    }
                });
            }
        });

        return liveData;
    }

    //todo: separate this method into two separate live data queries later combined together in...
    // add to list view model. This is in order to avoid 2nd query of records to get document ids for...
    // deletion, etc , also saving quota
    @Override
    public List<UserListModel> getAllListsContainingMediaAlt(String movieId) {
        List<UserListModel> finalLists = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, movieId)
                .get();

        Task<QuerySnapshot> task2 = MasterActivity.getUserDbHomeReference()
                .collection(USER_LISTS_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            QuerySnapshot snapshots2 = Tasks.await(task2);

            List<MediaRecord> records = new ArrayList<>();
            List<UserListModel> lists = new ArrayList<>();

            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                MovieDataRecord record = parseMediaRecord(documentSnapshot);
                records.add(record);
            }

            for (DocumentSnapshot documentSnapshot: snapshots2.getDocuments()) {
                UserListModel listModel = parseUserListModel(documentSnapshot);
                lists.add(listModel);
            }

            List<String> listNames = MediaRecord.extractListNames(records);

            for (UserListModel listModel: lists) {
                if (listNames.contains(listModel.getName())) {
                    finalLists.add(listModel);
                }
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return finalLists;
    }

    @Override
    public void deleteRecord(MovieDataRecord movieDataRecord) {
        MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_DATA_RECORDS_COLLECTION_PATH)
                .document(movieDataRecord.getUniqueExternalId())
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

    @SuppressWarnings("ConstantConditions")
    private static MovieDataRecord parseMediaRecord(DocumentSnapshot document) {
        MovieDataRecord mediaRecord = new MovieDataRecord(
                (String) document.get(ID_KEY),
                (String) document.get(LIST_NAME_KEY)
        );

        mediaRecord.setUniqueExternalId(document.getId());
        return mediaRecord;
    }
}