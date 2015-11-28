package com.flowzr.activity;

import android.app.Activity;
import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.flowzr.R;
import com.flowzr.model.Attribute;

public class MyDrawerLayout extends DrawerLayout {

    private Context mContext;

    public MyDrawerLayout(Context context) {
        super(context);
        mContext=context;
    }

    public MyDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext=context;
    }

    public MyDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext=context;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        ((Activity)mContext).onTouchEvent(event);
        return false;
    }
}
