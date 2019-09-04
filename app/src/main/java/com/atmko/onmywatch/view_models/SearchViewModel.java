package com.atmko.onmywatch.view_models;//package com.upkipp.onmywatch.view_models;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class SearchViewModel extends AndroidViewModel {
    private static final String TAG = SearchViewModel.class.getSimpleName();

    private int mCurrentTabPosition;

    public SearchViewModel(@NonNull Application application) {
        super(application);

        Log.d(TAG, "starting tab position tracking");
        mCurrentTabPosition = 0;
    }

    public int getCurrentTabPosition() {
        return mCurrentTabPosition;
    }

    public void setCurrentTabPosition(int currentTabPosition) {
        this.mCurrentTabPosition = currentTabPosition;
    }
}
