/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch;

import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;
import androidx.room.Room;
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
import com.atmko.onmywatch.utils.NotificationHandler;
import com.atmko.onmywatch.utils.UpdateNotifierService;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
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
    private NotificationIdlingResource notificationIdlingResource;
    private IdlingResource addToListIdlingResource;
    private final Context context = ApplicationProvider.getApplicationContext();

    private AppDatabase db;

    @Rule
    public ActivityTestRule<MasterActivity> masterActivityTestRule =
            new ActivityTestRule<>(MasterActivity.class);
    @Rule
    public final ActivityTestRule<AddToListActivity> addToListActivityTestRule =
            new ActivityTestRule<>(AddToListActivity.class, true, false);

    @Before
    public void enableTestMode() {
        UpdateNotifierService.sActionMode = UpdateNotifierService.ACTION_TESTING;
        NotificationHandler.IS_TESTING = true;
    }

    @Before
    public void setupTestDatabase() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .build();

        AppDatabase.setDatabase(db);
    }

    @Before
    public void populateDatabase() {
        String[] watchStatusTitles = context.getResources()
                .getStringArray(R.array.watch_status_titles);

        //starting from 1 skips 0(none watch status)
        for (int i = 1; i < watchStatusTitles.length; i++) {
            WatchListModel watchListModel = new WatchListModel(watchStatusTitles[i]);
            AppDatabase.getInstance(context).watchListsDao().addList(watchListModel);
        }
    }

    @After
    public void unregisterIdlingResources() {
        IdlingRegistry.getInstance().unregister(notificationIdlingResource);
        IdlingRegistry.getInstance().unregister(addToListIdlingResource);
    }

    @After
    public void closeDb() {
        db.close();
        db = null;
    }

    @After
    public void reset() {
        UpdateNotifierService.sActionMode = UpdateNotifierService.ACTION_SET;
        NotificationHandler.TEST_TIME_DILATION = 0;
        NotificationHandler.IS_TESTING = false;
        NotificationManagerCompat.from(context).cancelAll();
        GeneralUtils.DateInject.custom = null;
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
                MovieNotifier.CONDITION_ON_RELEASE, true));

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
                SeriesNotifier.CONDITION_ON_RELEASE, true));
        db.seriesNotifierDao().addMediaNotifier(new SeriesNotifier(seriesData.getId(),
                SeriesNotifier.CONDITION_NEW_EPISODE, true));

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

    @Test
    public void testMoviesNotificationClick() {
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
        registerNotificationIdleResource();

        //release notifier created with either "To Watch" or "Watching" watch status
        //randomize selecting to watch and watching
        if (utcCalender.getTime().getTime() % 2 == 0) {
            onView(withText("To Watch")).perform(click());

        } else if (utcCalender.getTime().getTime() % 2 == 1) {
            onView(withText("Watching")).perform(click());
        }

        onView(withText("SAVE")).perform(click());

        clickNotification(movieData, MediaNotifier.CONDITION_ON_RELEASE,
                true, ScheduledMedia.SOURCE_TMDB);
    }

    @Test
    public void testSeriesNotificationClick() {
        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "",
                "2020-04-24");

        seriesData.setReleaseStatus("Running");
        seriesData.setTraktId("1393");

        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, 7);

        Episode nextEpisode = new Episode(seriesData.getId(), 1, 1,
                ScheduledMedia.SOURCE_TRAKT, parseIsoDateFromCalender(utcCalender));
        seriesData.setNextEpisodeToAir(nextEpisode);

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource();

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        clickNotification(seriesData, SeriesNotifier.CONDITION_NEW_EPISODE,
                true, ScheduledMedia.SOURCE_TRAKT);
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
        registerNotificationIdleResource();

        //release notifier created with either "To Watch" or "Watching" watch status
        //randomize selecting to watch and watching
        if (utcCalender.getTime().getTime() % 2 == 0) {
            onView(withText("To Watch")).perform(click());

        } else if (utcCalender.getTime().getTime() % 2 == 1) {
            onView(withText("Watching")).perform(click());
        }

        onView(withText("SAVE")).perform(click());

        checkForNotification(movieData, MediaNotifier.CONDITION_ON_RELEASE,
                true, ScheduledMedia.SOURCE_TMDB);

        //ensure notifier is removed after notification
        MovieNotifier movieNotifier =
                db.movieNotifierDao().getNotifierByIdAlt(movieData.getId(),
                        MovieNotifier.CONDITION_ON_RELEASE);

        if (movieNotifier != null) fail();
    }

    @Test
    public void testSettingNewEpisodeNotifierThroughTmdb() {
        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, 7);

        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "",
                "2020-04-24");

        seriesData.setReleaseStatus("Running");
        seriesData.setTraktId("1393");

        //set date in episode as placeholder, bypass production logic, insert TEST_TIME_DILATION
        Episode nextEpisode = new Episode(seriesData.getId(), 1, 1,
                ScheduledMedia.SOURCE_TMDB, parseIsoDateFromCalender(utcCalender));
        seriesData.setNextEpisodeToAir(nextEpisode);

        //bypass logic to allow past air date to be posted as notification
        GeneralUtils.LOGIC_BYPASS = true;
        NotificationHandler.TEST_TIME_DILATION = TimeUnit.SECONDS.toMillis(7);

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource();

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        checkForNotification(seriesData, SeriesNotifier.CONDITION_NEW_EPISODE,
                true, ScheduledMedia.SOURCE_TMDB);

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
                new ArrayList<String>(), new ArrayList<String>(), "", "",
                "2020-04-24");

        seriesData.setTraktId("1393");
        seriesData.setReleaseStatus(ApiConstants.TextReplacement.REPLACEMENT_RETURNING_SERIES);

        //bypass logic to allow past air date to be posted as notification
        GeneralUtils.LOGIC_BYPASS = true;

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource();

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

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource();

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

        //activate test mode for notification handler
        NotificationHandler.IS_TESTING = true;

        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "", "");

        Episode nextEpisode = new Episode(seriesData.getId(), 1, 1,
                ScheduledMedia.SOURCE_TRAKT, parseIsoDateFromCalender(utcCalender));
        seriesData.setNextEpisodeToAir(nextEpisode);

        seriesData.setTraktId("1393");

        seriesData.setReleaseStatus(ApiConstants.RELEASE_STATUS_IN_PRODUCTION);

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource();

        onView(withText("To Watch")).perform(click());
        onView(withText("SAVE")).perform(click());

        checkForNotification(seriesData, MediaNotifier.CONDITION_ON_RELEASE,
                true, ScheduledMedia.SOURCE_TRAKT);

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
                new ArrayList<String>(), new ArrayList<String>(), "", "",
                "2020-04-24");

        seriesData.setReleaseStatus("Running");
        seriesData.setTraktId("1393");

        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, 7);

        Episode nextEpisode = new Episode(seriesData.getId(), 1, 1,
                ScheduledMedia.SOURCE_TRAKT, parseIsoDateFromCalender(utcCalender));

        seriesData.setNextEpisodeToAir(nextEpisode);

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource();

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        checkForNotification(seriesData, SeriesNotifier.CONDITION_NEW_EPISODE,
                true, ScheduledMedia.SOURCE_TRAKT);

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

        seriesData.setReleaseStatus("Running");
        seriesData.setWatchStatus(MediaData.WATCH_STATUS_WATCHING);
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

    @Test
    public void testNotificationClickFunctionality() {
        //set date to
        ScheduledMedia scheduledMedia = new ScheduledMedia();
        try {
            scheduledMedia.setAirDate("2020-04-23");
            GeneralUtils.DateInject.custom = scheduledMedia.getBestLocalAirDate();
        } catch (ScheduledMedia.DateFormatException e) {
            e.printStackTrace();
        }

        //test first notification displays correct item
        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "",
                "2020-04-24");

        seriesData.setReleaseStatus("Running");
        seriesData.setTraktId("1393");

        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, 7);

        Episode nextEpisode = new Episode(seriesData.getId(), 1, 1,
                ScheduledMedia.SOURCE_TRAKT, parseIsoDateFromCalender(utcCalender));
        seriesData.setNextEpisodeToAir(nextEpisode);

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource();

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        //test second notification displays correct item
        //(needed because pending intent id might conflict and send old data)
        SeriesData seriesData2 = new SeriesData("44217", "", "", "Vikings",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "",
                "2020-04-24");

        seriesData2.setReleaseStatus("Running");
        seriesData2.setTraktId("43973");

        TimeZone utcTimeZone2 = TimeZone.getTimeZone("UTC");
        Calendar utcCalender2 = Calendar.getInstance(utcTimeZone2);
        utcCalender2.add(Calendar.SECOND, 7);

        Episode nextEpisode2 = new Episode(seriesData.getId(), 1, 1,
                ScheduledMedia.SOURCE_TRAKT, parseIsoDateFromCalender(utcCalender));
        seriesData2.setNextEpisodeToAir(nextEpisode2);

        Intent intent2 = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent2.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData2));
        intent2.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent2);

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        clickNotification(seriesData, SeriesNotifier.CONDITION_NEW_EPISODE,
                true, ScheduledMedia.SOURCE_TRAKT);
        onView(withId(R.id.title_text_view)).perform().check(matches(withText("Dead")));
        clickNotification(seriesData2, SeriesNotifier.CONDITION_NEW_EPISODE,
                true, ScheduledMedia.SOURCE_TRAKT);
        onView(withId(R.id.title_text_view)).perform().check(matches(withText("Vikings")));
    }

    @Test
    public void testMovieRestoreReleaseNotificationFromPast() {
        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, -1);

        MovieData movieData = new MovieData("399579", "", false, "",
                "Alita", 0, "", "", "",
                new ArrayList<String>(), "", false, "",
                parseIsoDateFromCalender(utcCalender));

        movieData.setReleaseStatus("Planned");

        //bypass logic to allow past air date to be posted as notification
        GeneralUtils.LOGIC_BYPASS = true;

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(movieData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_MOVIE);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource();

        onView(withText("To Watch")).perform(click());
        onView(withText("SAVE")).perform(click());

        clickNotification(movieData, MediaNotifier.CONDITION_ON_RELEASE,
                false, ScheduledMedia.SOURCE_TMDB);
    }

    @Test
    public void testSeriesRestoreNewEpisodeNotificationFromPast() {
        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, -1);

        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "",
                "2020-04-24");

        seriesData.setReleaseStatus("Running");
        seriesData.setTraktId("1393");

        //set date in episode as placeholder, bypass production logic
        Episode nextEpisode = new Episode(seriesData.getId(), 1, 1,
                ScheduledMedia.SOURCE_TMDB, parseIsoDateFromCalender(utcCalender));
        seriesData.setNextEpisodeToAir(nextEpisode);

        //bypass logic to allow past air date to be posted as notification
        GeneralUtils.LOGIC_BYPASS = true;

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource();

        onView(withText("Watching")).perform(click());
        onView(withText("SAVE")).perform(click());

        clickNotification(seriesData, SeriesNotifier.CONDITION_NEW_EPISODE,
                false, ScheduledMedia.SOURCE_TMDB);
    }

    @Test
    public void testSeriesRestoreReleaseNotificationFromPast() {
        TimeZone utcTimeZone = TimeZone.getTimeZone("UTC");
        Calendar utcCalender = Calendar.getInstance(utcTimeZone);
        utcCalender.add(Calendar.SECOND, -1);

        SeriesData seriesData = new SeriesData("43435", "", "", "Dead",
                0, "", "", "",
                new ArrayList<String>(), new ArrayList<String>(), "", "",
                "2020-04-24");

        seriesData.setReleaseStatus("Planned");
        seriesData.setTraktId("1393");

        //set date in episode as placeholder, bypass production logic
        Episode nextEpisode = new Episode(seriesData.getId(), 1, 1,
                ScheduledMedia.SOURCE_TMDB, parseIsoDateFromCalender(utcCalender));
        seriesData.setNextEpisodeToAir(nextEpisode);

        //bypass logic to allow past air date to be posted as notification
        GeneralUtils.LOGIC_BYPASS = true;

        Intent intent = new Intent(getInstrumentation().getTargetContext(), AddToListActivity.class);
        intent.putExtra(AddToListActivity.MEDIA_DATA_KEY, Parcels.wrap(seriesData));
        intent.putExtra(AddToListActivity.MEDIA_TYPE_KEY, MasterActivity.MEDIA_TYPE_SERIES);

        addToListActivityTestRule.launchActivity(intent);

        registerSimpleIdleResource();
        registerNotificationIdleResource();

        onView(withText("To Watch")).perform(click());
        onView(withText("SAVE")).perform(click());

        clickNotification(seriesData, SeriesNotifier.CONDITION_ON_RELEASE,
                false, ScheduledMedia.SOURCE_TMDB);
    }

    private void registerSimpleIdleResource() {
        addToListIdlingResource = addToListActivityTestRule.getActivity().getIdlingResource();
        IdlingRegistry.getInstance().register(addToListIdlingResource);
    }

    private void registerNotificationIdleResource() {
        notificationIdlingResource = NotificationIdlingResource.getInstance();
        IdlingRegistry.getInstance().register(notificationIdlingResource);
    }

    @SuppressWarnings("SameParameterValue")
    private void checkForNotification(MediaData mediaData, int condition, boolean isInFuture, int source) {
        UiDevice device = UiDevice.getInstance(getInstrumentation());

        device.openNotification();

        int mediaType = mediaData instanceof MovieData ? MasterActivity.MEDIA_TYPE_MOVIE : MasterActivity.MEDIA_TYPE_SERIES;
        String titleText = getTitleText(mediaType, mediaData.getTitle(), isInFuture);
        String containingText = getContainingText(mediaType, condition, isInFuture, source);
        if (containingText == null) fail();

        UiSelector uiSelector = new UiSelector().textContains(titleText).textContains(containingText);
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

    private void clickNotification(MediaData mediaData, int condition, boolean isInFuture, int source) {
        UiDevice device = UiDevice.getInstance(getInstrumentation());

        device.openNotification();

        int mediaType = mediaData instanceof MovieData ? MasterActivity.MEDIA_TYPE_MOVIE : MasterActivity.MEDIA_TYPE_SERIES;
        String titleText = getTitleText(mediaType, mediaData.getTitle(), isInFuture);
        String containingText = getContainingText(mediaType, condition, isInFuture, source);
        if (containingText == null) fail();

        UiSelector uiSelector = new UiSelector().textContains(titleText);
        uiSelector.textContains(containingText);
        UiCollection uiCollection = new UiCollection(uiSelector);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (!uiCollection.exists()) {
            fail();
        }

        try {
            uiCollection.click();
        } catch (UiObjectNotFoundException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private String getTitleText(int mediaType, String mediaTitle, boolean isInFuture) {
        if (mediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
            if (isInFuture) {
                return mediaTitle + " releases today";
            } else {
                return mediaTitle + " was already released";
            }
        } else {
            if (isInFuture) {
                return mediaTitle + " airs soon";
            } else {
                return mediaTitle + " already aired";
            }
        }
    }

    private String getContainingText(int mediaType, int condition, boolean isInFuture, int source) {
        if (mediaType == MasterActivity.MEDIA_TYPE_MOVIE) return "";
        if (condition == MediaNotifier.CONDITION_ON_RELEASE) {
            if (isInFuture) {
                if (source == ScheduledMedia.SOURCE_TRAKT) {
                    return "Premieres in 10 minutes";
                } else {
                    return "Premieres today";
                }
            } else {
                if (source == ScheduledMedia.SOURCE_TRAKT) {
                    return "Premiere already aired";
                } else {
                    return "Premiere may have already aired";
                }
            }
        } else if (condition == SeriesNotifier.CONDITION_NEW_EPISODE) {
            if (isInFuture) {
                if (source == ScheduledMedia.SOURCE_TRAKT) {
                    return "S1E1 airs in 10 minutes";
                } else {
                    return "S1E1 airs today";
                }
            } else {
                if (source == ScheduledMedia.SOURCE_TRAKT) {
                    return "S1E1 already aired";
                } else {
                    return "S1E1 may have already aired";
                }
            }
        } else {
            return null;
        }
    }
}
