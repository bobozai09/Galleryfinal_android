package com.ling.bobozai.galleryfinal_android.unitls;import android.app.Activity;import android.widget.AbsListView;import java.lang.ref.WeakReference;/** * Created by boboz on 2018/3/24. */public abstract class MyPauseOnScrollListener implements AbsListView.OnScrollListener {    public Activity getActivity(){        return activityWeakReference.get();    }    private final boolean pauseOnScroll;    private final boolean pauseOnFling;    private final AbsListView.OnScrollListener externalListener;    WeakReference<Activity> activityWeakReference;    public MyPauseOnScrollListener(Activity activity, boolean pauseOnScroll, boolean pauseOnFling) {        this(activity,pauseOnScroll, pauseOnFling, null);    }    protected MyPauseOnScrollListener(Activity activity, boolean pauseOnScroll, boolean pauseOnFling, AbsListView.OnScrollListener customListener) {        this.pauseOnScroll = pauseOnScroll;        this.pauseOnFling = pauseOnFling;        externalListener = customListener;        activityWeakReference=new WeakReference<Activity>(activity);    }    @Override    public void onScrollStateChanged(AbsListView view, int scrollState) {        switch (scrollState) {            case MyPauseOnScrollListener.SCROLL_STATE_IDLE:                resume();                break;            case MyPauseOnScrollListener.SCROLL_STATE_TOUCH_SCROLL:                if (pauseOnScroll) {                    pause();                }                break;            case MyPauseOnScrollListener.SCROLL_STATE_FLING:                if (pauseOnFling) {                    pause();                }                break;        }        if (externalListener != null) {            externalListener.onScrollStateChanged(view, scrollState);        }    }    @Override    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {        if (externalListener != null) {            externalListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);        }    }    public abstract void resume();    public abstract void pause();}