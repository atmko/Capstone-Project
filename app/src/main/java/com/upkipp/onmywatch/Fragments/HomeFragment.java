package com.upkipp.onmywatch.Fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.upkipp.onmywatch.R;
import com.upkipp.onmywatch.utils.SearchPreferences;

import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_MOVIE;
import static com.upkipp.onmywatch.MasterActivity.MEDIA_TYPE_SERIES;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CastFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CastFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    public static final String FRAGMENT_KEY = "home_fragment";

    public static final String MEDIA_TYPE_KEY = "media_type";

    private int mMediaType;

//    private OnListButtonClickListener mListButtonClickListener;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * @param defaultMedia Parameter 1.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    //TODO @param defaultMedia doesnt need to be passed and can be retrieved within the fragment
    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
//        Bundle args = new Bundle();
//        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) { ;
//        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        defineViews();

        //TODO replace MEDIA_TYPE_SERIES with default media shared preference
        if (savedInstanceState == null) {
            mMediaType = MEDIA_TYPE_SERIES;
            loadHomeScreen();

            //TODO replace MEDIA_TYPE_SERIES with default media shared preference
        } else {
            mMediaType = savedInstanceState.getInt(MEDIA_TYPE_KEY, MEDIA_TYPE_SERIES);
            loadMediaLabel();
        }

    }

    private void defineViews() {
        final TextView mediaTypeTextView = getView().findViewById(R.id.media_type_text_view);

        mediaTypeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaType == (MEDIA_TYPE_SERIES)) {
                    mMediaType = MEDIA_TYPE_MOVIE;

                } else if (mMediaType == (MEDIA_TYPE_MOVIE)) {
                    mMediaType = MEDIA_TYPE_SERIES;
                }

                loadHomeScreen();
            }
        });

        TextView listsTextView = getView().findViewById(R.id.lists_text_view);
        listsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListsParentFragment listsParentFragment = ListsParentFragment.newInstance();

                getActivity().getSupportFragmentManager().beginTransaction()
//                        .addToBackStack(FRAGMENT_KEY)
                        .add(R.id.master_fragments_container, listsParentFragment, ListsParentFragment.FRAGMENT_KEY)
                        .commit();
            }
        });

        ImageButton searchImageButton = getView().findViewById(R.id.search_image_button);
        searchImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SearchParentFragment searchParentFragment = SearchParentFragment.newInstance();

                getActivity().getSupportFragmentManager().beginTransaction()
//                        .addToBackStack(FRAGMENT_KEY)
                        .add(R.id.master_fragments_container, searchParentFragment, SearchParentFragment.FRAGMENT_KEY)
                        .commit();
            }
        });
    }

    private void loadMediaLabel() {
        final TextView mediaTypeTextView = getView().findViewById(R.id.media_type_text_view);

        if (mMediaType == MEDIA_TYPE_SERIES) {
            mediaTypeTextView.setText("Series");

        } else if (mMediaType == MEDIA_TYPE_MOVIE) {
            mediaTypeTextView.setText("Movies");

        }
    }

    private void loadHomeScreen() {
        loadMediaLabel();

        SearchPreferences searchPreferences =  new SearchPreferences();

        if (mMediaType == MEDIA_TYPE_MOVIE) {
            String spotlightUrl =
                    getContext().getResources().getStringArray(R.array.spotlight_url)[0];

            SearchResultsFragment spotLightHomeList = SearchResultsFragment.newInstance(mMediaType, spotlightUrl, searchPreferences);
            HomeListDisplayFragment watchingHomeList = HomeListDisplayFragment.newInstance(mMediaType, "watching");
            HomeListDisplayFragment toWatchHomeList = HomeListDisplayFragment.newInstance(mMediaType, "to watch");

            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.spotlight_container, spotLightHomeList, SearchResultsFragment.FRAGMENT_KEY)
                    .replace(R.id.watching_list_container, watchingHomeList, HomeListDisplayFragment.FRAGMENT_KEY)
                    .replace(R.id.to_watch_list_container, toWatchHomeList, HomeListDisplayFragment.FRAGMENT_KEY)
                    .commit();

        } else if (mMediaType == MEDIA_TYPE_SERIES) {
            String spotlightUrl =
                    getContext().getResources().getStringArray(R.array.spotlight_url)[1];

            SearchResultsFragment spotLightHomeList = SearchResultsFragment.newInstance(mMediaType, spotlightUrl, searchPreferences);
            HomeListDisplayFragment watchingHomeList = HomeListDisplayFragment.newInstance(mMediaType, "watching");
            HomeListDisplayFragment toWatchHomeList = HomeListDisplayFragment.newInstance(mMediaType, "to watch");

            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.spotlight_container, spotLightHomeList, SearchResultsFragment.FRAGMENT_KEY)
                    .replace(R.id.watching_list_container, watchingHomeList, HomeListDisplayFragment.FRAGMENT_KEY)
                    .replace(R.id.to_watch_list_container, toWatchHomeList, HomeListDisplayFragment.FRAGMENT_KEY)
                    .commit();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(MEDIA_TYPE_KEY, mMediaType);
    }

    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListButtonClickListener != null) {
//            mListButtonClickListener.onListButtonClick(uri);
//        }
//    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        if (context instanceof OnListButtonClickListener) {
//            mListButtonClickListener = (OnListButtonClickListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListButtonClickListener = null;
//    }

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
//    public interface OnListButtonClickListener {
//        // TODO: Update argument type and name
//        void onListButtonClick();
//    }
}
