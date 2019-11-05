/*
 * Copyright (C) 2019 Aayat Mimiko
 */

/*convenience class for adding data to firebase lists*/

package com.atmko.onmywatch;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.atmko.onmywatch.database.daos.FirebaseMovieDataDao;
import com.atmko.onmywatch.database.daos.FirebaseMovieDataRecordsDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataDao;
import com.atmko.onmywatch.database.daos.FirebaseSeriesDataRecordsDao;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.UserListModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.atmko.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

public class AddToListFirebaseHelper {
    private final static String TAG = AddToListFirebaseHelper.class.getSimpleName();

    static void saveFirebaseData(Activity activity, MediaData mediaData, int mediaType,
                                 int selectedWatchStatus, List<UserListModel> originalContainingLists,
                                 List<UserListModel> newContainingLists) {

        Task<Void> updateListRecordsTask;
        Task<Void> addMediaRecordTask;

        if (mediaType == MEDIA_TYPE_MOVIE) {
            checkIfMovieDataExists(mediaData, selectedWatchStatus, newContainingLists);

            addMediaRecordTask = FirebaseMovieDataRecordsDao.addMediaListRecords(
                    originalContainingLists, newContainingLists, mediaData.getId());

        } else {
            checkIfSeriesDataExists(mediaData, selectedWatchStatus, newContainingLists);

            addMediaRecordTask = FirebaseSeriesDataRecordsDao.addMediaListRecords(
                    originalContainingLists, newContainingLists, mediaData.getId());
        }

        addUserListRecords(addMediaRecordTask);
        deleteUserListRecords(activity, mediaType, mediaData.getId(),
                originalContainingLists,newContainingLists);
    }

    //checks if movie data exists, update if it does, create if it doesn't
    private static void checkIfMovieDataExists(final MediaData mediaData,
                                               final int selectedWatchStatus,
                                               final List<UserListModel> newContainingLists) {
        //check if movie exists in db
        FirebaseMovieDataDao.getMovieById(mediaData.getId()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        //if null
                        if (task.getResult() == null) return;

                        //if not successful
                        if (!task.isSuccessful()) {
                            Log.d(TAG, "failed to check if media data exists");
                            return;
                        }

                        Map<String, Object> movieDataMap;

                        //TODO: adding a check for value greater than one can help clean up duplicate database records
                        if (task.getResult().getDocuments().size() != 0) {
                            DocumentSnapshot movieDataDocument =
                                    task.getResult().getDocuments().get(0);

                            //update media data
                            movieDataMap = movieDataDocument.getData();
                            //TODO: mediaDataMap null check already done by getting by id and checking getDocuments != 0
                            //noinspection ConstantConditions
                            updateMedia(MasterActivity.MEDIA_TYPE_MOVIE,
                                    movieDataMap,
                                    movieDataDocument.getId(),
                                    selectedWatchStatus,
                                    newContainingLists);

                        } else {
                            //create new movie data
                            movieDataMap = ((MovieData) mediaData).parseMediaDataToDataMap();
                            createNewMedia(MEDIA_TYPE_MOVIE,
                                    movieDataMap,
                                    selectedWatchStatus,
                                    newContainingLists);
                        }
                    }
                });
    }

    //checks if series data exists, update if it does, create if it doesn't
    private static void checkIfSeriesDataExists(final MediaData mediaData,
                                                final int selectedWatchStatus,
                                                final List<UserListModel> newContainingLists) {
        //check if series exists in db
        FirebaseSeriesDataDao.getSeriesById(mediaData.getId()).get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        //if null
                        if (task.getResult() == null) return;

                        //if not successful
                        if (!task.isSuccessful()) {
                            Log.d(TAG, "failed to check if media data exists");
                            return;
                        }

                        Map<String, Object> seriesDataMap;

                        //TODO: adding a check for value greater than one can help clean up duplicate database records
                        if (task.getResult().getDocuments().size() != 0) {
                            DocumentSnapshot seriesDataDocument =
                                    task.getResult().getDocuments().get(0);

                            //update media data
                            seriesDataMap = seriesDataDocument.getData();
                            updateMedia(MasterActivity.MEDIA_TYPE_SERIES,
                                    seriesDataMap,
                                    seriesDataDocument.getId(),
                                    selectedWatchStatus,
                                    newContainingLists);

                        } else {
                            //create new series data
                            seriesDataMap = ((SeriesData) mediaData).parseMediaDataToDataMap();
                            createNewMedia(MEDIA_TYPE_SERIES,
                                    seriesDataMap,
                                    selectedWatchStatus,
                                    newContainingLists);
                        }
                    }
                });
    }

    //updates existing media data
    private static void updateMedia(final int mediaTpe, Map<String, Object> mediaDataMap,
                                    final String documentId,
                                    final int selectedWatchStatus,
                                    final List<UserListModel> newContainingLists) {

        //update watch status
        mediaDataMap.put(MediaData.WATCH_STATUS_KEY, selectedWatchStatus);

        Task<Void> mediaUpdateTask;

        if (mediaTpe == MEDIA_TYPE_MOVIE) {
            mediaUpdateTask = FirebaseMovieDataDao
                    .updateMovieData(documentId, mediaDataMap);

        } else {
            mediaUpdateTask = FirebaseSeriesDataDao
                    .updateSeriesData(documentId, mediaDataMap);

        }

        mediaUpdateTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "updated media data");

                firebaseDeleteMediaDataIfDataNotUsed(mediaTpe, documentId,
                        selectedWatchStatus, newContainingLists.size());
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "failed to update media data");
                    }
                });
    }

    private static void createNewMedia(final int mediaType, Map<String, Object> mediaDataMap,
                                       final int selectedWatchStatus,
                                       final List<UserListModel> newContainingLists) {

        //set watch status
        mediaDataMap.put(MediaData.WATCH_STATUS_KEY, selectedWatchStatus);

        Task<DocumentReference> createNewMediaTask;

        if (mediaType == MEDIA_TYPE_MOVIE) {
            createNewMediaTask = FirebaseMovieDataDao.addMovieData(mediaDataMap);

        } else {
            createNewMediaTask = FirebaseSeriesDataDao.addSeriesData(mediaDataMap);

        }

        createNewMediaTask.addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d(TAG, "created new media data");

                firebaseDeleteMediaDataIfDataNotUsed(mediaType, documentReference.getId(),
                        selectedWatchStatus, newContainingLists.size());
            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "failed to create new media data");
                    }
                });
    }

    private static void addUserListRecords(Task<Void> updateListRecordsTask) {
        updateListRecordsTask.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "batch updateUserListRecords successful");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "batch updateUserListRecords failed");

            }
        });
    }

    private static void deleteUserListRecords(final Activity activity,
                                              final int mediaType,
                                              final String mediaId,
                                              final List<UserListModel> originalContainingLists,
                                              final List<UserListModel> newContainingLists) {

        for (UserListModel userListModel : originalContainingLists) {
            if (!newContainingLists.contains(userListModel)) {
                Task<QuerySnapshot> getMediaRecordTask;

                if (mediaType == MEDIA_TYPE_MOVIE) {
                    getMediaRecordTask =
                            FirebaseMovieDataRecordsDao.getMediaListRecord(userListModel.getName(), mediaId);

                } else {
                    getMediaRecordTask =
                            FirebaseSeriesDataRecordsDao.getMediaListRecord(userListModel.getName(), mediaId);
                }

                getMediaRecordTask
                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                            @Override
                            public void onSuccess(QuerySnapshot snapshots) {
                                DocumentReference documentReference =
                                        snapshots.getDocuments().get(0).getReference();

                                Task<Void> deleteMediaRecordTask;

                                if (mediaType == MEDIA_TYPE_MOVIE) {
                                    deleteMediaRecordTask =
                                            FirebaseMovieDataRecordsDao
                                                    .deleteMediaListRecord(documentReference.getId());

                                } else {
                                    deleteMediaRecordTask =
                                            FirebaseSeriesDataRecordsDao
                                                    .deleteMediaListRecord(documentReference.getId());
                                }

                                deleteMediaRecordTask
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                //notify user of error
                                                Snackbar.make(activity.findViewById(R.id.top_layout),
                                                        activity.getString(R.string.list_delete_record_error_message),
                                                        Snackbar.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //notify user of error
                                Snackbar.make(activity.findViewById(R.id.top_layout),
                                        activity.getString(R.string.list_delete_record_error_message),
                                        Snackbar.LENGTH_LONG).show();
                            }
                        });
            }
        }
    }

    private static void firebaseDeleteMediaDataIfDataNotUsed(int mediaType, String documentId,
                                                             int newWatchStatus,
                                                             int newContainingListsSize) {

        //if watch status is none and if there are no lists containing this media

        Task<Void> deleteMediaTask;

        if (newWatchStatus == MediaData.WATCH_STATUS_NONE
                && newContainingListsSize == 0) {
            //delete the media from the database
            Log.d(TAG, "deleting empty media data");

            if (mediaType == MEDIA_TYPE_MOVIE) {
                deleteMediaTask = FirebaseMovieDataDao.deleteMovieData(documentId);

            } else {
                deleteMediaTask = FirebaseSeriesDataDao.deleteSeriesData(documentId);

            }

            deleteMediaTask.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "media data deleted");

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "failed to delete media data");

                }
            });
        }
    }
}
