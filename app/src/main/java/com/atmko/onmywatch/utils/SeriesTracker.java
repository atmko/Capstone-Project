/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.androidnetworking.common.ANRequest;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.StringRequestListener;
import com.atmko.onmywatch.R;
import com.atmko.onmywatch.database.AppDatabase;
import com.atmko.onmywatch.database.daos.SeriesLogsDao;
import com.atmko.onmywatch.models.Episode;
import com.atmko.onmywatch.models.MediaData;
import com.atmko.onmywatch.models.SeriesLog;
import com.atmko.onmywatch.models.NotificationIdlingResource;
import com.atmko.onmywatch.models.Season;
import com.atmko.onmywatch.models.SeriesData;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;
import com.atmko.onmywatch.utils.api_utils.NetworkFunctions;
import com.atmko.onmywatch.utils.api_utils.SeriesDataParser.EpisodeParser;
import com.atmko.onmywatch.utils.api_utils.SeriesDataParser.SeasonParser;
import com.atmko.onmywatch.utils.network_utils.AppExecutors;
import com.atmko.onmywatch.utils.network_utils.TraktApiConstants;
import com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker;

import org.parceler.Parcels;

import java.util.List;

import static com.atmko.onmywatch.models.SeriesLog.CONDITION_AIRED;
import static com.atmko.onmywatch.models.SeriesLog.CONDITION_UNDATED;
import static com.atmko.onmywatch.models.SeriesLog.CONDITION_UPCOMING;
import static com.atmko.onmywatch.models.SeriesLog.TYPE_EPISODE;
import static com.atmko.onmywatch.models.SeriesLog.TYPE_SEASON;
import static com.atmko.onmywatch.utils.GeneralUtils.MILLISECOND_CONVERSION;
import static com.atmko.onmywatch.utils.network_utils.work_manager_workers.UpdateMediaWorker.NEW_MEDIA_DATA_KEY;

public class SeriesTracker extends JobIntentService {
    private static final String ACTION_TESTING = "testing";
    public static final String ACTION_SET = "set";
    public static final String ACTION_DELETE = "delete";

    private static final int COOL_DOWN_REQUEST_TRAKT_SEASON = 1;
    private static final int COOL_DOWN_REQUEST_TRAKT_SEASON_EPISODES = 2;

    private static final int JOB_ID = 21;

    public static String sActionMode = ACTION_SET;

    private AppDatabase mDatabase;
    private SeriesData newMediaData;
    private Season lastSeason;

    public static void enqueueWork(Context appContext, Intent intent) {
        //set idle state to false
        if (NotificationIdlingResource.getNotificationIdlingResource() != null) {
            NotificationIdlingResource.getNotificationIdlingResource().setIdleState(false);
        }

        enqueueWork(appContext, SeriesTracker.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        newMediaData = Parcels.unwrap(intent.getParcelableExtra(NEW_MEDIA_DATA_KEY));
        mDatabase = AppDatabase.getInstance(getApplicationContext());

        if (sActionMode.equals(ACTION_SET) || sActionMode.equals(ACTION_TESTING)) {
            if (newMediaData.getWatchStatus() == MediaData.WATCH_STATUS_WATCHING) {
                trackSeriesMedia();
            }
        } else if (sActionMode.equals(ACTION_DELETE)) {
            if (newMediaData.getWatchStatus() != MediaData.WATCH_STATUS_WATCHING) {
                deleteTrackedMedia();
            }
        }
    }

    private void trackSeriesMedia() {
        getSeasons(newMediaData.getTraktId());
    }

    //get seasons from trakt api
    private void getSeasons(final String traktId) {
        String traktFetchUrl = getString(R.string.trakt_season_url);
        ANRequest request = NetworkFunctions.traktAgnosticRequestById(traktFetchUrl, traktId);

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        List<Season> seasons =
                                SeasonParser.parseTraktSeasons(returnedJSONString);
                        lastSeason = seasons.get(seasons.size() - 1);
                        getEpisodesInSeason(traktId, String.valueOf(lastSeason.seasonNumber));
                    }
                });
            }

            @Override
            public void onError(final ANError anError) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (anError.getErrorCode() == TraktApiConstants.TOO_MANY_REQUESTS) {
                            retryAfterCoolDOwn(anError, COOL_DOWN_REQUEST_TRAKT_SEASON);
                        }
                    }
                });
            }
        });
    }

    //get episodes from trakt api
    private void getEpisodesInSeason(final String traktId, String seasonNumber) {
        String traktFetchUrl = getString(R.string.trakt_season_episodes_url);
        ANRequest request = NetworkFunctions.traktSeasonEpisodeRequestBy(traktFetchUrl, traktId,
                seasonNumber);

        request.getAsString(new StringRequestListener() {
            @Override
            public void onResponse(final String returnedJSONString) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        deleteTrackedMedia();

                        List<Episode> episodes =
                                EpisodeParser.parseTraktEpisodes(newMediaData.getId(), returnedJSONString);
                        lastSeason.setEpisodes(episodes);

                        //TODO:when trakt info unavailable show tmdb episode log

                        //make data congruent by...
                        //setting episodes aired using series next episode property
                        //replacing corresponding episode with series next episode only if net episode's source is trakt
                        if (newMediaData.getNextEpisodeToAir() != null &&
                                newMediaData.getNextEpisodeToAir().source == Episode.SOURCE_TRAKT) {
                            if (lastSeason.seasonNumber == newMediaData.getNextEpisodeToAir().seasonNumber) {
                                lastSeason.episodesAired = newMediaData.getNextEpisodeToAir().episodeNumber - 1;
                                lastSeason.overrideEpisode(newMediaData.getNextEpisodeToAir());
                            }
                        }

                        boolean isRunning = newMediaData.getReleaseStatus()
                                .equals(ApiConstants.TextReplacement.REPLACEMENT_RETURNING_SERIES);

                        //if season is finished and is running, show season end and show next season without release date
                        //otherwise process bundle if bundle and single if single

                        if (lastSeason.hasEnded() && isRunning) {
                            //set season ended
                            Episode lastEpisodeInSeason =
                                    lastSeason.getEpisodes().get(lastSeason.getEpisodes().size() - 1);

                            insertSeason(lastSeason.seasonNumber, CONDITION_AIRED,
                                    lastEpisodeInSeason.getTimestamp(), lastSeason.isBundled);

                            //set next season without air date();
                            Season nextSeason = new Season(lastSeason.seasonNumber + 1, "");
                            insertSeason(nextSeason.seasonNumber, CONDITION_UNDATED,
                                    nextSeason.getTimestamp(), lastSeason.isBundled);

                        } else if (!lastSeason.hasEnded() && isRunning){
                            if (lastSeason.isBundled) {
                                processBundle();
                            } else {
                                processSingles();
                            }
                        }
                    }
                });
            }

            @Override
            public void onError(final ANError anError) {
                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (anError.getErrorCode() == TraktApiConstants.TOO_MANY_REQUESTS) {
                            retryAfterCoolDOwn(anError, COOL_DOWN_REQUEST_TRAKT_SEASON_EPISODES);
                        }
                    }
                });
            }
        });
    }

    private void processBundle() {
        //set upcoming season if air date available else set without air date
        Episode nextEpisode = lastSeason.getNextEpisodeInSeason();
        String nextEpisodeAirDate = nextEpisode.getBestAvailableDateString();

        if (nextEpisodeAirDate != null && !nextEpisodeAirDate.equals("")) {
            if (nextEpisode.isInFuture()) {
                //set season upcoming
                insertSeason(lastSeason.seasonNumber, CONDITION_UPCOMING,
                        nextEpisode.getTimestamp(), lastSeason.isBundled);

            } else {
                //set season aired
                insertSeason(lastSeason.seasonNumber, CONDITION_AIRED,
                        nextEpisode.getTimestamp(), lastSeason.isBundled);
            }

        } else {
            //set season undated
            insertSeason(lastSeason.seasonNumber, CONDITION_UNDATED,
                    nextEpisode.getTimestamp(), lastSeason.isBundled);
        }
    }

    private void processSingles() {
        Episode currentEpisode = null;
        if (lastSeason.episodesAired > 0) {
            currentEpisode = lastSeason.getEpisode(lastSeason.episodesAired);
        }

        Episode nextEpisode = lastSeason.getNextEpisodeInSeason();
        String nextEpisodeAirDate = null;

        if (nextEpisode != null) {
            nextEpisodeAirDate = nextEpisode.getBestAvailableDateString();
        }

        //if both current and next episodes are in the past, log next episode aired only
        if (currentEpisode != null && nextEpisode != null
                && !currentEpisode.isInFuture() && !nextEpisode.isInFuture()) {
            insertEpisode(lastSeason.seasonNumber, nextEpisode.episodeNumber, CONDITION_AIRED,
                    nextEpisode.getTimestamp(), lastSeason.isBundled);
            return;
        }

        //set episode ended
        if (currentEpisode != null) {
            insertEpisode(lastSeason.seasonNumber, currentEpisode.episodeNumber, CONDITION_AIRED,
                    currentEpisode.getTimestamp(), lastSeason.isBundled);
        }

        //set upcoming / aired episode if air date available else set without air date
        if (nextEpisodeAirDate != null && !nextEpisodeAirDate.equals("")) {
            if (nextEpisode.isInFuture()) {
                //set episode upcoming
                insertEpisode(lastSeason.seasonNumber, nextEpisode.episodeNumber, CONDITION_UPCOMING,
                        nextEpisode.getTimestamp(), lastSeason.isBundled);

            } else {
                //set episode upcoming
                insertEpisode(lastSeason.seasonNumber, nextEpisode.episodeNumber, CONDITION_AIRED,
                        nextEpisode.getTimestamp(), lastSeason.isBundled);
            }

        } else {
            //set episode undated
            if (nextEpisode != null) {
                insertEpisode(lastSeason.seasonNumber, nextEpisode.episodeNumber, CONDITION_UNDATED,
                        nextEpisode.getTimestamp(), lastSeason.isBundled);
            }
        }
    }

    private void insertSeason(int seasonNumber, int condition, long timestamp, boolean isBundled) {
        SeriesLog mediaLog = new SeriesLog(TYPE_SEASON, seasonNumber, condition, timestamp,
                newMediaData.getTitle(), newMediaData.getPosterPath(), newMediaData.getBackdropPath(),
                newMediaData.getId(), isBundled);

        mDatabase.seriesLogsDao().addMediaLog(mediaLog);
    }

    private void insertEpisode(int seasonNumber, int episodeNumber, int condition, long timestamp,
                               boolean isBundled) {
        SeriesLog mediaLog = new SeriesLog(TYPE_EPISODE, seasonNumber, episodeNumber, condition,
                timestamp, newMediaData.getTitle(), newMediaData.getPosterPath(),
                newMediaData.getBackdropPath(), newMediaData.getId(), isBundled);

        mDatabase.seriesLogsDao().addMediaLog(mediaLog);
    }

    //retry method if api returns too may requests error
    private void retryAfterCoolDOwn(ANError anError, final int coolDownRequestId) {
        int coolDown;

        try {
            //noinspection ConstantConditions
            coolDown = Integer.parseInt(anError.getResponse().header(ApiConstants.RETRY_AFTER_KEY));

        } catch (NullPointerException e) {
            e.printStackTrace();
            coolDown = UpdateMediaWorker.REQUEST_COOL_DOWN;

        }

        int coolDownInMilliSecs = coolDown * MILLISECOND_CONVERSION;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (coolDownRequestId == COOL_DOWN_REQUEST_TRAKT_SEASON){
                        getSeasons(newMediaData.getTraktId());

                    } else if (coolDownRequestId == COOL_DOWN_REQUEST_TRAKT_SEASON_EPISODES) {
                        getEpisodesInSeason(newMediaData.getTraktId(),
                                String.valueOf(lastSeason.seasonNumber));
                    }

                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

            }
        }, coolDownInMilliSecs);
    }

    //deletes the upcoming log and updates upcoming log variable condition to CONDITION_AIRED
    //deletes the upcoming log from database
    //writes upcoming log(now with CONDITION_AIRED) to database, by creating or updating existing
    public static void transferUpcomingLogToReleased(Context context, String mediaId) {
        SeriesLogsDao logsDao = AppDatabase.getLocalDatabase(context).seriesLogsDao();

        SeriesLog upcomingLog = logsDao.getLog(mediaId, CONDITION_UPCOMING);
        if (upcomingLog != null) {
            logsDao.deleteMediaLog(upcomingLog);
            upcomingLog.condition = CONDITION_AIRED;

            SeriesLog lastAiredLog = logsDao.getLog(mediaId, CONDITION_AIRED);
            if (lastAiredLog != null) {
                logsDao.updateLog(upcomingLog);

            } else {
                logsDao.addMediaLog(upcomingLog);
            }
        }
    }

    private void deleteTrackedMedia() {
        List<SeriesLog> mediaLogs =
                mDatabase.seriesLogsDao().getAllLogsWithMediaIdAlt(newMediaData.getId());

        for (SeriesLog mediaLog : mediaLogs) {
            LogUpdateReceiver.cancelLogUpdateAlarm(getApplicationContext(), mediaLog);
        }
    }
}