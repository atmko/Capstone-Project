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
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.SearchListTag;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.google.android.material.snackbar.Snackbar;

import org.parceler.Parcels;

import java.util.List;

public class CreateListActivity extends AppCompatActivity {
    public static String FRAGMENT_KEY = "create_list_fragment";

    public static final String MODE_KEY = "mode";
    public static final String USER_LIST_KEY = "user_list";

    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT = 1;

    private int mMode;
    private UserListModel mEditListModel;

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
        if (mMode == MODE_EDIT) {
            mEditListModel = Parcels.unwrap(intent.getParcelableExtra(USER_LIST_KEY));
        }

        defineViews();
        setViewValues(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(USER_LIST_KEY, Parcels.wrap(mEditListModel));
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

                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            final AppDatabase appDatabase = AppDatabase.getInstance(CreateListActivity.this);
                            String snackBarMessage;

                            if (mMode == MODE_EDIT) {
                                UserListModel newUserListModel = new UserListModel(
                                        nameEditTextView.getText().toString(), mEditListModel.getItemCount());

                                //TODO: remove list name from being a primary key
                                // this ensures list and list records are easily updated instead of creating new records and deleting old ones.
                                // this will save quota in firebase implementations
                                //can't update list directly because name is changing and table id == list name
                                //create new updated list
                                appDatabase.userListsDao().addList(newUserListModel);
                                //create tag
                                AppDatabase.getLocalDatabase(CreateListActivity.this)
                                        .searchListTagsDao().addTag(new SearchListTag(newUserListModel.getName()));

                                //can't update records directly because name is changing and table id == list name
                                List<MovieDataRecord> movieRecords =
                                        appDatabase.movieDataRecordsDao().getAllRecordsOfListAlt(mEditListModel.getName());

                                for (MovieDataRecord dataRecord: movieRecords) {
                                    //create new updated record
                                    MovieDataRecord newDataRecord =
                                            new MovieDataRecord(dataRecord.getId(),
                                                    nameEditTextView.getText().toString());

                                    appDatabase.movieDataRecordsDao().addRecord(newDataRecord);
                                }

                                List<SeriesDataRecord> seriesRecord =
                                        appDatabase.seriesDataRecordsDao().getAllRecordsOfListAlt(mEditListModel.getName());

                                for (SeriesDataRecord dataRecord: seriesRecord) {
                                    //create new updated record
                                    SeriesDataRecord newDataRecord =
                                            new SeriesDataRecord(dataRecord.getId(),
                                                    nameEditTextView.getText().toString());

                                    appDatabase.seriesDataRecordsDao().addRecord(newDataRecord);
                                }

                                //delete old list from database
                                //old record cascades on delete when list is deleted
                                appDatabase.userListsDao().deleteList(mEditListModel);

                                //delete list tag
                                AppDatabase localDb = AppDatabase.getLocalDatabase(CreateListActivity.this);
                                SearchListTag tag = localDb.searchListTagsDao()
                                        .getTagAlt(mEditListModel.getName().toLowerCase());
                                if (tag != null) {
                                    localDb.searchListTagsDao().deleteTag(tag);
                                }

                                snackBarMessage = getString(R.string.list_updated_message);

                            } else {
                                UserListModel userListModel =
                                        new UserListModel(nameEditTextView.getText().toString(), 0);
                                //create new list
                                appDatabase.userListsDao().addList(userListModel);
                                //create tag
                                AppDatabase.getLocalDatabase(CreateListActivity.this)
                                        .searchListTagsDao().addTag(new SearchListTag(userListModel.getName()));
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
        });
    }

    private void setViewValues(Bundle savedInstanceState) {
        String editTextString =
                savedInstanceState == null ? "" : savedInstanceState.getString(USER_LIST_KEY);

        nameEditTextView.setText(editTextString);
    }
}
