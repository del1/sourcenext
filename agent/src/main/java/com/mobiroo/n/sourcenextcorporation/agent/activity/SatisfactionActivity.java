package com.mobiroo.n.sourcenextcorporation.agent.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;

import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

public class SatisfactionActivity extends FragmentActivity {
	public static final String PREF_SATISFACTION_ACTIVITY_SEEN = "prefSatisfactionActivitySeen";
	public static final String PREF_SATISFACTION_ACTIVITY_WAIT = "prefSatisfactionActivityWait";

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.satisfaction_dialog);

	}
	
	public void love(View v) {
		Usage.logEvent(this, Usage.Events.SATISFACTION_LOVE, false, new Usage.EventProperty(
                Usage.Properties.DAYS_INSTALLED, Integer.toString(PrefsHelper.getPrefInt(this, PREF_SATISFACTION_ACTIVITY_WAIT))));
		
		setContentView(R.layout.rate_us_dialog);		
	}
	
	public void rate_now(View v) {
		final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
		try {
		    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
		} catch (android.content.ActivityNotFoundException anfe) {
		    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
		}

		finish();
	}
	
	public void rate_later(View v) {
		finish();		
	}

	public void like(View v) {
		Usage.logEvent(this, Usage.Events.SATISFACTION_LIKE, false, new Usage.EventProperty(
                Usage.Properties.DAYS_INSTALLED, Integer.toString(PrefsHelper.getPrefInt(this, PREF_SATISFACTION_ACTIVITY_WAIT))));
		finish();
	}

	public void dislike(View v) {
		Usage.logEvent(this, Usage.Events.SATISFACTION_DISLIKE, false, new Usage.EventProperty(
                Usage.Properties.DAYS_INSTALLED, Integer.toString(PrefsHelper.getPrefInt(this, PREF_SATISFACTION_ACTIVITY_WAIT))));
		finish();
	}

}
