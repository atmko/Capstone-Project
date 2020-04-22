package com.atmko.onmywatch.adapters;

import android.content.res.Resources;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.fragment.app.Fragment;

import com.atmko.onmywatch.R;
import com.atmko.onmywatch.utils.api_utils.ApiConstants;

public class CustomParams {
    public static int[] getSearchParams(Fragment fragment) {
        DisplayMetrics displayDimensions = Resources.getSystem().getDisplayMetrics();

        int masterRatio;
        int detailRatio;

        int imageColumnSpan;

        //get layout weights
        masterRatio = fragment.getResources().getInteger(R.integer.master_fragment_layout_weight);
        detailRatio = fragment.getResources().getInteger(R.integer.detail_fragment_layout_weight);

        imageColumnSpan = fragment.getResources().getInteger(R.integer.search_column_span);

        //get weight total
        int weightTotal = masterRatio + detailRatio;

        //get search fragment pixel width
        int searchFragmentPixelWidth =
                displayDimensions.widthPixels * masterRatio/weightTotal;

        //get single image pixel width: (searchFragmentPixelWidth/num of columns)
        int singleImgPixelWidth =
                searchFragmentPixelWidth / imageColumnSpan;

        //convert spacing between images to pixels
        int imageSpacing = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                fragment.getResources().getInteger(R.integer.search_image_spacing),
                fragment.getResources().getDisplayMetrics());

        //new image width now that spacing is applied
        int adjustedViewWidth = singleImgPixelWidth - imageSpacing;

        //get poster height
        long posterHeight = Math.round(adjustedViewWidth * ApiConstants.POSTER_ASPECT_RATIO);

        int[] params = new int[2];

        //set layout params
        params[0] = adjustedViewWidth;
        params[1] = (int) posterHeight;

        return params;
    }

    public static int[] getDetailExtrasParams(Fragment fragment) {
        DisplayMetrics displayDimensions = Resources.getSystem().getDisplayMetrics();

        int masterRatio;
        int detailRatio;

        int imageColumnSpan;

        if (fragment.getResources().getBoolean(R.bool.isPhone)) {
            //get phone layout weights
            masterRatio = fragment.getResources().getInteger(R.integer.details_main_layout_weight);
            detailRatio = fragment.getResources().getInteger(R.integer.details_extras_layout_weight);

        } else {
            //get tablet layout weights
            masterRatio = fragment.getResources().getInteger(R.integer.master_fragment_layout_weight);
            detailRatio = fragment.getResources().getInteger(R.integer.detail_fragment_layout_weight);
        }

        imageColumnSpan = fragment.getResources().getInteger(R.integer.detail_extras_column_span);

        //get weight total
        int weightTotal = masterRatio + detailRatio;

        //get search fragment pixel width
        int searchFragmentPixelWidth =
                displayDimensions.widthPixels * masterRatio/weightTotal;

        //get single image pixel width: (searchFragmentPixelWidth/num of columns)
        int singleImgPixelWidth =
                searchFragmentPixelWidth / imageColumnSpan;

        //convert spacing between images to pixels
        int imageSpacing = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                fragment.getResources().getInteger(R.integer.search_image_spacing),
                fragment.getResources().getDisplayMetrics());

        //new image width now that spacing is applied
        int adjustedViewWidth = singleImgPixelWidth - imageSpacing;

        //get poster height
        long posterHeight = Math.round(adjustedViewWidth * ApiConstants.POSTER_ASPECT_RATIO);

        int[] params = new int[2];

        //set layout params
        params[0] = adjustedViewWidth;
        params[1] = (int) posterHeight;

        return params;
    }

    //configures width, height and margins of home list display container
    public static int[] getSpotlightParams(Fragment fragment) {
        if (fragment.getActivity() == null) return new int[0];

        DisplayMetrics displayDimensions = Resources.getSystem().getDisplayMetrics();

        //how much spotlight height is compared to entire screen height
        double imageHeightFactor;

        if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            imageHeightFactor = fragment.getResources().getFloat(R.dimen.spotlight_height_scale_factor);

        } else {
            TypedValue outValue = new TypedValue();
            fragment.getResources().getValue(R.dimen.spotlight_height_scale_factor, outValue, true);
            imageHeightFactor = outValue.getFloat();
        }

        int screenHeight = displayDimensions.heightPixels;

        //get single image pixel width: (searchFragmentPixelWidth * height factor)
        int singleImgPixelHeight = ((Long) Math.round(screenHeight * imageHeightFactor)).intValue();
        int singleImgPixelWidth =
                ((Long) Math.round(singleImgPixelHeight / ApiConstants.POSTER_ASPECT_RATIO)).intValue();

        int[] params = new int[2];

        //set layout params
        params[0] = singleImgPixelWidth;
        params[1] = (int) singleImgPixelHeight;

        return params;
    }
}
