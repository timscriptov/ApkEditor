package com.gmail.heagoo.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

//public class DynamicExpandListView {
//}

public class DynamicExpandListView extends ListView implements OnScrollListener {

    private int scrollState;
    private AdapterWrapper wrapper;
    private int initListSize = 30;

    public DynamicExpandListView(Context context) {
        super(context);

        this.setOnScrollListener(this);
    }
    public DynamicExpandListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnScrollListener(this);
    }
    public DynamicExpandListView(Context context, AttributeSet attrs,
                                 int defStyle) {
        super(context, attrs, defStyle);
        this.setOnScrollListener(this);
    }

    public void dataChanged() {
        if (wrapper != null) {
            wrapper.notifyDataSetChanged();
        }
    }

    public void setInitListSize(int initSize) {
        this.initListSize = initSize;
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        wrapper = new AdapterWrapper(adapter, initListSize);
        super.setAdapter(wrapper);
    }

//	@Override
//	public ListAdapter getAdapter() {
//		if (wrapper != null) {
//			return wrapper.adapterRef.get();
//		}
//		return null;
//	}

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        this.scrollState = scrollState;
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {

        // Log.d("TEST",
        // String.format(
        // "firstVisibleItem=%d visibleItemCount=%d totalItemCount=%d scrollState=%d!",
        // firstVisibleItem, visibleItemCount, totalItemCount,
        // scrollState));
        if (firstVisibleItem + visibleItemCount >= totalItemCount) {
            // && this.scrollState ==
            // AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
            if (wrapper != null) {
                wrapper.loadMore();
            }
        }
    }

    private static class AdapterWrapper extends BaseAdapter {
        // How may items want to show, but the actual item number could be
        // different
        private int pageItems;
        private int itemSteps = 30;
        private boolean loadEnded = false;
        private ListAdapter adapter;
        // The really showing count
        private int count;

        // private Object lock = new Object();

        public AdapterWrapper(ListAdapter adapter, int initListSize) {
            //this.adapterRef = new WeakReference<ListAdapter>(adapter);
            this.adapter = adapter;
            this.pageItems = initListSize;
            if (adapter.getCount() > pageItems) {
                this.count = pageItems;
                loadEnded = false;
            } else {
                this.count = adapter.getCount();
                loadEnded = true;
            }
        }

        public void loadMore() {
            // Log.d("TEST", Thread.currentThread().toString() +
            // ": loadMore called!");
            if (!loadEnded) {
                pageItems += itemSteps;
                if (adapter.getCount() > pageItems) {
                    this.loadEnded = false;
                    this.count = pageItems;
                } else {
                    this.loadEnded = true;
                    this.count = adapter.getCount();
                }
                this.notifyDataSetChanged();
            } else {
                // Log.d("TEST", "loadEnded=true");
            }
        }

        @Override
        public int getCount() {
            // Log.d("TEST", Thread.currentThread().toString() +
            // ": getCount called!");

            return count;
        }

        @Override
        public Object getItem(int arg0) {
            return adapter.getItem(arg0);
        }

        @Override
        public long getItemId(int arg0) {
            return adapter.getItemId(arg0);
        }

        @Override
        public View getView(int arg0, View arg1, ViewGroup arg2) {
            return adapter.getView(arg0, arg1, arg2);
        }
    }
}