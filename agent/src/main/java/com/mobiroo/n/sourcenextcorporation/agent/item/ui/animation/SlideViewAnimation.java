package com.mobiroo.n.sourcenextcorporation.agent.item.ui.animation;

import android.animation.ValueAnimator;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.google.analytics.tracking.android.Log;

public class SlideViewAnimation {
	public static void closeSlideUp(final LinearLayout v, final Runnable onPostExecute) {
		ValueAnimator va = ValueAnimator.ofInt(v.getHeight(), 0);
		
		va.setDuration((v.getHeight())*2);
	    va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
	        public void onAnimationUpdate(ValueAnimator animation) {
	            Integer value = (Integer) animation.getAnimatedValue();
	            v.getLayoutParams().height = value.intValue();
	            v.requestLayout();
	            
	            if((value.intValue() == 0) && (onPostExecute != null)) onPostExecute.run();
	        }
	    });
	    
		va.start();
	}

	public static void openSlideDown(final LinearLayout v, final Runnable onPostExecute) {
		v.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
		        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));

		final int target = v.getMeasuredHeight();
		ValueAnimator va = ValueAnimator.ofInt(target, 0);

		va.setDuration(target*2);
	    va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
	        public void onAnimationUpdate(ValueAnimator animation) {
	            Integer value = (Integer) animation.getAnimatedValue();
	            if (animation.getAnimatedFraction()>0.9) {
	            	Log.i("got here");
	            	v.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
	            } else {
	            	v.getLayoutParams().height = Math.max(target - value.intValue(), 1);
	            }
            	Log.i("HEIGHT: " + v.getLayoutParams().height);

	            v.requestLayout();
	            
	            if((value.intValue() == v.getMeasuredHeight()) && (onPostExecute != null)) onPostExecute.run();

	        }
	    });
	    
		va.start();
	}
}
