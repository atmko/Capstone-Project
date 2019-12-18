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
import com.atmko.onmywatch.models.Episode;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.MediaNotifier;
import com.atmko.onmywatch.models.MovieData;
import com.atmko.onmywatch.models.MovieNotifier;
import com.atmko.onmywatch.models.NotificationIdlingResource;
import com.atmko.onmywatch.models.ScheduledMedia;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.models.SeriesNotifier;
import com.atmko.onmywatch.models.WatchListModel;
import com.atmko.onmywatch.utils.GeneralUtils;
import com.atmko.onmywatch.utils.UpdateNotifierService;
import com.atmko.onmywatch.utils.network_utils.ApiConstants;
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

    @Before
    public void enableTestMode() {
        UpdateNotifierService.sActionMode = UpdateNotifierService.ACTION_TESTING;
    }

    //ensures movie notifiers get canceled when watch status updated to other than "to watch" or "watching"
    @Test
    public void TestCancelingMovieNotifierOnWatchStatusChanged() {
        //create media data and notifier
        MovieData movieData = new MovieData("399579", "", false, "",
                "", 0, "", "", "",
                new ArrayList<String>(), "", false, "", "");

        db.movieDataDao().addMovieData(movieData);
        db.movieNotifierDao().addMediaNotifier(new MovieNotifier(movieData.getId(),
                MovieNotifier.CONDITION_ON_RELEASE));

        //ensure notifier is created
        MovieNotifier movieNotifier =
                db.movieNotifierDao().getNotifierByIdAlt(movieData.getId(),
                        MovieNotifier.CONDITION_ON_RELEASE);

        if (movieNotifier == null) fail();

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //select watch status
        //release notifier canceled with non "To Watch" or "Watching" watch status
        onView(withText("Watched")).perform(click());
        onView(withText("SAVE")).perform(click());

        //ensure notifier is removed due to watch status change
        movieNotifier = db.movieNotifierDao().getNotifierByIdAlt(movieData.getId(),
                MovieNotifier.CONDITION_ON_RELEASE);

        if (movieNotifier != null) fail();
    }

    //ensures series notifiers get canceled when watch status updated to other than "to watch" or "watching"
    @Test
    public void TestCancelingSeriesNotifierOnWatchStatusChanged() {
        //create media data and notifier
        SeriesData seriesData = new SeriesData("43435", "", "", "",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "", "");

        db.seriesDataDao().addSeriesData(seriesData);
        db.seriesNotifierDao().addMediaNotifier(new SeriesNotifier(seriesData.getId(),
                SeriesNotifier.CONDITION_ON_RELEASE));
        db.seriesNotifierDao().addMediaNotifier(new SeriesNotifier(seriesData.getId(),
                SeriesNotifier.CONDITION_NEW_EPISODE));

        //ensure notifier is created
        SeriesNotifier releaseNotifier =
                db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                        SeriesNotifier.CONDITION_ON_RELEASE);

        SeriesNotifier newEpisodeNotifier =
                db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                        SeriesNotifier.CONDITION_NEW_EPISODE);

        if (releaseNotifier == null) fail();
        if (newEpisodeNotifier == null) fail();

        //launch activity
        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        //select watch status
        //release notifier canceled with non "To Watch" or "Watching" watch status
        onView(withText("Watched")).perform(click());
        onView(withText("SAVE")).perform(click());

        //ensure notifiers are removed due to watch status change
        releaseNotifier = db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                SeriesNotifier.CONDITION_ON_RELEASE);
        newEpisodeNotifier = db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                SeriesNotifier.CONDITION_NEW_EPISODE);

        if (releaseNotifier != null) fail();
        if (newEpisodeNotifier != null) fail();
    }

    //tests movie release notifications when release date doesn't exist by using release status
    @Test
    public void TestCreatingReleaseNotifierThroughReleaseStatus() {
        long currentTimeMillis = System.currentTimeMillis();

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
        if (currentTimeMillis % 2 == 0) {
            onView(withText("To Watch")).perform(click());

        } else if (currentTimeMillis % 2 == 1) {
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

    @Test
    public void testSettingNewEpisodeNotifierThroughTmdb() {
        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "", "");

        seriesData.setTraktId("1393");

        Episode nextEpisode = new Episode();

        //set date in past to ensure notification first and second triggered with bypass logic
        try {
            nextEpisode.setAirDate("2019-08-08T05:00:00.000Z");
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        //bypass logic to allow past air date to be posted as notification
        GeneralUtils.LOGIC_BYPASS = true;

        seriesData.setNextEpisodeToAir(nextEpisode);

        //feign trakt next episode NULL
        UpdateNotifierService.ASSUME_TRAKT_NEXT_EPISODE_NULL = true;

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource(1);

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        checkForNotification(seriesData, SeriesNotifier.CONDITION_NEW_EPISODE);

        //ensure notifier isn't removed after notification
        SeriesNotifier seriesNotifier =
                db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                        SeriesNotifier.CONDITION_NEW_EPISODE);

        if (seriesNotifier == null) fail();
    }

    @Test
    public void testSettingSeriesNewEpisodeNotifierThroughReleaseStatus() {
        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "", "");

        seriesData.setTraktId("1393");
        seriesData.setReleaseStatus(ApiConstants.TextReplacement.REPLACEMENT_RETURNING_SERIES);

        //bypass logic to allow past air date to be posted as notification
        GeneralUtils.LOGIC_BYPASS = true;

        //feign trakt next episode NULL
        UpdateNotifierService.ASSUME_TRAKT_NEXT_EPISODE_NULL = true;

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        //ensure notifier isn't removed after notification
        SeriesNotifier seriesNotifier =
                db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                        SeriesNotifier.CONDITION_NEW_EPISODE);

        if (seriesNotifier == null) fail();
    }

    @Test
    public void testSettingSeriesReleaseNotifierThroughReleaseStatus() {
        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "", "");

        seriesData.setTraktId("1393");
        seriesData.setReleaseStatus(ApiConstants.TextReplacement.REPLACEMENT_IN_PRODUCTION);

        //bypass logic to allow past air date to be posted as notification
        GeneralUtils.LOGIC_BYPASS = true;

        //feign trakt next episode NULL
        UpdateNotifierService.ASSUME_TRAKT_NEXT_EPISODE_NULL = true;

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        //ensure notifier isn't removed after notification
        SeriesNotifier seriesNotifier =
                db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                        SeriesNotifier.CONDITION_ON_RELEASE);

        if (seriesNotifier == null) fail();
    }

    //tests series release notifications when watch status is switched to "to watch"
    @Test
    public void testSeriesReleaseNotification() {
        //not enabling test mode because live dates and times are being used

        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, 7);

        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "",
                parseIsoDateFromCalender(utcCalender));

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource(1);

        onView(withText("To Watch")).perform(click());
        onView(withText("SAVE")).perform(click());

        checkForNotification(seriesData, MediaNotifier.CONDITION_ON_RELEASE);

        //ensure release notifier is removed after notification
        SeriesNotifier seriesReleaseNotifier =
                db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                        SeriesNotifier.CONDITION_ON_RELEASE);

        if (seriesReleaseNotifier != null) fail();

        //ensure new episode notifier isn't created after notification
        SeriesNotifier newEpisodeNotifier =
                db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                        SeriesNotifier.CONDITION_NEW_EPISODE);

        if (newEpisodeNotifier != null) fail();
    }

    //test if notification is shown when next episode date is in the future
    @Test
    public void testNextEpisodeInFuture() {
        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "", "");

        seriesData.setTraktId("1393");
        Episode nextEpisode = new Episode();

        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, 7);

        try {
            nextEpisode.setAirDate(parseIsoDateFromCalender(utcCalender));

        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        seriesData.setNextEpisodeToAir(nextEpisode);

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource(1);

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        checkForNotification(seriesData, SeriesNotifier.CONDITION_NEW_EPISODE);

        //ensure notifier isn't removed after notification
        SeriesNotifier seriesNotifier =
                db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                        SeriesNotifier.CONDITION_NEW_EPISODE);

        if (seriesNotifier == null) fail();
    }

    //test if notifier is created when next episode date is in the past
    @Test
    public void testNextEpisodeInPast() {
        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "", "");

        seriesData.setTraktId("1393");
        Episode nextEpisode = new Episode();

        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, -1);

        try {
            nextEpisode.setAirDate(parseIsoDateFromCalender(utcCalender));

        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        seriesData.setNextEpisodeToAir(nextEpisode);

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        //ensure notifier is never created
        SeriesNotifier seriesNotifier =
                db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                        SeriesNotifier.CONDITION_NEW_EPISODE);

        if (seriesNotifier != null) fail();
    }

    //tests recurring next episode notifications
    @Test
    public void doubleNextEpisodeNotificationTest() {
        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "", "");

        seriesData.setTraktId("1393");
        Episode nextEpisode = new Episode();

        //set date in past to ensure notification first and second triggered with bypass logic
        try {
            nextEpisode.setAirDate("2019-08-08T05:00:00.000Z");
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        //bypass logic to allow past air date to be posted as notification
        GeneralUtils.LOGIC_BYPASS = true;

        seriesData.setNextEpisodeToAir(nextEpisode);

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource(2);

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        checkForNotification(seriesData, SeriesNotifier.CONDITION_NEW_EPISODE);

        //ensure notifier isn't removed after notification
        SeriesNotifier seriesNotifier =
                db.seriesNotifierDao().getNotifierByIdAlt(seriesData.getId(),
                        SeriesNotifier.CONDITION_NEW_EPISODE);

        if (seriesNotifier == null) fail();
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

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!uiCollection.exists()) {
            fail();
        }

        device.pressBack();
    }
}
