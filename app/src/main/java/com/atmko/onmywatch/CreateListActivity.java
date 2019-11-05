/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.database.daos.FirebaseUserListDao;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.atmko.onmywatch.models.ListModel.DOCUMENT_ID_KEY;
import static com.atmko.onmywatch.models.ListModel.ITEM_COUNT_KEY;
import static com.atmko.onmywatch.models.ListModel.LIST_NAME_KEY;

public class CreateListActivity extends AppCompatActivity {
    public static String FRAGMENT_KEY = "create_list_fragment";

    public static final String MODE_KEY = "mode";

    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT = 1;

    private int mMode, mItemCount;
    private String mListId, mListName;

    private EditText nameEditTextView;
    private Button mSaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_list);

        //configure percentage of display dialog activity takes
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int height = displayMetrics.heightPixels *
                getResources().getInteger(R.integer.create_list_activity_popup_screen_percent) / 100;

        int width = displayMetrics.widthPixels *
                getResources().getInteger(R.integer.create_list_activity_popup_screen_percent) / 100;

        getWindow().setLayout(width, height);

        Intent intent = getIntent();
        mMode = intent.getIntExtra(MODE_KEY, 0);
        mListId = intent.getStringExtra(DOCUMENT_ID_KEY);
        mListName = intent.getStringExtra(LIST_NAME_KEY);
        mItemCount = intent.getIntExtra(ITEM_COUNT_KEY, 0);

        defineViews();
        setViewValues(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(LIST_NAME_KEY, nameEditTextView.getText().toString());
    }

    private void defineViews() {
        nameEditTextView = findViewById(R.id.name_edit_text_view);

        mSaveButton = findViewById(R.id.save_button);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nameEditTextView.getText().toString().equals("")) {
                    return;
                }

                //add list to database
                if (MasterActivity.isProMode()) {
                    // Create a new list map to save to firebase
                    Map<String, Object> list = new HashMap<>();

                    list.put(LIST_NAME_KEY, nameEditTextView.getText().toString());
                    list.put(ITEM_COUNT_KEY, mItemCount);

                    String snackBarMessage;

                    if (mMode == MODE_EDIT) {
                        //edit list
                        FirebaseUserListDao.updateUserList(mListId, list)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        //TODO:

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //TODO:

                                    }
                                });

                        snackBarMessage = getString(R.string.list_updated_message);

                    } else {
                        //create new list
                        FirebaseUserListDao.addUserList(list)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        //TODO:

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        //TODO:

                                    }
                                });

                        snackBarMessage = getString(R.string.new_list_created_message);
                    }

                    Snackbar.make(findViewById(R.id.top_layout),
                            snackBarMessage, Snackbar.LENGTH_LONG).show();

                    //exit activity
                    finish();

                } else {
                    AppExecutors.getInstance().diskIO().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                UserListModel newUserListModel = new UserListModel(
                                        nameEditTextView.getText().toString(), mItemCount);

                                final AppDatabase appDatabase =
                                        AppDatabase.getInstance(CreateListActivity.this);
                                String snackBarMessage;
                                if (mMode == MODE_EDIT) {
                                    //can't update list directly because name is changing and table id == list name
                                    //create new updated list
                                    appDatabase.userListsDao().addList(newUserListModel);

                                    //can't update records directly because name is changing and table id == list name
                                    List<MovieDataRecord> movieRecords
                                            = appDatabase.movieDataRecordsDao().getAllRecordsOfList(mListName);

                                    for (MovieDataRecord dataRecord: movieRecords) {
                                        //create new updated record
                                        MovieDataRecord newDataRecord =
                                                new MovieDataRecord(dataRecord.getId(),
                                                        nameEditTextView.getText().toString());

                                        appDatabase.movieDataRecordsDao().addRecord(newDataRecord);

                                    }

                                    List<SeriesDataRecord> seriesRecord
                                            = appDatabase.seriesDataRecordsDao().getAllRecordsOfList(mListName);

                                    for (SeriesDataRecord dataRecord: seriesRecord) {
                                        //create new updated record
                                        SeriesDataRecord newDataRecord =
                                                new SeriesDataRecord(dataRecord.getId(),
                                                        nameEditTextView.getText().toString());

                                        appDatabase.seriesDataRecordsDao().addRecord(newDataRecord);

                                    }

                                    //delete old list from database
                                    //old record cascades on delete when list is deleted
                                    appDatabase.userListsDao().deleteList(new UserListModel(mListName));

                                    snackBarMessage = getString(R.string.list_updated_message);

                                } else {
                                    //create new list
                                    appDatabase.userListsDao().addList(newUserListModel);
                                    snackBarMessage = getString(R.string.new_list_created_message);
                                }

                                Snackbar.make(findViewById(R.id.top_layout),
                                        snackBarMessage, Snackbar.LENGTH_LONG).show();

                                //exit activity
                                finish();

                            } catch (SQLiteConstraintException e) {
                                e.printStackTrace();

                                Snackbar.make(findViewById(R.id.top_layout),
                                        getString(R.string.list_already_exists_error_message),
                                        Snackbar.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        });
    }

    private void setViewValues(Bundle savedInstanceState) {
        String editTextString =
                savedInstanceState == null ? mListName : savedInstanceState.getString(LIST_NAME_KEY);

        nameEditTextView.setText(editTextString);
    }
}
