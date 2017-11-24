package com.mobiroo.n.sourcenextcorporation.tagstand.util;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class TextViewRobotoLight extends TextView {
	
    public TextViewRobotoLight(Context context) {
        super(context);
        setCustomFont(context, "Roboto-Light.ttf");
    }

    public TextViewRobotoLight(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomFont(context, "Roboto-Light.ttf");
    }

    public TextViewRobotoLight(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setCustomFont(context, "Roboto-Light.ttf");
    }

    public boolean setCustomFont(Context ctx, String asset) {
        Typeface tf = null;
        try {
        tf = Typeface.createFromAsset(ctx.getAssets(), asset);  
        } catch (Exception e) {
            Log.e("AGENT", "Could not get typeface: "+e.getMessage());
            return false;
        }

        setTypeface(tf);  
        return true;
    }
}
