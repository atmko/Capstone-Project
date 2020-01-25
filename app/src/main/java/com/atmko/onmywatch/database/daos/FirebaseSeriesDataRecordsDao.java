/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.MediaRecord;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesDataRecord;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.atmko.onmywatch.database.FirebaseDatabase.getFirstDocument;
import static com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao.SERIES_COLLECTION_PATH;
import static com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao.parseDataMapToMediaData;
import static com.atmko.onmywatch.database.daos.FirebaseUserListDao.USER_LISTS_PATH;
import static com.atmko.onmywatch.database.daos.FirebaseUserListDao.parseUserListModel;
import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.ID_KEY;

/*
 * SeriesDataRecords firebase Dao
 */

public class FirebaseSeriesDataRecordsDao implements SeriesDataRecordsDao {

    private static final String SERIES_DATA_RECORDS_COLLECTION_PATH = "series_data_records";

    @Override
    public void addRecord(SeriesDataRecord seriesDataRecord) {
        Task<DocumentReference> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_DATA_RECORDS_COLLECTION_PATH)
                .add(seriesDataRecord.parseListModelToDataMap());

        try {
            seriesDataRecord.setUniqueExternalId(Tasks.await(task).getId());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void addSeriesDataRecordBatch(List<Map<String, Object>> watchListMaps) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> recordMap: watchListMaps) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(SERIES_DATA_RECORDS_COLLECTION_PATH)
                    .document();

            batch.set(documentReference, recordMap);
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
    public SeriesDataRecord getRecordByIdAlt(String mediaId, String listName) {
        SeriesDataRecord record = null;

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_DATA_RECORDS_COLLECTION_PATH)
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
    public List<SeriesDataRecord> getAllRecordsAlt() {
        List<SeriesDataRecord> records = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_DATA_RECORDS_COLLECTION_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                SeriesDataRecord record = parseMediaRecord(documentSnapshot);
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
    public LiveData<List<SeriesData>> getAllSeriesInList(String listId) {
        final MutableLiveData<List<SeriesData>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, listId);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MediaRecord> records = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        SeriesDataRecord record = parseMediaRecord(document);

                        records.add(record);
                    }
                }

                Query query = MasterActivity.getUserDbHomeReference()
                        .collection(SERIES_COLLECTION_PATH);

                query.addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        final List<SeriesData> mediaList = new ArrayList<>();

                        if (snapshots != null) {
                            List<DocumentSnapshot> documents = snapshots.getDocuments();

                            for (DocumentSnapshot document: documents) {
                                if (document.getData() == null) continue;

                                SeriesData mediaData = parseDataMapToMediaData(document);

                                mediaList.add(mediaData);
                            }
                        }

                        List<SeriesData> finalMediaList = new ArrayList<>();

                        List<String> mediaNames = MediaRecord.extractMediaNames(records);

                        for (SeriesData mediaData: mediaList) {
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
    public List<SeriesData> getAllSeriesInListAlt(String listId) {
        List<SeriesData> finalMediaList = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, listId)
                .get();

        Task<QuerySnapshot> task2 = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            QuerySnapshot snapshots2 = Tasks.await(task2);

            List<MediaRecord> records = new ArrayList<>();
            List<SeriesData> mediaList = new ArrayList<>();

            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                SeriesDataRecord record = parseMediaRecord(documentSnapshot);
                records.add(record);
            }

            for (DocumentSnapshot documentSnapshot: snapshots2.getDocuments()) {
                SeriesData mediaData = parseDataMapToMediaData(documentSnapshot);
                mediaList.add(mediaData);
            }

            List<String> mediaNames = MediaRecord.extractMediaNames(records);

            for (SeriesData mediaData: mediaList) {
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
    public LiveData<List<SeriesData>> getSeriesWithNameLike(String listId, String mediaTitle) {
        return null;
    }

    @Override
    public List<SeriesDataRecord> getAllRecordsOfListAlt(String listId) {
        List<SeriesDataRecord> records = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, listId)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                SeriesDataRecord record = parseMediaRecord(documentSnapshot);
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
    public LiveData<List<String>> getAllListNamesContainingMedia(String seriesId) {
        final MutableLiveData<List<String>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, seriesId);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MediaRecord> records = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        SeriesDataRecord record = parseMediaRecord(document);

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
    public LiveData<List<UserListModel>> getAllListsContainingMedia(String seriesId) {
        final MutableLiveData<List<UserListModel>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, seriesId);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MediaRecord> records = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        SeriesDataRecord record = parseMediaRecord(document);

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

    @Override
    public List<UserListModel> getAllListsContainingMediaAlt(String seriesId) {
        List<UserListModel> finalLists = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_DATA_RECORDS_COLLECTION_PATH)
                .whereEqualTo(LIST_NAME_KEY, seriesId)
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
                SeriesDataRecord record = parseMediaRecord(documentSnapshot);
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
    public void deleteRecord(SeriesDataRecord seriesDataRecord) {
        Task<Void> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_DATA_RECORDS_COLLECTION_PATH)
                .document(seriesDataRecord.getUniqueExternalId())
                .delete();

        try {
            Tasks.await(task);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static SeriesDataRecord parseMediaRecord(DocumentSnapshot document) {
        SeriesDataRecord mediaRecord = new SeriesDataRecord(
                (String) document.get(ID_KEY),
                (String) document.get(LIST_NAME_KEY)
        );

        mediaRecord.setUniqueExternalId(document.getId());
        return mediaRecord;
    }
}