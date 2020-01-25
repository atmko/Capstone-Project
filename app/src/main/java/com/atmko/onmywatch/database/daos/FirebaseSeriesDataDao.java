/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.database.Converters;
import com.atmko.onmywatch.models.Episode;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.SeriesApiConstants;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.atmko.onmywatch.database.FirebaseDatabase.getFirstDocument;
import static com.atmko.onmywatch.models.MediaData.WATCH_STATUS_KEY;
import static com.atmko.onmywatch.models.SeriesData.NEXT_EPISODE_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.ID_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.RELEASE_STATUS_KEY;

/*
 * SeriesData firebase Dao
 */

public class FirebaseSeriesDataDao implements SeriesDataDao {
    public static final String SERIES_COLLECTION_PATH = "series";

    @Override
    public void addSeriesData(SeriesData seriesData) {
        Task<DocumentReference> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .add(seriesData.parseMediaDataToDataMap());

        try {
            seriesData.setUniqueExternalId(Tasks.await(task).getId());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void addSeriesDataBatch(List<Map<String, Object>> seriesDataMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> movieDataMap: seriesDataMapList) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(SERIES_COLLECTION_PATH)
                    .document();

            batch.set(documentReference, movieDataMap);
        }

        Task<Void> task = batch.commit();
        try {
            Tasks.await(task);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<SeriesData> getAllSeriesAlt() {
        List<SeriesData> seriesList = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                SeriesData mediaData = parseDataMapToMediaData(documentSnapshot);
                seriesList.add(mediaData);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return seriesList;
    }

    @Override
    public LiveData<SeriesData> getSeriesById(String mediaId) {
        final MutableLiveData<SeriesData> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, mediaId);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                SeriesData mediaData = null;

                if (snapshots != null) {
                    DocumentSnapshot documentSnapshot = getFirstDocument(snapshots);

                    if (documentSnapshot != null) {
                        mediaData = parseDataMapToMediaData(documentSnapshot);
                    }
                }

                //set notifiers
                liveData.setValue(mediaData);
            }
        });

        return liveData;
    }

    @Override
    public SeriesData getSeriesByIdAlt(String mediaId) {
        SeriesData mediaData = null;

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, mediaId)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            DocumentSnapshot documentSnapshot = getFirstDocument(snapshots);

            if (documentSnapshot != null) {
                mediaData = parseDataMapToMediaData(documentSnapshot);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return mediaData;
    }

    @Override
    public LiveData<Integer> getSeriesWatchStatus(String mediaId) {
        final MutableLiveData<Integer> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, mediaId);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                Integer watchStatus = null;

                if (snapshots != null) {
                    DocumentSnapshot documentSnapshot = getFirstDocument(snapshots);

                    if (documentSnapshot != null) {
                        watchStatus = parseDataMapToMediaData(documentSnapshot).getWatchStatus();
                    }
                }

                //set notifiers
                liveData.setValue(watchStatus);
            }
        });

        return liveData;
    }

    @Override
    public LiveData<List<SeriesData>> getSeriesByWatchStatus(int watchStatus) {
        final MutableLiveData<List<SeriesData>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereEqualTo(WATCH_STATUS_KEY, watchStatus);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<SeriesData> seriesList = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        SeriesData mediaData = parseDataMapToMediaData(document);

                        seriesList.add(mediaData);
                    }

                    liveData.setValue(seriesList);

                } else {
                    liveData.setValue(seriesList);
                }
            }
        });

        return liveData;
    }

    @Override
    public List<SeriesData> getSeriesByWatchStatusAlt(int watchStatus) {
        List<SeriesData> seriesList = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereEqualTo(WATCH_STATUS_KEY, watchStatus)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                SeriesData mediaData = parseDataMapToMediaData(documentSnapshot);
                seriesList.add(mediaData);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return seriesList;
    }

    @Override
    public LiveData<List<SeriesData>> getSeriesByWatchStatusLike(int watchStatus, String mediaTitle) {
        return null;
    }

    @Override
    public LiveData<List<SeriesData>> getUserUpcomingEpisodes() {
        final MutableLiveData<List<SeriesData>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereGreaterThan(NEXT_EPISODE_KEY, 0)
                .whereEqualTo(WATCH_STATUS_KEY, 2)
                .orderBy(NEXT_EPISODE_KEY, Query.Direction.ASCENDING)
                .limit(10);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<SeriesData> seriesList = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        SeriesData mediaData = parseDataMapToMediaData(document);

                        seriesList.add(mediaData);
                    }

                    liveData.setValue(seriesList);

                } else {
                    liveData.setValue(seriesList);
                }
            }
        });

        return liveData;
    }

    @Override
    public LiveData<List<SeriesData>> getUndatedSeries() {
        final MutableLiveData<List<SeriesData>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereGreaterThan(WATCH_STATUS_KEY, 0)
                .whereLessThan(WATCH_STATUS_KEY, 3)
                .whereEqualTo(NEXT_EPISODE_KEY, 0)
                .whereIn(RELEASE_STATUS_KEY, Arrays.asList("Planned", "In Production", "Pilot"))
                .limit(10);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<SeriesData> seriesList = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        SeriesData mediaData = parseDataMapToMediaData(document);

                        seriesList.add(mediaData);
                    }

                    liveData.setValue(seriesList);

                } else {
                    liveData.setValue(seriesList);
                }
            }
        });

        return liveData;
    }

    @Override
    public LiveData<List<SeriesData>> getEndedSeries() {
        final MutableLiveData<List<SeriesData>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .whereGreaterThan(WATCH_STATUS_KEY, 0)
                .whereLessThan(WATCH_STATUS_KEY, 3)
                .whereIn(RELEASE_STATUS_KEY, Arrays.asList("Canceled", "Ended"))
                .orderBy(WATCH_STATUS_KEY, Query.Direction.DESCENDING)
                .limit(10);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<SeriesData> seriesList = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        SeriesData mediaData = parseDataMapToMediaData(document);

                        seriesList.add(mediaData);
                    }

                    liveData.setValue(seriesList);

                } else {
                    liveData.setValue(seriesList);
                }
            }
        });

        return liveData;
    }

    @Override
    public void updateSeriesData(SeriesData seriesData) {
        Task<Void> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .document(seriesData.getUniqueExternalId())
                .update(seriesData.parseMediaDataToDataMap());

        try {
            Tasks.await(task);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void updateSeriesDataBatch(List<String> batchDocumentIds, List<Map<String,
            Object>> seriesDataMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (int i = 0; i < batchDocumentIds.size(); i++) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(SERIES_COLLECTION_PATH)
                    .document(batchDocumentIds.get(i));

            batch.update(documentReference, seriesDataMapList.get(i));
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
    public void deleteSeriesData(SeriesData seriesData) {
        Task<Void> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_COLLECTION_PATH)
                .document(seriesData.getUniqueExternalId())
                .delete();

        try {
            Tasks.await(task);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    static SeriesData parseDataMapToMediaData(DocumentSnapshot document) {
        Episode episode = document.get(NEXT_EPISODE_KEY) == null ? null
                : Converters.longToEpisode((long) document.get(NEXT_EPISODE_KEY));

        SeriesData seriesData = new SeriesData(
                (String) document.get(ApiConstants.ID_KEY),
                ((String) document.get(TraktApiConstants.TRAKT_ID_KEY)),
                (String) document.get(ApiConstants.VOTE_AVERAGE_KEY),
                (String) document.get(SeriesApiConstants.NAME_KEY),
                (String) document.get(ApiConstants.POSTER_PATH_KEY),
                (String) document.get(ApiConstants.ORIG_LANG_KEY),
                (String) document.get(SeriesApiConstants.ORIG_NAME_KEY),
                (ArrayList<String>) document.get(SeriesApiConstants.ORIGIN_COUNTRY_KEY),
                (ArrayList<String>) document.get(ApiConstants.GENRES_KEY),
                (String) document.get(ApiConstants.BACKDROP_PATH_KEY),
                (String) document.get(ApiConstants.OVERVIEW_KEY),
                (String) document.get(SeriesApiConstants.FIRST_AIR_DATE_KEY),
                (String) document.get(ApiConstants.RELEASE_STATUS_KEY),
                episode
        );

        seriesData.setWatchStatus(
                ((Long) document.get(MediaData.WATCH_STATUS_KEY)).intValue());
        seriesData.setUserRating(
                ((Long) document.get(MediaData.USER_RATING_KEY)).intValue());

        seriesData.setUniqueExternalId(document.getId());

        return seriesData;
    }
}
