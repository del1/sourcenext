package com.mobiroo.n.sourcenextcorporation.agent.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.android.gms.maps.MapView;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.ParkingHistoryFragment;

public class CustomViewPager extends ViewPager{

    public CustomViewPager(Context context) {

        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {

        if(v instanceof MapView){
            return true;
        }
        return super.canScroll(v, checkV, dx, x, y);
    }
}
