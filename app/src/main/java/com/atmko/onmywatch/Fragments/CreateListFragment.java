package com.atmko.onmywatch.Fragments;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.SeriesDataRecord;
import com.google.android.material.snackbar.Snackbar;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;

import java.util.List;

public class CreateListFragment extends Fragment {
    public static String FRAGMENT_KEY = "create_list_fragment";

    public static final String MODE_KEY = "mode";
    public static final String LIST_NAME_KEY = "list_name";
    public static final String ITEM_COUNT_KEY = "item_count";

    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT = 1;

    private int mMode, mItemCount;
    private String mListName;
    private OnSavePressedActionListener mSaveActionListener;

    private EditText nameEditTextView;
    private Button mSaveButton;


    public CreateListFragment() {
        // Required empty public constructor
    }

    public interface OnSavePressedActionListener {
        void onSavePressed();
    }

    public static CreateListFragment newInstance(int mode, String listName, int itemCount) {
        CreateListFragment fragment = new CreateListFragment();
        Bundle args = new Bundle();
        args.putInt(MODE_KEY, mode);
        args.putString(LIST_NAME_KEY, listName);
        args.putInt(ITEM_COUNT_KEY, itemCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMode = getArguments().getInt(MODE_KEY);
            mListName = getArguments().getString(LIST_NAME_KEY);
            mItemCount = getArguments().getInt(ITEM_COUNT_KEY);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_list, container, false);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(LIST_NAME_KEY, nameEditTextView.getText().toString());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();
        setViewValues(savedInstanceState);
    }

    private void defineViews() {
        nameEditTextView = getView().findViewById(R.id.name_edit_text_view);

        mSaveButton = getView().findViewById(R.id.save_button);
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

                            final AppDatabase appDatabase = AppDatabase.getInstance(getContext());
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

                            Snackbar.make(getActivity().findViewById(R.id.top_layout),
                                    snackBarMessage, Snackbar.LENGTH_LONG).show();

                            //exit fragment
                            mSaveActionListener.onSavePressed();

                        } catch (SQLiteConstraintException e) {
                            e.printStackTrace();

                            Snackbar.make(getActivity().findViewById(R.id.top_layout),
                                    getString(R.string.list_already_exists_error_message),
                                    Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    private void setViewValues(Bundle savedInstanceState) {
        mListName =
                savedInstanceState == null ? mListName : savedInstanceState.getString(LIST_NAME_KEY);

        nameEditTextView.setText(mListName);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnSavePressedActionListener) {
            mSaveActionListener = (OnSavePressedActionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnSavePressedAction");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mSaveActionListener = null;
    }
}
