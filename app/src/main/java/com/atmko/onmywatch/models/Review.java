package com.atmko.onmywatch.models;

import org.parceler.Parcel;

@Parcel
public class Review {
    private String mAuthor;
    private String mContent;

    public Review() {
    }

    public Review(String mAuthor, String mContent) {
        this.mAuthor = mAuthor;
        this.mContent = mContent;
    }

    public String getAuthor() {
        if (mAuthor == null) {
            return "";

        } else  {
            return mAuthor;
        }
    }

    public String getContent() {
        if (mContent == null) {
            return "";

        } else  {
            return mContent;
        }
    }
}