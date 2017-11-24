package com.mobiroo.n.sourcenextcorporation.agent.item.ui.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

public class FadeAnimation {
	public static void crossfade(View newView, final View oldView, int animationPeriod) {

        if(newView != null) {
            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            newView.setAlpha(0f);
            newView.setVisibility(View.VISIBLE);

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view.
            newView.animate()
                    .alpha(1f)
                    .setDuration(animationPeriod)
                    .setListener(null);
        }

	    // Animate the loading view to 0% opacity. After the animation ends,
	    // set its visibility to GONE as an optimization step (it won't
	    // participate in layout passes, etc.)
		oldView.animate()
	            .alpha(0f)
	            .setDuration(animationPeriod)
	            .setListener(new AnimatorListenerAdapter() {
	                @Override
	                public void onAnimationEnd(Animator animation) {
	                	oldView.setVisibility(View.GONE);
	                }
	            });
	}
}
