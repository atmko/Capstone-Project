/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.custom_views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class SuperEditText extends EditText {
    private OnKeyBoardDismissListener mKeyBoardDismissListener;

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
}
