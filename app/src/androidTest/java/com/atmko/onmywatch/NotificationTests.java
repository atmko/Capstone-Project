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
import androidx.test.uiautomator.UiSelector;

import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.NotificationIdlingResource;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Calendar;
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
public class NotificationTests {
    private IdlingResource mIdlingResource;
    private NotificationIdlingResource mNotificationIdlingResource;

    private AppDatabase db;

    @Before
    public void setupTestDatabase() {
        Context context = ApplicationProvider.getApplicationContext();

        RoomDatabase.Callback callback = databaseInitializer(context);

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .addCallback(callback)
                .build();

        AppDatabase.setDatabase(db);
    }

    private static RoomDatabase.Callback databaseInitializer(final Context context) {
        //reference
        //https://medium.com/@srinuraop/database-create-and-open-callbacks-in-room-7ca98c3286ab
        return new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        String[] seriesWatchListTitles = context.getResources()
                                .getStringArray(R.array.watch_status_series_titles);
                        for (String title: seriesWatchListTitles) {
                            WatchListModel watchListModel = new WatchListModel(title);
                            AppDatabase.getInstance(context).watchListsDao()
                                    .addList(watchListModel);
                        }
                    }
                });
            }

            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
            }
        };
    }

    @Rule
    public ActivityTestRule<MasterActivity> masterActivityTestRule =
            new ActivityTestRule<>(MasterActivity.class);
    @Rule
    public ActivityTestRule<AddToListActivity> addToListActivityTestRule =
            new ActivityTestRule<>(AddToListActivity.class, true, false);

    private void registerSimpleIdleResource() {
        mIdlingResource = addToListActivityTestRule.getActivity().getIdlingResource();
        IdlingRegistry.getInstance().register(mIdlingResource);

    }

    private void registerNotificationIdleResource(int idleCountLimit) {
        mNotificationIdlingResource = NotificationIdlingResource.getInstance();
        mNotificationIdlingResource.setIdleCountLimit(idleCountLimit);
        IdlingRegistry.getInstance().register(mNotificationIdlingResource);
    }

    //tests movie release notifications when watch status is switched to "to watch" or "watching"
    @Test
    public void TestCreatingReleaseNotifierThroughReleaseStatus() {
        //not enabling test mode because live dates and times are being used

        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, 7);

        MovieData movieData = new MovieData("399579", "", false, "",
                "Alita", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        movieData.setReleaseStatus("Post Production");

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //release notifier created with either "To Watch" or "Watching" watch status
        //randomize selecting to watch and watching
        if (utcCalender.getTime().getTime() % 2 == 0) {
            onView(withText("To Watch")).perform(click());

        } else if (utcCalender.getTime().getTime() % 2 == 1) {
            onView(withText("Watching")).perform(click());
        }

        onView(withText("SAVE")).perform(click());

        //ensure notifier is created
        MovieNotifier movieNotifier =
                db.movieNotifierDao().getNotifierByIdAlt(movieData.getId(),
                        MovieNotifier.CONDITION_ON_RELEASE);

        if (movieNotifier == null) fail();
    }

    //tests movie release notifications when watch status is switched to "to watch" or "watching"
    @Test
    public void testMovieReleaseNotification() {
        //not enabling test mode because live dates and times are being used

        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, 7);

        MovieData movieData = new MovieData("399579", "", false, "",
                "Alita", 0, "", "", "",
                new ArrayList<String>(), "", false, "",
                parseIsoDateFromCalender(utcCalender));

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource(1);

        //release notifier created with either "To Watch" or "Watching" watch status
        //randomize selecting to watch and watching
        if (utcCalender.getTime().getTime() % 2 == 0) {
            onView(withText("To Watch")).perform(click());

        } else if (utcCalender.getTime().getTime() % 2 == 1) {
            onView(withText("Watching")).perform(click());
        }

        onView(withText("SAVE")).perform(click());

        checkForNotification(movieData, MediaNotifier.CONDITION_ON_RELEASE);

        //ensure notifier is removed after notification
        MovieNotifier movieNotifier =
                db.movieNotifierDao().getNotifierByIdAlt(movieData.getId(),
                        MovieNotifier.CONDITION_ON_RELEASE);

        if (movieNotifier != null) fail();
    }

    private void checkForNotification(MediaData mediaData, int condition) {
        UiDevice device = UiDevice.getInstance(getInstrumentation());

        device.openNotification();

        String containingText = "";

        if (condition == MediaNotifier.CONDITION_ON_RELEASE) {
            containingText = mediaData.getTitle() + " has been released";

        } else if (condition == SeriesNotifier.CONDITION_NEW_EPISODE){
            containingText = "A new episode of " + mediaData.getTitle();
        }

        UiSelector uiSelector = new UiSelector().textContains(containingText);
        UiCollection uiCollection = new UiCollection(uiSelector);

        if (!uiCollection.exists()) {
            fail();
        }

        device.pressBack();
    }
}
