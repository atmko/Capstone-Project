package com.atmko.onmywatch.Fragments;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;

public class CreateListFragment extends Fragment {
    public static String FRAGMENT_KEY = "create_list_fragment";

    private OnSavePressedActionListener mSaveActionListener;

    private EditText nameEditTextView;
    private Button mSaveButton;


    public CreateListFragment() {
        // Required empty public constructor
    }

    public interface OnSavePressedActionListener {
        void onSavePressed();
    }

    public static CreateListFragment newInstance() {
        CreateListFragment fragment = new CreateListFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_create_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();
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

                final UserListModel newUserListModel =
                        new UserListModel(
                                nameEditTextView.getText().toString(), 0);

                final AppDatabase appDatabase = AppDatabase.getInstance(getContext());

                //add list to database
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            appDatabase.userListsDao().addList(newUserListModel);

                            Snackbar.make(getActivity().findViewById(R.id.top_layout),
                                    getString(R.string.new_list_created_message), Snackbar.LENGTH_LONG).show();

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
