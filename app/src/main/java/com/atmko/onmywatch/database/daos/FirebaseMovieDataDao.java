/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.database.daos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.atmko.onmywatch.MasterActivity;
import com.atmko.onmywatch.database.Converters;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.ScheduledMedia;
import com.atmko.onmywatch.models.SearchMediaTag;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.MovieApiConstants;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.atmko.onmywatch.database.FirebaseDatabase.getFirstDocument;
import static com.atmko.onmywatch.models.MediaData.TAGS_KEY;
import static com.atmko.onmywatch.models.MediaData.WATCH_STATUS_KEY;
import static com.atmko.onmywatch.models.MovieData.SCHEDULED_MEDIA_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.ID_KEY;
import static com.atmko.onmywatch.utils.api_utils.ApiConstants.RELEASE_STATUS_KEY;

/*
 * MovieData firebase Dao
 */

public class FirebaseMovieDataDao implements MovieDataDao {
    public static final String MOVIES_COLLECTION_PATH = "movies";

    @Override
    public void addMovieData(MovieData movieData) {
        DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .document();

        movieData.setUniqueExternalId(documentReference.getId());

        documentReference.set(movieData.parseMediaDataToDataMap())
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

    //TODO: remove update code from pro migrations since set method can handle create and updates
    public static void addMovieDataBatch(List<Map<String, Object>> movieDataMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (Map<String, Object> movieDataMap: movieDataMapList) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(MOVIES_COLLECTION_PATH)
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
    public List<MovieData> getAllMoviesAlt() {
        List<MovieData> movieList = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                MovieData mediaData = parseDataMapToMediaData(documentSnapshot);
                movieList.add(mediaData);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return movieList;
    }

    @Override
    public List<MovieData> getAllMediaWithTagAlt(String tag) {
        List<MovieData> movieList = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereArrayContains(TAGS_KEY, tag)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                MovieData mediaData = parseDataMapToMediaData(documentSnapshot);
                movieList.add(mediaData);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return movieList;
    }

    @Override
    public LiveData<List<MovieData>> getAllMediaWithWatchStatusAndTags(int watchStatus, String tag1,
                                                                       String tag2, String tag3,
                                                                       String tag4, String tag5,
                                                                       String tag6, String tag7) {
        //remove empty tags
        final ArrayList<String> tagList = new ArrayList<>();
        for (String tag: Arrays.asList(tag1, tag2, tag3, tag4, tag5, tag6, tag7)) {
            if (!tag.equals("")) {
                tagList.add(tag);
            }
        }

        final MutableLiveData<List<MovieData>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereEqualTo(WATCH_STATUS_KEY, watchStatus);

        //query if there are tags requested
        if (tagList.size() != 0) query = query.whereArrayContainsAny(TAGS_KEY, tagList);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MovieData> movieList = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        //TODO: document.get(TAGS_KEY) always produces a string list
                        @SuppressWarnings("unchecked")
                        ArrayList<String> mediaTags = ((ArrayList<String>) document.get(TAGS_KEY));

                        if (mediaTags == null) continue;

                        mediaTags.retainAll(tagList);

                        if (mediaTags.size() == tagList.size()) {
                            MovieData mediaData = parseDataMapToMediaData(document);
                            movieList.add(mediaData);
                        }
                    }

                    liveData.setValue(movieList);

                } else {
                    liveData.setValue(movieList);
                }
            }
        });

        return liveData;
    }

    @Override
    public LiveData<MovieData> getMovieById(String mediaId) {
        final MutableLiveData<MovieData> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereEqualTo(ID_KEY, mediaId);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                MovieData mediaData = null;

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
    public MovieData getMovieByIdAlt(String mediaId) {
        MovieData mediaData = null;

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
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
    public LiveData<Integer> getMoviesWatchStatus(String mediaId) {
        final MutableLiveData<Integer> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
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
    public LiveData<List<MovieData>> getMoviesByWatchStatus(int watchStatus) {
        final MutableLiveData<List<MovieData>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereEqualTo(WATCH_STATUS_KEY, watchStatus);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MovieData> movieList = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        MovieData mediaData = parseDataMapToMediaData(document);

                        movieList.add(mediaData);
                    }

                    liveData.setValue(movieList);

                } else {
                    liveData.setValue(movieList);
                }
            }
        });

        return liveData;
    }

    @Override
    public List<MovieData> getMoviesByWatchStatusAlt(int watchStatus) {
        List<MovieData> movieList = new ArrayList<>();

        Task<QuerySnapshot> task = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereEqualTo(WATCH_STATUS_KEY, watchStatus)
                .get();

        try {
            QuerySnapshot snapshots = Tasks.await(task);
            for (DocumentSnapshot documentSnapshot: snapshots.getDocuments()) {
                MovieData mediaData = parseDataMapToMediaData(documentSnapshot);
                movieList.add(mediaData);
            }

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return movieList;
    }

    @Override
    public LiveData<List<MovieData>> getUserUpcomingMovies() {
        final MutableLiveData<List<MovieData>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereGreaterThan(WATCH_STATUS_KEY, 0)
                .whereLessThan(WATCH_STATUS_KEY, 3)
                .whereIn(RELEASE_STATUS_KEY, Arrays.asList("Rumored", "Planned", "In Production", "Post Production"))
                .orderBy(WATCH_STATUS_KEY, Query.Direction.DESCENDING)
                .orderBy(SCHEDULED_MEDIA_KEY, Query.Direction.ASCENDING)
                .limit(10);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MovieData> movieList = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        MovieData mediaData = parseDataMapToMediaData(document);

                        movieList.add(mediaData);
                    }
                }
//                TODO: remove extraneous if statement from other live data gets
                liveData.setValue(movieList);
            }
        });

        return liveData;
    }

    @Override
    public LiveData<List<MovieData>> getReleasedMovies() {
        final MutableLiveData<List<MovieData>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereGreaterThan(WATCH_STATUS_KEY, 0)
                .whereLessThan(WATCH_STATUS_KEY, 3)
                .whereEqualTo(RELEASE_STATUS_KEY, "Released")
                .limit(10);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MovieData> movieList = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        MovieData mediaData = parseDataMapToMediaData(document);

                        movieList.add(mediaData);
                    }

                    liveData.setValue(movieList);

                } else {
                    liveData.setValue(movieList);
                }
            }
        });

        return liveData;
    }

    @Override
    public LiveData<List<MovieData>> getUndatedMovies() {
        final MutableLiveData<List<MovieData>> liveData = new MutableLiveData<>();

        Query query = MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .whereGreaterThan(WATCH_STATUS_KEY, 0)
                .whereLessThan(WATCH_STATUS_KEY, 3)
                .whereEqualTo(SCHEDULED_MEDIA_KEY, 0)
                .whereIn(RELEASE_STATUS_KEY, Arrays.asList("Rumored", "Planned", "In Production", "Post Production"))
                .limit(10);

        query.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                final List<MovieData> movieList = new ArrayList<>();

                if (snapshots != null) {
                    List<DocumentSnapshot> documents = snapshots.getDocuments();

                    for (DocumentSnapshot document: documents) {
                        if (document.getData() == null) continue;

                        MovieData mediaData = parseDataMapToMediaData(document);

                        movieList.add(mediaData);
                    }

                    liveData.setValue(movieList);

                } else {
                    liveData.setValue(movieList);
                }
            }
        });

        return liveData;
    }

    @Override
    public void updateMovieData(MovieData movieData) {
        MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .document(movieData.getUniqueExternalId())
                .update(movieData.parseMediaDataToDataMap())
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

    public static void updateMovieDataBatch(List<String> batchDocumentIds, List<Map<String,
            Object>> movieDataMapList) {
        final WriteBatch batch = FirebaseFirestore.getInstance().batch();

        for (int i = 0; i < batchDocumentIds.size(); i++) {
            DocumentReference documentReference = MasterActivity.getUserDbHomeReference()
                    .collection(MOVIES_COLLECTION_PATH)
                    .document(batchDocumentIds.get(i));

            batch.update(documentReference, movieDataMapList.get(i));
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
    public void deleteMovieData(MovieData movieData) {
        MasterActivity.getUserDbHomeReference()
                .collection(MOVIES_COLLECTION_PATH)
                .document(movieData.getUniqueExternalId())
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

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    static MovieData parseDataMapToMediaData(DocumentSnapshot document) {
        ScheduledMedia scheduledMedia = document.get(SCHEDULED_MEDIA_KEY) == null ? null
                : Converters.longToScheduledMedia((long) document.get(SCHEDULED_MEDIA_KEY));

        List<String> tagStrings = (ArrayList<String>) document.get(TAGS_KEY);
        List<SearchMediaTag> searchTags = new ArrayList<>();
        for (String tagString: tagStrings) {
            searchTags.add(new SearchMediaTag(tagString));
        }

        MovieData movieData = new MovieData(
                (String) document.get(ApiConstants.ID_KEY),
                ((String) document.get(TraktApiConstants.TRAKT_ID_KEY)),
                (String) document.get(ApiConstants.VOTE_AVERAGE_KEY),
                (String) document.get(MovieApiConstants.TITLE_KEY),
                (String) document.get(ApiConstants.POSTER_PATH_KEY),
                (String) document.get(ApiConstants.ORIG_LANG_KEY),
                (String) document.get(MovieApiConstants.ORIG_TITLE_KEY),
                (ArrayList<String>) document.get(ApiConstants.GENRES_KEY),
                (boolean) document.get(MovieApiConstants.ADULT_KEY),
                (String) document.get(ApiConstants.BACKDROP_PATH_KEY),
                (String) document.get(ApiConstants.OVERVIEW_KEY),
                (String) document.get(MovieApiConstants.RELEASE_DATE_KEY),
                (String) document.get(MovieApiConstants.CERTIFICATION_KEY),
                (String) document.get(ApiConstants.RELEASE_STATUS_KEY),
                scheduledMedia,
                searchTags
        );

        movieData.setWatchStatus(
                ((Long) document.get(MediaData.WATCH_STATUS_KEY)).intValue());
        movieData.setUserRating(
                ((Long) document.get(MediaData.USER_RATING_KEY)).intValue());

        movieData.setUniqueExternalId(document.getId());

        return movieData;
    }
}