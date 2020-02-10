/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.models.MovieNotifier;
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
import static com.atmko.onmywatch.models.MediaNotifier.CONDITION_KEY;
import static com.atmko.onmywatch.models.MediaNotifier.IS_ACTIVE_KEY;
import static com.atmko.onmywatch.models.MediaNotifier.NOTIFIER_ID_KEY;

/*
 * MovieData firebase Dao
 */

public class FirebaseMovieNotifiersDao implements MovieNotifierDao {

    private static final String MOVIE_NOTIFIERS_COLLECTION_PATH = "movie_notifiers";

    @Override
    public void addMediaNotifier(MovieNotifier movieNotifier) {
        DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_NOTIFIERS_COLLECTION_PATH)
                .document();

        movieNotifier.setUniqueExternalId(documentReference.getId());

        documentReference.set(movieNotifier.parseNotifierToDataMap())
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

    public static void addMovieNotifierBatch(List<Map<String, Object>> notifierMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> movieDataMap: notifierMapList) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(MOVIE_NOTIFIERS_COLLECTION_PATH)
                    .document();

            batch.set(documentReference, movieDataMap);
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
    public List<MovieNotifier> getAllNotifiersAlt() {
        List<MovieNotifier> notifiers = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_NOTIFIERS_COLLECTION_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                MovieNotifier notifier = parseMediaNotifier(documentSnapshot);
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
    public List<MovieNotifier> getActiveNotifiersAlt() {
        List<MovieNotifier> notifiers = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_NOTIFIERS_COLLECTION_PATH)
                .whereEqualTo(IS_ACTIVE_KEY, true)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                MovieNotifier notifier = parseMediaNotifier(documentSnapshot);
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
    public MovieNotifier getNotifierByIdAlt(String mediaId, int condition) {
        MovieNotifier notifier = null;

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_NOTIFIERS_COLLECTION_PATH)
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
    public LiveData<List<MovieNotifier>> getNotifiersWithMediaId(String mediaId) {
        final MutableLiveData<List<MovieNotifier>> notifiers = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_NOTIFIERS_COLLECTION_PATH)
                .whereEqualTo(NOTIFIER_ID_KEY, mediaId);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MovieNotifier> mediaNotifiers = new ArrayList<>();

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
    public List<MovieNotifier> getNotifiersWithMediaIdAlt(String mediaId) {
        List<MovieNotifier> notifiers = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_NOTIFIERS_COLLECTION_PATH)
                .whereEqualTo(NOTIFIER_ID_KEY, mediaId)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                MovieNotifier notifier = parseMediaNotifier(documentSnapshot);
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
    public void deleteNotifier(MovieNotifier notifier) {
        MasterActivity.getUserDbHomeReference()
                .collection(MOVIE_NOTIFIERS_COLLECTION_PATH)
                .document(notifier.getUniqueExternalId())
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
    private static MovieNotifier parseMediaNotifier(DocumentSnapshot document) {
        MovieNotifier mediaNotifier = new MovieNotifier(
                (String) document.get(NOTIFIER_ID_KEY),
                ((Long) document.get(CONDITION_KEY)).intValue(),
                ((boolean) document.get(IS_ACTIVE_KEY))
        );

        mediaNotifier.setUniqueExternalId(document.getId());
        return mediaNotifier;
    }
}
