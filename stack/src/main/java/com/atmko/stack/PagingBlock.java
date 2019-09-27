/*
 * Copyright (C) 2019 Aayat Mimiko
 */

package com.atmko.stack;

import android.util.SparseArray;

import java.util.List;

public class PagingBlock {
    private int firstPage;
    private int blockIndex;
    private int blockPageCapacity;
    private SparseArray<List> pageList;

    PagingBlock(int firstPage, int blockIndex, int blockPageCapacity) {
        this.firstPage = firstPage;
        this.blockIndex = blockIndex;
        this.blockPageCapacity = blockPageCapacity;
        this.pageList = new SparseArray<>();
    }

    int getBlockPageCapacity() {
        return blockPageCapacity;
    }

    List getDataListByPage(int page) {
        return pageList.get(page);
    }

    SparseArray<List> getPageList() {
        return pageList;
    }

    void setDataListByPage(int page, List dataList) {
        pageList.put(page, dataList);
    }

    int getFullDataCount() {
        int count = 0;

        //iterate through list
        for (int index = 0; index < pageList.size(); index++) {
            int page = pageList.keyAt(index);

            try {
                //add to count
                count += pageList.get(page).size();

                //catch error if items in page not yet set
            } catch (NullPointerException e) {
                count += 0;
            }
        }

        return count;
    }

    int getFirstPageInBlock() {
        //define first page index
        return  firstPage + (blockPageCapacity * blockIndex);
    }

    int getLastPageInBlock() {
        return getFirstPageInBlock() + (blockPageCapacity - 1);
    }
}