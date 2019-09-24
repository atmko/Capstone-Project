package com.atmko.stack;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.SparseArray;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class Stack extends RecyclerView.OnScrollListener {
    //stack operation identifiers
    public static final int GO_DOWN_ONE_BLOCK = 1;
    public static final int GO_UP_ONE_BLOCK = 2;

    private int firstPage;
    private int totalPages;
    private int blockLimit;
    private Object mPreloadObject;
    private PagingBlockTemplate pagingBlockTemplate;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private SparseArray<PagingBlock> pagingBlockMap;
    private boolean mIsIdle;

    public Stack(boolean pageZeroStart, int blockLimit, PagingBlockTemplate pagingBlockTemplate,
                 Object preloadObject, RecyclerView recyclerView, RecyclerView.Adapter adapter) {

        this.firstPage = pageZeroStart ? 0 : 1;
        this.blockLimit = blockLimit;
        this.pagingBlockTemplate = pagingBlockTemplate;
        this.mPreloadObject = preloadObject;
        this.recyclerView = recyclerView;
        this.adapter = adapter;
        this.pagingBlockMap = new SparseArray<>();
        mIsIdle = true;
    }


    private List getAdapterData() {
        List dataList = null;
        try {
            Class adapterClass = Class.forName(adapter.getClass().getName());
            Method getAdapterData = adapterClass.getMethod("getAdapterData");
            dataList = (List) getAdapterData.invoke(adapter);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new Error ("method \"getAdapterData()\" not found\n");
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return dataList;
    }

    private boolean isAdapterEmpty() {
        return getAdapterData().size() == 0;
    }

    public SparseArray<PagingBlock> getPagingBlockMap() {
        return pagingBlockMap;
    }

    public boolean isIdle() {
        return mIsIdle;
    }

    public int[] saveBlockStructure() {
        int[] blockIndexRange = new int[2];

        blockIndexRange[0] = pagingBlockMap.keyAt(0);

        int lastBlockNumber = pagingBlockMap.keyAt(pagingBlockMap.size() - 1);
        //index range[1] not inclusive in operation.
        //therefore + 1 is added to include last block
        int rangeAdjustment = lastBlockNumber + 1;

        blockIndexRange[1] = rangeAdjustment;

        return blockIndexRange;
    }

    public void restorePagingBlockStructure(int[] blockIndexRange) {
        //index range[1] not inclusive in operation.
        //e.g range of: 1, 4 generates 3 values (1, 2, 3)
        int iterationSize = blockIndexRange[1] - blockIndexRange[0];

        for (int index = 0; index < iterationSize; index++) {
            int blockIndex = blockIndexRange[0] + index;
            PagingBlock pagingBlock = new PagingBlock(getFirstPage(), blockIndex, pagingBlockTemplate.getBlockPageCapacity());

            pagingBlockMap.put(blockIndex, pagingBlock);
        }
    }

    private int getTotalPages() {
        return this.totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    //initial setup paging block
    public void initialize() {
        if (pagingBlockTemplate.createPageLoader.onCustomScrollCondition()) {
            //stack is not idle
            mIsIdle = false;

            //clear values
            pagingBlockMap.clear();
            getAdapterData().clear();

            adapter.notifyDataSetChanged();
            totalPages = 0;

            //load new block
            loadNextBlock(0);

        }
    }

    public int getFirstPage() {
        return firstPage;
    }

    public void setIsFrozen(boolean isFrozen) {
        this.recyclerView.setLayoutFrozen(isFrozen);

    }

    //this method is called as many times as the value of blockPageCapacity
    public void stackPage(int blockNumber, int pageNumber, List dataList, int stackOperation) {
        //get blocks for stacking
        PagingBlock pagingBlock = pagingBlockMap.get(blockNumber);

        //if data list is null
        //define data list as a list of preload objects so they can be stacked without incident
        if (dataList == null) {
            dataList = new ArrayList();
            for (int i = 0; i < pagingBlockTemplate.pageCapacity ; i++) {
                dataList.add(mPreloadObject);
            }
        }

        //do not stack page if null
        //user has scrolled to a point where original requesting paging block has been removed
        //this is caused by scrolling quickly where a more recent paging block has replaced an older...
        //one before its results could be stacked
        if (pagingBlock != null) {
            //set data lists
            pagingBlock.setDataListByPage(pageNumber, dataList);

        }else {
            return;

        }

        //if we're moving down a block
        if (stackOperation == GO_DOWN_ONE_BLOCK) {
            addItemsIntoAdapter(pagingBlock, pageNumber, dataList);

            //if we're moving up a block
        } else if (stackOperation == GO_UP_ONE_BLOCK){
            addItemsIntoAdapter(pagingBlock, pageNumber, dataList);
        }
            
        mIsIdle = true;
    }

    private void addItemsIntoAdapter(PagingBlock pagingBlock, int pageNumber, List dataList) {
        int pagingBlockIndex = pagingBlockMap.indexOfValue(pagingBlock);

        //get index of the paging block's first item its the first page
        int blockStartingIndex = pagingBlockIndex
                * pagingBlockTemplate.pageCapacity
                * pagingBlockTemplate.blockPageCapacity;

        //use page number to find the index the page relative to existing pages currently in the block
        int pageIndex = pageNumber - pagingBlock.getFirstPageInBlock();
        //define first adapter position loop should begin.
        int firstInsertPosition = blockStartingIndex + (pageIndex * pagingBlockTemplate.pageCapacity);

        for (int index = 0; index < dataList.size(); index++) {
            //increase value with each iteration through zero index
            int currentInsertPosition = firstInsertPosition + index;

            //replace preload object and update adapter
            getAdapterData().remove(currentInsertPosition);
            getAdapterData().add(currentInsertPosition, dataList.get(index));
        }

        adapter.notifyItemRangeChanged(firstInsertPosition, dataList.size());

        //TODO add method that remove extra data BEFORE items are stacked to avoid late clean up
        //if incoming data < page capacity, remove extraneous empty data
        if (dataList.size() < pagingBlockTemplate.pageCapacity) {
            int correctionDifference = pagingBlockTemplate.pageCapacity - dataList.size();

            for (int i = 0; i < correctionDifference; i++) {
                getAdapterData().remove(getAdapterData().size() - 1);
                adapter.notifyItemRemoved(getAdapterData().size() - 1);

            }
        }

        //TODO data still in paging data now useless now that its been added to adapter.
    }

    private void removeTopBlock() {
        //stack is not idle
        mIsIdle = false;

        int firstKey = pagingBlockMap.keyAt(0);
        int listSize = pagingBlockMap.get(firstKey).getFullDataCount();

        //loop through length of block
        for (int index = 0; index < listSize; index++) {
            //remove top item in adapter
            getAdapterData().remove(0);
            //notify change
            adapter.notifyItemRemoved(0);
        }

        pagingBlockMap.remove(firstKey);

        //stack is idle
        mIsIdle = true;
    }

    private void removeBottomBlock() {
        //stack is not idle
        mIsIdle = false;

        int lastKey = pagingBlockMap.keyAt(pagingBlockMap.size() - 1);
        int listSize = pagingBlockMap.get(lastKey).getFullDataCount();

        //loop through length of block
        for (int index = 0; index < listSize; index++) {
            //remove bottom item in adapter
            getAdapterData().remove(getAdapterData().size() - 1);
            //notify change
            adapter.notifyItemRemoved(getAdapterData().size() - 1);
        }

        pagingBlockMap.remove(lastKey);

        //stack is idle
        mIsIdle = true;
    }

    private void addTopBlock() {
        //stack is not idle
        mIsIdle = false;

        mIsIdle = false;int firstKey = pagingBlockMap.keyAt(0);
        int newKey = firstKey - 1;

        loadPreviousBlock(newKey);
    }

    private void loadPreviousBlock(int blockNumber) {
        //initialize paging block
        PagingBlock pagingBlock =
                new PagingBlock(getFirstPage(), blockNumber, pagingBlockTemplate.blockPageCapacity);

        //add block to list
        pagingBlockMap.put(blockNumber, pagingBlock);

        //define first targetPage
        int targetPage = pagingBlock.getFirstPageInBlock();

        //add placeholder objects till real stacking begins
        for (int i = 0; i < pagingBlockTemplate.getBlockPageCapacity(); i++) {
            prestackPageBackWards();
        }

        //iterate through block page capacity
        for (int i = 0; i < pagingBlockTemplate.getBlockPageCapacity(); i++) {
            //fetch page data
            pagingBlockTemplate.createPageLoader.onPageStartReached(blockNumber, targetPage);

            //increase targetPage value
            targetPage += 1;
        }
    }

    private void prestackPageBackWards() {
        for (int i = pagingBlockTemplate.pageCapacity - 1; i >= 0; i--) {
            //add item to front
            getAdapterData().add(0, mPreloadObject);

        }

        adapter.notifyItemRangeInserted(0, pagingBlockTemplate.pageCapacity);
    }

    private void addBottomBlock() {
        //stack is not idle
        mIsIdle = false;

        int lastKey = pagingBlockMap.keyAt(pagingBlockMap.size() - 1);
        int newKey = lastKey + 1;

        loadNextBlock(newKey);
    }

    private void loadNextBlock(int blockNumber) {
        //initialize paging block
        PagingBlock pagingBlock =
                new PagingBlock(getFirstPage(), blockNumber, pagingBlockTemplate.blockPageCapacity);

        //add block to list
        pagingBlockMap.put(blockNumber, pagingBlock);

        //define first targetPage
        int targetPage = pagingBlock.getFirstPageInBlock();

        //add placeholder objects till real stacking begins
        for (int i = 0; i < pagingBlockTemplate.getBlockPageCapacity(); i++) {
            prestackPageForwards();
        }

        //TODO if number of pages ahead is < getBlockPageCapacity then extra api queries are a wasted
        //iterate through block page capacity
        for (int i = 0; i < pagingBlockTemplate.getBlockPageCapacity(); i++) {
            //fetch page data
            pagingBlockTemplate.createPageLoader.onPageEndReached(blockNumber, targetPage);

            //increase targetPage value
            targetPage += 1;
        }
    }

    private void prestackPageForwards() {
        for (int i = 0; i < pagingBlockTemplate.pageCapacity; i++) {
            //add item to end
            getAdapterData().add(mPreloadObject);
        }

        adapter.notifyItemRangeInserted(
                (getAdapterData().size()-1) - (pagingBlockTemplate.pageCapacity-1),
                pagingBlockTemplate.pageCapacity);
    }

    private int getFirstPageInStack() throws IndexOutOfBoundsException {
        if (getPagingBlockMap().size() == 0) {
            throw new IndexOutOfBoundsException();
        }

        //get key of first block in stack
        int topPagingBlockKey = pagingBlockMap.keyAt(0);
        //get top block using key
        PagingBlock topPagingBlock = pagingBlockMap.get(topPagingBlockKey);

        return topPagingBlock.getFirstPageInBlock();
    }

    private int getLastPageInStack() throws IndexOutOfBoundsException {
        if (getPagingBlockMap().size() == 0) {
            throw new IndexOutOfBoundsException();
        }

        //get key of last block in stack
        int bottomPagingBlockKey = pagingBlockMap.keyAt(pagingBlockMap.size() - 1);
        //get bottom block using key
        PagingBlock bottomPagingBlock = pagingBlockMap.get(bottomPagingBlockKey);

        return bottomPagingBlock.getLastPageInBlock();
    }

    public static class PagingBlockTemplate {
        OnCreatePageLoader createPageLoader;
        private int pageCapacity;
        private int blockPageCapacity;

        public PagingBlockTemplate(OnCreatePageLoader createPageLoader, int pageCapacity,
                                   int blockPageCapacity) {
            this.createPageLoader = createPageLoader;
            this.pageCapacity = pageCapacity;
            this.blockPageCapacity = blockPageCapacity;
        }

        private int getBlockPageCapacity() {
            return blockPageCapacity;
        }

        public interface OnCreatePageLoader {
            void onPageEndReached(int blockNumber, int targetPage);
            void onPageStartReached(int blockNumber, int targetPage);
            boolean onCustomScrollCondition();
        }
    }

    private boolean atListEnd;
    private boolean atListStart;

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        @SuppressWarnings("ConstantConditions")
        //error caught in throw when invoking findLastCompletelyVisibleItemPosition()
        int lastShown = ((GridLayoutManager)recyclerView.getLayoutManager())
                .findLastVisibleItemPosition();

        //error caught in throw when invoking findFirstCompletelyVisibleItemPosition()
        int firstShownIndex = ((GridLayoutManager)recyclerView.getLayoutManager())
                .findFirstVisibleItemPosition();

        //isLastItem makes sure we are at the end of list
        boolean isLastItem = lastShown == adapter.getItemCount() - 1;
        //isFirstItem makes sure we are at the start of list
        boolean isFirstItem = firstShownIndex == 0;

        int availablePages = getTotalPages();

        //!emptyAdapter prevents unwanted page loads when clearing adapter data...
        // ...because lastItem is considered true
        boolean emptyAdapter = isAdapterEmpty();

        try {//catches error if paging block maps's size is 0
            boolean morePagesAhead = getLastPageInStack() < availablePages;

            //if at lastItem && if morePagesAhead && if adapter not empty
            atListEnd = isLastItem && morePagesAhead && !emptyAdapter;

        } catch (IndexOutOfBoundsException e) {
            atListEnd = false;
        }

        try {//catches error if paging block maps's size is 0
            boolean morePagesBehind = getFirstPageInStack() > getFirstPage();

            //if at firstItem && if morePagesBehind && if adapter not empty
            atListStart = isFirstItem && morePagesBehind && !emptyAdapter;

        } catch (IndexOutOfBoundsException e) {
            atListStart = false;
        }
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        if (atListEnd && newState == RecyclerView.SCROLL_STATE_IDLE
                && pagingBlockTemplate.createPageLoader.onCustomScrollCondition()) {
            if (pagingBlockMap.size() == blockLimit) {
                removeTopBlock();
            }

            addBottomBlock();

        } else if (atListStart && newState == RecyclerView.SCROLL_STATE_IDLE
                && pagingBlockTemplate.createPageLoader.onCustomScrollCondition()) {
            if (pagingBlockMap.size() == blockLimit) {
                removeBottomBlock();
            }

            addTopBlock();
        }
    }
}