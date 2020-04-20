/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.onmywatch.custom_views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;

public class SuperEditText extends androidx.appcompat.widget.AppCompatAutoCompleteTextView {
    private OnKeyBoardDismissListener mKeyBoardDismissListener;
    private int activeTextIndex;

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

    @Override
    protected void performFiltering(CharSequence text, int keyCode) {
        super.performFiltering(getActiveText(), keyCode);
    }

    @Override
    protected void replaceText(CharSequence text) {
        //replace active text entry
        String oldText = getText().toString();
        String[] oldTextSplit = oldText.split(" ");

        //insert new text
        oldTextSplit[activeTextIndex] = text.toString();

        int indexTotal = 0;

        //combine old text split back to string
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < oldTextSplit.length; i++) {
            String word = oldTextSplit[i];
            builder.append(word);
            builder.append(" ");

            //count index to later set cursor position (+= 1 represents the space after each word)
            if (i <= activeTextIndex) {
                indexTotal += word.length();
                indexTotal += 1;
            }
        }

        super.replaceText(builder.toString());

        //set cursor to end of text being replaced
        setSelection(indexTotal);
    }
}
