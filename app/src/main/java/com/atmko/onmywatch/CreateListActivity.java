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
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class CreateListActivity extends AppCompatActivity {
    public static String FRAGMENT_KEY = "create_list_fragment";

    public static final String MODE_KEY = "mode";
    public static final String LIST_NAME_KEY = "list_name";
    public static final String ITEM_COUNT_KEY = "item_count";

    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT = 1;

    private int mMode, mItemCount;
    private String mListName;
    private CreateListFragment.OnSavePressedActionListener mSaveActionListener;

    private EditText nameEditTextView;
    private Button mSaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_list);

        boolean isPhone = getResources().getBoolean(R.bool.isPhone);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

        int height;
        int width;

        if (isPhone) {
            height = displayMetrics.heightPixels * 80/100;
            width = displayMetrics.widthPixels * 80/100;

        } else {
            height = displayMetrics.heightPixels * 50/100;
            width = displayMetrics.widthPixels * 50/100;
        }

        getWindow().setLayout(width, height);

        Intent intent = getIntent();
        mMode = intent.getIntExtra(MODE_KEY, 0);
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
        });
    }

    private void setViewValues(Bundle savedInstanceState) {
        String editTextString =
                savedInstanceState == null ? mListName : savedInstanceState.getString(LIST_NAME_KEY);

        nameEditTextView.setText(editTextString);
    }
}
