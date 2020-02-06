/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.custom_views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.AutoCompleteTextView;

public class SuperEditText extends AutoCompleteTextView {
    private OnKeyBoardDismissListener mKeyBoardDismissListener;
    int activeTextIndex;

    public SuperEditText(Context context) {
        super(context);
    }

    public void setKeyBoardDismissListener(OnKeyBoardDismissListener keyBoardDismissListener) {
        mKeyBoardDismissListener = keyBoardDismissListener;
    }

    public interface OnKeyBoardDismissListener {
        void onKeyBoardDismiss();
    }

    public SuperEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SuperEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && mKeyBoardDismissListener != null) {

            mKeyBoardDismissListener.onKeyBoardDismiss();

        }

        return super.onKeyPreIme(keyCode, event);
    }

    //get the text touching cursor
    public String getActiveText() {
        int cursorIndex = getSelectionStart() > 0? getSelectionStart()-1 : 0;
        String[] splitText = getText().toString().split(" ");

        String activeText = "";

        int indexTotal = 0;

        for (int i = 0; i < splitText.length ; i++) {
            String word = splitText[i];
            indexTotal += word.length();

            if (cursorIndex <= indexTotal) {
                activeText = word;

                //define active index
                activeTextIndex = i;
                break;
            }

            //+= 1 represents the space after each word)
            indexTotal += 1;
        }

        return activeText;
    }
}
