package com.upkipp.onmywatch.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.database.AppDatabase;
import com.upkipp.onmywatch.models.UserListModel;
import com.upkipp.onmywatch.utils.network_utils.AppExecutors;

public class CreateListFragment extends Fragment {
    public static String FRAGMENT_KEY = "create_list_fragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ID_KEY = "id";

    // TODO: Rename and change types of parameters
    private String mId;

    private OnSavePressedActionListener mSaveActionListener;

    private EditText nameEditTextView;
    private Button mCancelButton;
    private Button mSaveButton;


    public CreateListFragment() {
        // Required empty public constructor
    }

    public interface OnSavePressedActionListener {
        void onSavePressed();
    }

    // TODO: Rename and change types and number of parameters
    public static CreateListFragment newInstance() {
        CreateListFragment fragment = new CreateListFragment();
//        Bundle args = new Bundle();
//        args.putInt(MEDIA_TYPE_KEY, mediaType);
//        args.putString(ID_KEY, id);
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mId = getArguments().getString(ID_KEY);
        }
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
        mCancelButton =  getView().findViewById(R.id.cancel_button);

        //TODO
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        mSaveButton = getView().findViewById(R.id.save_button);

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//
                final UserListModel newUserListModel =
                        new UserListModel(
                                nameEditTextView.getText().toString(), 0);

                final AppDatabase appDatabase = AppDatabase.getInstance(getContext());

                //add list to database
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        appDatabase.userListsDao().addList(newUserListModel);
                    }
                });

                Snackbar.make(getView().findViewById(R.id.top_layout),
                        "new list created", Snackbar.LENGTH_LONG).show();

                //exit fragment
                mSaveActionListener.onSavePressed();
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
