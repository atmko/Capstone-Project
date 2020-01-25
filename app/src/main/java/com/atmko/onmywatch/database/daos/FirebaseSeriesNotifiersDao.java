/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.SeriesNotifier;
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
import static com.atmko.onmywatch.models.MediaNotifier.CONDITION_KEY;
import static com.atmko.onmywatch.models.MediaNotifier.NOTIFIER_ID_KEY;

/*
 * SeriesData firebase Dao
 */

public class FirebaseSeriesNotifiersDao implements SeriesNotifierDao {

    private static final String SERIES_NOTIFIERS_COLLECTION_PATH = "series_notifiers";

    @Override
    public void addMediaNotifier(SeriesNotifier seriesNotifier) {
        Task<DocumentReference> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_NOTIFIERS_COLLECTION_PATH)
                .add(seriesNotifier.parseNotifierToDataMap());

        try {
            seriesNotifier.setUniqueExternalId(Tasks.await(task).getId());
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void addSeriesNotifierBatch(List<Map<String, Object>> notifierMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> notifierMap: notifierMapList) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(SERIES_NOTIFIERS_COLLECTION_PATH)
                    .document();

            batch.set(documentReference, notifierMap);
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
    public List<SeriesNotifier> getAllNotifiersAlt() {
        List<SeriesNotifier> notifiers = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_NOTIFIERS_COLLECTION_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                SeriesNotifier notifier = parseMediaNotifier(documentSnapshot);
                notifiers.add(notifier);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return notifiers;
    }

    @Override
    public List<String> getAllMediaIdsAlt() {
        return null;
    }

    @Override
    public SeriesNotifier getNotifierByIdAlt(String mediaId, int condition) {
        SeriesNotifier notifier = null;

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_NOTIFIERS_COLLECTION_PATH)
                .whereEqualTo(NOTIFIER_ID_KEY, mediaId)
                .whereEqualTo(CONDITION_KEY, condition)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            DocumentSnapshot documentSnapshot = getFirstDocument(snapshots);

            if (documentSnapshot != null) {
                notifier = parseMediaNotifier(documentSnapshot);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return notifier;
    }

    @Override
    public LiveData<List<SeriesNotifier>> getNotifiersWithMediaId(String mediaId) {
        final MutableLiveData<List<SeriesNotifier>> notifiers = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_NOTIFIERS_COLLECTION_PATH)
                .whereEqualTo(NOTIFIER_ID_KEY, mediaId);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<SeriesNotifier> mediaNotifiers = new ArrayList<>();

                //TODO: make message when error occurs
                if (e != null) return;
                if (snapshots == null) return;

                if (snapshots.getDocuments().size() != 0) {
                    List<DocumentSnapshot> notifierDocuments = snapshots.getDocuments();

                    for (DocumentSnapshot documentSnapshot: notifierDocuments) {
                        mediaNotifiers.add(parseMediaNotifier(documentSnapshot));
                    }
                }

                //set notifiers
                notifiers.setValue(mediaNotifiers);
            }
        });

        return notifiers;
    }

    @Override
    public List<SeriesNotifier> getNotifiersWithMediaIdAlt(String mediaId) {
        List<SeriesNotifier> notifiers = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_NOTIFIERS_COLLECTION_PATH)
                .whereEqualTo(NOTIFIER_ID_KEY, mediaId)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                SeriesNotifier notifier = parseMediaNotifier(documentSnapshot);
                notifiers.add(notifier);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return notifiers;
    }

    @Override
    public void deleteNotifier(SeriesNotifier notifier) {
        Task<Void> task = MasterActivity.getUserDbHomeReference()
                .collection(SERIES_NOTIFIERS_COLLECTION_PATH)
                .document(notifier.getUniqueExternalId())
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
    private static SeriesNotifier parseMediaNotifier(DocumentSnapshot document) {
        SeriesNotifier mediaNotifier = new SeriesNotifier(
                (String) document.get(NOTIFIER_ID_KEY),
                ((Long) document.get(CONDITION_KEY)).intValue()
        );

        mediaNotifier.setUniqueExternalId(document.getId());
        return mediaNotifier;
    }
}
