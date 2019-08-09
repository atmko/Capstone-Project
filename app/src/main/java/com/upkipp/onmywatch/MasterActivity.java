package com.upkipp.onmywatch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.net.Uri;
import android.os.Bundle;

import com.upkipp.onmywatch.Fragments.AddToListFragment;
import com.upkipp.onmywatch.Fragments.CreateListFragment;
import com.upkipp.onmywatch.Fragments.HomeFragment;
import com.upkipp.onmywatch.Fragments.ListsParentFragment;
import com.upkipp.onmywatch.Fragments.SeasonsFragment;

public class MasterActivity extends AppCompatActivity implements
//        HomeFragment.OnListButtonClickListener,
        ListsParentFragment.OnFragmentInteractionListener,
//        SearchFragment.OnFragmentInteractionListener,
        SeasonsFragment.OnFragmentInteractionListener,
        AddToListFragment.OnSavePressedActionListener,
        CreateListFragment.OnSavePressedActionListener {

    private static final String DEFAULT_MEDIA_KEY = "default_media";

    private static final String ADD_TO_LIST_VISIBILITY_KEY = "add_to_list_visibility";

    public static final int MEDIA_TYPE_SERIES = 0;
    public static final int MEDIA_TYPE_MOVIE = 1;
    public static final int MEDIA_TYPE_PEOPLE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        if (savedInstanceState == null) {
            startHomeFragment();

        } else {
            int addToListVisibility = savedInstanceState.getInt(ADD_TO_LIST_VISIBILITY_KEY);
            findViewById(R.id.popup_container).setVisibility(addToListVisibility);
        }
    }

    private void startHomeFragment() {
                HomeFragment homeFragment = HomeFragment.newInstance();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.master_fragments_container, homeFragment, HomeFragment.FRAGMENT_KEY)
                .commit();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(ADD_TO_LIST_VISIBILITY_KEY,
                findViewById(R.id.popup_container).getVisibility());
    }

    @Override
    public void onBackPressed() {
        Fragment popupFragment = getSupportFragmentManager().findFragmentById(R.id.popup_container);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.master_fragments_container);

        if (popupFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(popupFragment).commit();

        } else {
            if (fragment !=  null) {
                getSupportFragmentManager().beginTransaction().remove(fragment).commit();

            } else {
                super.onBackPressed();

            }
        }
    }

    @Override
    public void onSavePressed() {
        onBackPressed();
    }
}
