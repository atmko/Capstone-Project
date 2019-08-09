//package com.upkipp.onmywatch.view_models;
//
//import android.content.Context;
//
//import androidx.lifecycle.LiveData;
//import androidx.lifecycle.ViewModel;
//
//import com.upkipp.onmywatch.database.AppDatabase;
//import com.upkipp.onmywatch.models.ListModel;
//import com.upkipp.onmywatch.models.MediaData;
//import com.upkipp.onmywatch.models.MovieData;
//import com.upkipp.onmywatch.models.SeriesData;
//
//import java.util.List;
//
//public class ListsViewModel extends ViewModel {
//    private LiveData<List<MovieData>> moviesNoneList;
//    private LiveData<List<MovieData>> moviesToWatchList;
//    private LiveData<List<MovieData>> moviesWatchingList;
//    private LiveData<List<MovieData>> moviesWatchedList;
//    private LiveData<List<MovieData>> moviesDroppedList;
//    private LiveData<List<MovieData>> moviesOtherList;
//
//    private LiveData<List<SeriesData>> seriesNoneList;
//    private LiveData<List<SeriesData>> seriesToWatchList;
//    private LiveData<List<SeriesData>> seriesWatchingList;
//    private LiveData<List<SeriesData>> seriesWatchedList;
//    private LiveData<List<SeriesData>> seriesDroppedList;
//    private LiveData<List<SeriesData>> seriesOtherList;
//
//    private LiveData<List<ListModel>> userLists;
//
//    public ListsViewModel(){
//        loadMoviesWatchList();
//        loadSeriesWatchList();
//    }
//
//    public LiveData<Integer> loadMoviesWatchList(AppDatabase database, int watchStatus, Context context) {
//
//        switch (watchStatus) {
//            case MediaData.WATCH_STATUS_NONE:
//                moviesNoneList = database.movieDataDao()
//                        .getMoviesByWatchStatus(MediaData.WATCH_STATUS_NONE);
//
//            case MediaData.WATCH_STATUS_TO_WATCH:
//                moviesToWatchList = database.movieDataDao()
//                        .getMoviesByWatchStatus(MediaData.WATCH_STATUS_TO_WATCH);
//
//            case MediaData.WATCH_STATUS_WATCHING:
//                moviesWatchingList = database.movieDataDao()
//                        .getMoviesByWatchStatus(MediaData.WATCH_STATUS_WATCHING);
//
//            case MediaData.WATCH_STATUS_WATCHED:
//                moviesWatchedList = database.movieDataDao()
//                        .getMoviesByWatchStatus(MediaData.WATCH_STATUS_WATCHED);
//
//            case MediaData.WATCH_STATUS_DROPPED:
//                moviesDroppedList = database.movieDataDao()
//                        .getMoviesByWatchStatus(MediaData.WATCH_STATUS_DROPPED);
//
//            case MediaData.WATCH_STATUS_OTHER:
//                moviesDroppedList = database.movieDataDao()
//                        .getMoviesByWatchStatus(MediaData.WATCH_STATUS_OTHER);
//
//        }
//    }
//
//    public LiveData<Integer> loadSeriesWatchList(AppDatabase database, int watchStatus, Context context) {
//        switch (watchStatus) {
//            case MediaData.WATCH_STATUS_NONE:
//                seriesNoneList = database.seriesDataDao()
//                        .getSeriesByWatchStatus(MediaData.WATCH_STATUS_NONE);
//
//            case MediaData.WATCH_STATUS_TO_WATCH:
//                seriesToWatchList = database.seriesDataDao()
//                        .getSeriesByWatchStatus(MediaData.WATCH_STATUS_TO_WATCH);
//
//            case MediaData.WATCH_STATUS_WATCHING:
//                seriesWatchingList = database.seriesDataDao()
//                        .getSeriesByWatchStatus(MediaData.WATCH_STATUS_WATCHING);
//
//            case MediaData.WATCH_STATUS_WATCHED:
//                seriesWatchedList = database.seriesDataDao()
//                        .getSeriesByWatchStatus(MediaData.WATCH_STATUS_WATCHED);
//
//            case MediaData.WATCH_STATUS_DROPPED:
//                seriesDroppedList = database.seriesDataDao()
//                        .getSeriesByWatchStatus(MediaData.WATCH_STATUS_DROPPED);
//
//            case MediaData.WATCH_STATUS_OTHER:
//                seriesOtherList = database.seriesDataDao()
//                        .getSeriesByWatchStatus(MediaData.WATCH_STATUS_OTHER);
//
//        }
//    }
//
//    private void loadUserLists() {
//            //if media data is movie
//            if (mMediaType == MasterActivity.MEDIA_TYPE_MOVIE) {
//                final LiveData<List<MovieData>> moviesInList = mDatabase.movieDataRecordsDao().getAllMoviesInList(mListName);
//                moviesInList.observe(getActivity(), new Observer<List<MovieData>>() {
//                    @Override
//                    public void onChanged(List<MovieData> movieDataList) {
//                        mMediaDataAdapter.getAdapterData().clear();
//                        mMediaDataAdapter.addAdapterData(movieDataList);
//
//                    }
//                });
//
//                //if media data is series
//            } else if (mMediaType == MasterActivity.MEDIA_TYPE_SERIES) {
//                final LiveData<List<SeriesData>> seriesInList = mDatabase.seriesDataRecordsDao().getAllSeriesInList(mListName);
//                seriesInList.observe(getActivity(), new Observer<List<SeriesData>>() {
//                    @Override
//                    public void onChanged(List<SeriesData> seriesDataList) {
//                        mMediaDataAdapter.getAdapterData().clear();
//                        mMediaDataAdapter.addAdapterData(seriesDataList);
//
//                    }
//                });
//
//                Log.d(FRAGMENT_KEY, "update user list");
//            }
//        }
//    }
//}
