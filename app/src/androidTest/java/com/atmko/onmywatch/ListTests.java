/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieDataRecord;
import com.atmko.onmywatch.models.UserListModel;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.UpdateNotifierService;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItem;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.fail;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ListTests {
    private IdlingResource masterIdlingResource;
    private IdlingResource addToListIdlingResource;
    private Context context = ApplicationProvider.getApplicationContext();

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
            db.watchListsDao().addList(watchListModel);
        }
    }

    @Before
    public void enableTestMode() {
        UpdateNotifierService.sActionMode = UpdateNotifierService.ACTION_TESTING;
    }

    @After
    public void unregisterIdlingResources() {
        IdlingRegistry.getInstance().unregister(masterIdlingResource);
        IdlingRegistry.getInstance().unregister(addToListIdlingResource);
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

        movieData.setReleaseStatus("");

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerAddToListSimpleIdleResource();

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
        movieData.setReleaseStatus("");

        //add media data
        db.movieDataDao().addMovieData(movieData);

        //get media from the database
        String oldWatchStatusName = MediaData.getWatchStatusTitle(movieData.getWatchStatus(), context);
        WatchListModel watchListModel = db.watchListsDao().getListByNameAlt(oldWatchStatusName);

        //give watch status count of 1 to represent the movie data
        watchListModel.setItemCount(1);
        db.watchListsDao().updateListConfiguration(watchListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerAddToListSimpleIdleResource();

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

        movieData.setReleaseStatus("");

        //add empty user list
        UserListModel userListModel = new UserListModel("test list", 0);
        db.userListsDao().addList(userListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerAddToListSimpleIdleResource();

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

        registerAddToListSimpleIdleResource();

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

        movieData.setReleaseStatus("");
        movieData.setWatchStatus(MediaData.WATCH_STATUS_TO_WATCH);

        //add movie data
        db.movieDataDao().addMovieData(movieData);

        //get media from the database
        String oldWatchStatusName = MediaData.getWatchStatusTitle(movieData.getWatchStatus(), context);
        WatchListModel originalWatchListModel = db.watchListsDao().getListByNameAlt(oldWatchStatusName);

        //give watch status count of 1 to represent the movie data
        originalWatchListModel.setItemCount(1);
        db.watchListsDao().updateListConfiguration(originalWatchListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerAddToListSimpleIdleResource();

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

        registerAddToListSimpleIdleResource();

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

        registerAddToListSimpleIdleResource();

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

        //add movie data
        db.movieDataDao().addMovieData(movieData);

        //get media from the database
        String oldWatchStatusName = MediaData.getWatchStatusTitle(movieData.getWatchStatus(), context);
        WatchListModel originalWatchList = db.watchListsDao().getListByNameAlt(oldWatchStatusName);

        //give watch status count of 1 to represent the movie data
        originalWatchList.setItemCount(1);
        db.watchListsDao().updateListConfiguration(originalWatchList);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerAddToListSimpleIdleResource();

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

        registerAddToListSimpleIdleResource();

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

        //add movie data
        db.movieDataDao().addMovieData(movieData);

        //get media from the database
        String oldWatchStatusName = MediaData.getWatchStatusTitle(movieData.getWatchStatus(), context);
        WatchListModel originalWatchList = db.watchListsDao().getListByNameAlt(oldWatchStatusName);

        //give watch status count of 1 to represent the movie data
        originalWatchList.setItemCount(1);
        db.watchListsDao().updateListConfiguration(originalWatchList);

        //add user list
        UserListModel userListModel = new UserListModel("test list");
        db.userListsDao().addList(userListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerAddToListSimpleIdleResource();

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

        //add movie data
        db.movieDataDao().addMovieData(movieData);

        //get media from the database
        String oldWatchStatusName = MediaData.getWatchStatusTitle(movieData.getWatchStatus(), context);
        WatchListModel originalWatchList = db.watchListsDao().getListByNameAlt(oldWatchStatusName);

        //give watch status count of 1 to represent the movie data
        originalWatchList.setItemCount(1);
        db.watchListsDao().updateListConfiguration(originalWatchList);

        //add user list
        UserListModel userListModel = new UserListModel("test list");
        db.userListsDao().addList(userListModel);

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerAddToListSimpleIdleResource();

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
        movieData.setReleaseStatus("");

        //add movie data
        db.movieDataDao().addMovieData(movieData);

        //get media from the database
        String oldWatchStatusName = MediaData.getWatchStatusTitle(movieData.getWatchStatus(), context);
        WatchListModel originalWatchList = db.watchListsDao().getListByNameAlt(oldWatchStatusName);

        //give watch status count of 1 to represent the movie data
        originalWatchList.setItemCount(1);
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

        registerAddToListSimpleIdleResource();

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

        registerMasterSimpleIdleResource();
        registerAddToListSimpleIdleResource();

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

    @Test
    public void TestWatchListCountWhenDeletingUserListWithMediaWithWatchStatus() {
        registerMasterSimpleIdleResource();

        //create media data
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");
        //create media data
        MovieData movieData2 = new MovieData("475557", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        movieData.setWatchStatus(1);
        db.movieDataDao().addMovieData(movieData);
        movieData2.setWatchStatus(1);
        db.movieDataDao().addMovieData(movieData2);

        //get watch list from database
        String oldWatchStatusName = MediaData.getWatchStatusTitle(movieData.getWatchStatus(), context);
        WatchListModel watchListModelUpdate = db.watchListsDao().getListByNameAlt(oldWatchStatusName);

        //update watch list
        watchListModelUpdate.setItemCount(2);
        db.watchListsDao().updateListConfiguration(watchListModelUpdate);

        //create user list
        UserListModel userListModel = new UserListModel("test list", 2);
        db.userListsDao().addList(userListModel);

        //create media record
        MovieDataRecord movieDataRecord = new MovieDataRecord(movieData.getId(), "test list");
        db.movieDataRecordsDao().addRecord(movieDataRecord);
        //create media record
        MovieDataRecord movieDataRecord2 = new MovieDataRecord(movieData2.getId(), "test list");
        db.movieDataRecordsDao().addRecord(movieDataRecord2);

        onView(withText("Lists")).perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withText("User Lists")).perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(allOf(ViewMatchers.withId(R.id.results_recycler_view), isCompletelyDisplayed()))
                .perform(actionOnItem(hasDescendant(withText("test list")), clickChildViewWithId(R.id.options_spinner)));

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withText("Delete")).perform(click());

        //click confirmation text
        onView(withText("Yes")).perform(click());

        //check user list deleted
        UserListModel deletedList = db.userListsDao().getListByNameAlt("test list");
        if (deletedList != null) fail();

        //check media not deleted
        MovieData savedMovieReference = db.movieDataDao().getMovieByIdAlt(movieData.getId());
        if (savedMovieReference == null) fail();
        MovieData savedMovieReference2 = db.movieDataDao().getMovieByIdAlt(movieData2.getId());
        if (savedMovieReference2 == null) fail();

        //check records deleted
        MovieDataRecord savedRecordReference = db.movieDataRecordsDao().getRecordByIdAlt(movieData.getId(), "test list");
        if (savedRecordReference != null) fail();
        MovieDataRecord savedRecordReference2 = db.movieDataRecordsDao().getRecordByIdAlt(movieData2.getId(), "test list");
        if (savedRecordReference2 != null) fail();

        //check watchlist count
        WatchListModel savedWatchListModel = db.watchListsDao().getListByNameAlt("to watch");
        if (savedWatchListModel.getItemCount() != 2) fail();
    }

    @Test
    public void TestWatchListCountWhenDeletingUserListWithMediaWithoutWatchStatus() {
        registerMasterSimpleIdleResource();

        //create media data
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");
        MovieData movieData2 = new MovieData("475557", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        db.movieDataDao().addMovieData(movieData);
        db.movieDataDao().addMovieData(movieData2);

        //create user list
        UserListModel userListModel = new UserListModel("test list", 2);
        db.userListsDao().addList(userListModel);

        //create media record
        MovieDataRecord movieDataRecord = new MovieDataRecord(movieData.getId(), "test list");
        db.movieDataRecordsDao().addRecord(movieDataRecord);
        MovieDataRecord movieDataRecord2 = new MovieDataRecord(movieData2.getId(), "test list");
        db.movieDataRecordsDao().addRecord(movieDataRecord2);

        onView(withText("Lists")).perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withText("User Lists")).perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(allOf(ViewMatchers.withId(R.id.results_recycler_view), isCompletelyDisplayed()))
                .perform(actionOnItem(hasDescendant(withText("test list")), clickChildViewWithId(R.id.options_spinner)));

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        onView(withText("Delete")).perform(click());

        //click confirmation text
        onView(withText("Yes")).perform(click());

        //check user list deleted
        UserListModel deletedList = db.userListsDao().getListByNameAlt("test list");
        if (deletedList != null) fail();

        //check media is deleted
        MovieData savedMovieReference = db.movieDataDao().getMovieByIdAlt(movieData.getId());
        if (savedMovieReference != null) fail();
        MovieData savedMovieReference2 = db.movieDataDao().getMovieByIdAlt(movieData2.getId());
        if (savedMovieReference2 != null) fail();

        //check records deleted
        MovieDataRecord savedRecordReference = db.movieDataRecordsDao().getRecordByIdAlt(movieData.getId(), "test list");
        if (savedRecordReference != null) fail();
        MovieDataRecord savedRecordReference2 = db.movieDataRecordsDao().getRecordByIdAlt(movieData2.getId(), "test list");
        if (savedRecordReference2 != null) fail();

        //check watchlist count
        WatchListModel savedWatchListModel = db.watchListsDao().getListByNameAlt("to watch");
        if (savedWatchListModel.getItemCount() != 0) fail();
    }

    private void registerMasterSimpleIdleResource() {
        masterIdlingResource = masterActivityTestRule.getActivity().getIdlingResource();
        IdlingRegistry.getInstance().register(masterIdlingResource);
    }

    private void registerAddToListSimpleIdleResource() {
        addToListIdlingResource = addToListActivityTestRule.getActivity().getIdlingResource();
        IdlingRegistry.getInstance().register(addToListIdlingResource);
    }

    public ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specified id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                v.performClick();
            }
        };
    }
}
