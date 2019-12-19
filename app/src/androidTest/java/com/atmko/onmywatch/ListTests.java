/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiCollection;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.Episode;
import com.atmko.onmywatch.models.ListModel;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.NotificationIdlingResource;
import com.atmko.onmywatch.models.ScheduledMedia;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.GeneralUtils;
import com.atmko.onmywatch.utils.UpdateNotifierService;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.parceler.Parcels;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.atmko.onmywatch.utils.GeneralUtils.parseIsoDateFromCalender;
import static org.junit.Assert.fail;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ListTests {
    private IdlingResource mIdlingResource;
    Context context = ApplicationProvider.getApplicationContext();

    private AppDatabase db;

    @Rule
    public ActivityTestRule<MasterActivity> masterActivityTestRule =
            new ActivityTestRule<>(MasterActivity.class);
    @Rule
    public ActivityTestRule<AddToListActivity> addToListActivityTestRule =
            new ActivityTestRule<>(AddToListActivity.class, true, false);

    @Before
    public void setupTestDatabase() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .build();

        AppDatabase.setDatabase(db);
    }

    @Before
    public void populateDatabase() {
        String[] seriesWatchListTitles = context.getResources()
                .getStringArray(R.array.watch_status_series_titles);
        for (String title: seriesWatchListTitles) {
            WatchListModel watchListModel = new WatchListModel(title);
            AppDatabase.getInstance(context).watchListsDao()
                    .addList(watchListModel);
        }
    }

    @Before
    public void enableTestMode() {
        UpdateNotifierService.sActionMode = UpdateNotifierService.ACTION_TESTING;
    }

    @After
    public void unregisterIdlingResources() {
        IdlingRegistry.getInstance().unregister(mIdlingResource);
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void TestMakingNoChangesToMovieWithoutWatchStatus() {
        //create media data and set watch status
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //save
        onView(withText("SAVE")).perform(click());

        WatchListModel watchListModel = db.watchListsDao().getListByNameAlt("none");
        movieData = db.movieDataDao().getMovieByIdAlt(movieData.getId());

        if (watchListModel.getItemCount() != 0) fail();
        if (movieData != null) fail();
    }

    @Test
    public void TestMakingNoChangesToMovieWithWatchStatus() {
        //create media data and set watch status
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        movieData.setWatchStatus(MediaData.WATCH_STATUS_TO_WATCH);

        //add media data
        db.movieDataDao().addMovieData(movieData);

        //give watch status count of 1 to represent the movie data
        WatchListModel watchListModel = new WatchListModel("to watch", 1);
        db.watchListsDao().updateListConfiguration(watchListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //save
        onView(withText("SAVE")).perform(click());

        watchListModel = db.watchListsDao().getListByNameAlt("to watch");

        if (watchListModel.getItemCount() != 1) fail();
    }

    @Test
    public void TestMakingNoChangesToUserListWithoutMovie() {
        //create media data and notifier
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        //add empty user list
        UserListModel userListModel = new UserListModel("test list", 0);
        db.userListsDao().addList(userListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //save
        onView(withText("SAVE")).perform(click());

        MovieData dbMovieData = db.movieDataDao().getMovieByIdAlt(movieData.getId());
        userListModel = db.userListsDao().getListByNameAlt("test list");
        List<MovieData> moviesInList = db.movieDataRecordsDao().getAllMoviesInListAlt("test list");

        if (dbMovieData != null) fail();
        if (userListModel.getItemCount() != 0) fail();
        if (moviesInList.size() != 0) fail();
    }

    @Test
    public void TestMakingNoChangesToMovieInUserList() {
        //create media data and notifier
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        //add movie data
        db.movieDataDao().addMovieData(movieData);
        //add user list
        UserListModel userListModel = new UserListModel("test list", 1);
        db.userListsDao().addList(userListModel);
        //add movie record
        db.movieDataRecordsDao().addRecord(new MovieDataRecord(movieData.getId(), userListModel.getName()));

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //save
        onView(withText("SAVE")).perform(click());

        userListModel = db.userListsDao().getListByNameAlt("test list");

        if (userListModel.getItemCount() != 1) fail();
    }

    @Test
    public void TestRemovingWatchStatus() {
        //create media data and set watch status
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        movieData.setWatchStatus(MediaData.WATCH_STATUS_TO_WATCH);

        //add movie data
        db.movieDataDao().addMovieData(movieData);

        //give watch status count of 1 to represent the movie data
        WatchListModel originalWatchListModel = new WatchListModel("to watch", 1);
        db.watchListsDao().updateListConfiguration(originalWatchListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //deselect user list
        onView(withText("None")).perform(click());
        onView(withText("SAVE")).perform(click());

        originalWatchListModel = db.watchListsDao().getListByNameAlt("to watch");
        WatchListModel newWatchListModel = db.watchListsDao().getListByNameAlt("none");
        movieData = db.movieDataDao().getMovieByIdAlt(movieData.getId());

        if (originalWatchListModel.getItemCount() != 0) fail();
        if (newWatchListModel.getItemCount() != 0) fail();
        if (movieData != null) fail();
    }

    @Test
    public void TestAddingMovieToUserList() {
        //create media data
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        //add user list
        UserListModel userListModel = new UserListModel("test list");
        db.userListsDao().addList(userListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //select user list
        onView(withText("test list")).perform(click());
        onView(withText("SAVE")).perform(click());

        WatchListModel watchListModel = db.watchListsDao().getListByNameAlt("none");
        userListModel = db.userListsDao().getListByNameAlt("test list");

        if (watchListModel.getItemCount() != 1) fail();
        if (userListModel.getItemCount() != 1) fail();
    }

    @Test
    public void TestAddingMovieWithoutWatchStatusToWatchList() {
        //create media data and notifier
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //select watch status
        onView(withText("Watched")).perform(click());
        onView(withText("SAVE")).perform(click());

        WatchListModel watchListModel = db.watchListsDao().getListByNameAlt("watched");
        if (watchListModel.getItemCount() != 1) fail();
    }

    @Test
    public void TestChangingMovieWithNonNoneWatchStatusToNonNoneWatchStatus() {
        //create media data and set watch status
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        movieData.setWatchStatus(MediaData.WATCH_STATUS_TO_WATCH);

        db.movieDataDao().addMovieData(movieData);

        //give watch status count of 1 to represent the movie data
        WatchListModel originalWatchList = new WatchListModel("to watch", 1);
        db.watchListsDao().updateListConfiguration(originalWatchList);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //select watch status
        onView(withText("Watched")).perform(click());
        onView(withText("SAVE")).perform(click());

        //get resulting watch lists
        originalWatchList = db.watchListsDao().getListByNameAlt("to watch");
        WatchListModel newWatchList = db.watchListsDao().getListByNameAlt("watched");

        //check if test passes
        if (originalWatchList.getItemCount() != 0) fail();
        if (newWatchList.getItemCount() != 1) fail();
    }

    @Test
    public void TestAddingMovieWithoutWatchStatusToUserListAndSelectingNonNoneWatchStatus() {
        //create media data
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        //add user list
        UserListModel userListModel = new UserListModel("test list");
        db.userListsDao().addList(userListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //select user list
        onView(withText("test list")).perform(click());
        //select watch status
        onView(withText("Watched")).perform(click());
        onView(withText("SAVE")).perform(click());

        WatchListModel originalWatchListModel = db.watchListsDao().getListByNameAlt("none");
        WatchListModel newWatchListModel = db.watchListsDao().getListByNameAlt("watched");
        userListModel = db.userListsDao().getListByNameAlt("test list");

        if (originalWatchListModel.getItemCount() != 0) fail();
        if (newWatchListModel.getItemCount() != 1) fail();
        if (userListModel.getItemCount() != 1) fail();
    }

    @Test
    public void TestAddingNonNoneWatchStatusMovieToUserListAndSelectingNonNoneWatchStatus() {
        //create media data and set watch status
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        movieData.setWatchStatus(MediaData.WATCH_STATUS_TO_WATCH);

        db.movieDataDao().addMovieData(movieData);

        //give watch status count of 1 to represent the movie data
        WatchListModel originalWatchList = new WatchListModel("to watch", 1);
        db.watchListsDao().updateListConfiguration(originalWatchList);

        //add user list
        UserListModel userListModel = new UserListModel("test list");
        db.userListsDao().addList(userListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //select user list
        onView(withText("test list")).perform(click());
        //select watch status
        onView(withText("Watched")).perform(click());
        onView(withText("SAVE")).perform(click());

        WatchListModel originalWatchListModel = db.watchListsDao().getListByNameAlt("to watch");
        WatchListModel newWatchListModel = db.watchListsDao().getListByNameAlt("watched");
        userListModel = db.userListsDao().getListByNameAlt("test list");

        if (originalWatchListModel.getItemCount() != 0) fail();
        if (newWatchListModel.getItemCount() != 1) fail();
        if (userListModel.getItemCount() != 1) fail();
    }

    @Test
    public void TestMovieRemovingWatchStatusAndAddingToUserList() {
        //create media data and set watch status
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        movieData.setWatchStatus(MediaData.WATCH_STATUS_TO_WATCH);

        db.movieDataDao().addMovieData(movieData);

        //give watch status count of 1 to represent the movie data
        WatchListModel originalWatchList = new WatchListModel("to watch", 1);
        db.watchListsDao().updateListConfiguration(originalWatchList);

        //add user list
        UserListModel userListModel = new UserListModel("test list");
        db.userListsDao().addList(userListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //select user list
        onView(withText("test list")).perform(click());
        //select watch status
        onView(withText("None")).perform(click());
        onView(withText("SAVE")).perform(click());

        WatchListModel originalWatchListModel = db.watchListsDao().getListByNameAlt("to watch");
        WatchListModel newWatchListModel = db.watchListsDao().getListByNameAlt("none");
        userListModel = db.userListsDao().getListByNameAlt("test list");

        if (originalWatchListModel.getItemCount() != 0) fail();
        if (newWatchListModel.getItemCount() != 1) fail();
        if (userListModel.getItemCount() != 1) fail();
    }

    @Test
    public void TestRemovingMovieFromUserListAndRetainingWatchStatus() {
        //create media data and set watch status
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        movieData.setWatchStatus(MediaData.WATCH_STATUS_TO_WATCH);

        db.movieDataDao().addMovieData(movieData);

        //give watch status count of 1 to represent the movie data
        WatchListModel originalWatchList = new WatchListModel("to watch", 1);
        db.watchListsDao().updateListConfiguration(originalWatchList);

        //add user list
        UserListModel userListModel = new UserListModel("test list", 1);
        db.userListsDao().addList(userListModel);
        //add movie record
        db.movieDataRecordsDao().addRecord(new MovieDataRecord(movieData.getId(), userListModel.getName()));

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //select user list
        onView(withText("test list")).perform(click());
        onView(withText("SAVE")).perform(click());

        WatchListModel watchListModel = db.watchListsDao().getListByNameAlt("to watch");
        userListModel = db.userListsDao().getListByNameAlt("test list");

        if (watchListModel.getItemCount() != 1) fail();
        if (userListModel.getItemCount() != 0) fail();
    }

    @Test
    public void TestRemovingMovieWithoutWatchStatusFromUserList() {
        //create media data and notifier
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        //add movie data
        db.movieDataDao().addMovieData(movieData);
        //add user list
        UserListModel userListModel = new UserListModel("test list", 1);
        db.userListsDao().addList(userListModel);
        //add movie record
        db.movieDataRecordsDao().addRecord(new MovieDataRecord(movieData.getId(), userListModel.getName()));

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //deselect user list
        onView(withText("test list")).perform(click());
        onView(withText("SAVE")).perform(click());

        WatchListModel watchListModel = db.watchListsDao().getListByNameAlt("none");
        userListModel = db.userListsDao().getListByNameAlt("test list");

        System.out.println("watchListModel: " + watchListModel.getItemCount());
        System.out.println("userListModel: " + userListModel.getItemCount());

        if (watchListModel.getItemCount() != 0) fail();
        if (userListModel.getItemCount() != 0) fail();
    }

    private void registerSimpleIdleResource() {
        mIdlingResource = addToListActivityTestRule.getActivity().getIdlingResource();
        IdlingRegistry.getInstance().register(mIdlingResource);
    }
}
