/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.stack;

import android.util.SparseArray;

import java.util.List;

class PagingBlock {
    private int mFirstPage;
    private int mBlockIndex;
    private int mBlockPageCapacity;
    private SparseArray<List> mPageList;

    PagingBlock(int firstPage, int blockIndex, int blockPageCapacity) {
        this.mFirstPage = firstPage;
        this.mBlockIndex = blockIndex;
        this.mBlockPageCapacity = blockPageCapacity;
        this.mPageList = new SparseArray<>();
    }

    int getBlockPageCapacity() {
        return mBlockPageCapacity;
    }

    List getDataListByPage(int page) {
        return mPageList.get(page);
    }

    SparseArray<List> getPageList() {
        return mPageList;
    }

    void setDataListByPage(int page, List dataList) {
        mPageList.put(page, dataList);
    }

    int getFullDataCount() {
        int count = 0;

        //iterate through list
        for (int index = 0; index < mPageList.size(); index++) {
            int page = mPageList.keyAt(index);

            try {
                //add to count
                count += mPageList.get(page).size();

                //catch error if items in page not yet set
            } catch (NullPointerException e) {
                count += 0;
            }
        }

        return count;
    }

    int getFirstPageInBlock() {
        //define first page index
        return  mFirstPage + (mBlockPageCapacity * mBlockIndex);
    }

    int getLastPageInBlock() {
        return getFirstPageInBlock() + (mBlockPageCapacity - 1);
    }
}