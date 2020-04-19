package com.coolweather.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class PocketSwipeRefreshLayout extends SwipeRefreshLayout {
    private float mStartX = 0;
    private float mStartY = 0;

    //记录Viewpager是否被拖拉
    private boolean mIsVpDrag;
    private final int mTouchSlop;
    public PocketSwipeRefreshLayout(@NonNull Context context) {
        super(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public PocketSwipeRefreshLayout(@NonNull Context context, AttributeSet attrs) {
        super(context,attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action){
            case MotionEvent.ACTION_DOWN:
                mStartX = ev.getX();
                mStartY = ev.getY();
                mIsVpDrag = false;
                break;
            case MotionEvent.ACTION_MOVE:
                //如果viewpager正在拖拽，则不拦截viewpager事件
                if (mIsVpDrag){
                    return false;
                }
                float x = ev.getX();
                float y = ev.getY();
                float distanceX = Math.abs(x - mStartX);
                float distanceY = Math.abs(y - mStartY);
                //如果滑动x位移大于y，不拦截viewpager事件
                if (distanceX > mTouchSlop && distanceX>distanceY){
                    mIsVpDrag = true;
                    return false;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsVpDrag = true;
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(ev);

    }
}
