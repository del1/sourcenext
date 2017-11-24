package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.DetectedActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.SleepAgent;
import com.mobiroo.n.sourcenextcorporation.agent.service.ActivityRecognitionService;
import com.mobiroo.n.sourcenextcorporation.agent.util.ActivityRecognitionHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

public class SleepAgentActivityReceiver extends BroadcastReceiver {

	private final int       mDesiredActivity = DetectedActivity.STILL;

	private static final String    PREF_STILL_START_TIME = "sleep.agent.still_start_time";
	private static final long      TIME_NOT_SET = -1;

	private void logd(String msg) {
		Logger.d("SleepAgentActivityReceiver: " + msg);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) { return; }

		// Grab broadcast activity from intent
        int activityType = intent.getIntExtra(ActivityRecognitionService.EXTRA_ACTIVITY_TYPE, DetectedActivity.UNKNOWN);
		String activityName = ActivityRecognitionHelper.getNameFromType(activityType);

		SleepAgent sa = (SleepAgent) AgentFactory.getAgentFromGuid(context, SleepAgent.HARDCODED_GUID);
		
		// TODO: re-xamine this hack
		// hack in case alarm receiver fails
		// for some reason, long running alarms sometimes fail; since Activity Detection runs in a loop
		// we can disable sleep agent here
		// see github issue #936, Ivan Dusper
		if ((sa.isActive()) && (! sa.isWithinUserTime())) {
			logd("Deactivating SA form within activity detection due to time mismatch.");
			DbAgent.setInactive(context, SleepAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_TIME);
		}

		if (activityType != mDesiredActivity) {
			logd("Received activity: " + activityName + "; clearing still");
			clearStill(context, sa);
			return;
		}

		long start = PrefsHelper.getPrefLong(context, PREF_STILL_START_TIME, TIME_NOT_SET);
		if (start == TIME_NOT_SET) {
			start = System.currentTimeMillis();
			PrefsHelper.setPrefLong(context, PREF_STILL_START_TIME, System.currentTimeMillis());
		}

		// We have a time here.  Calculate elapsed since start
		long current = System.currentTimeMillis();
		long stillFor = current - start;

		sa.setStillFor(stillFor);
		logd("Received activity " + activityName + "; device has been still for " + stillFor);

		// will check preconditions before executing
		DbAgent.setActive(context, SleepAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_TIME);
	}

	public static void clearStill(Context context, SleepAgent sa) {
		if (sa == null) {
			sa = (SleepAgent) AgentFactory.getAgentFromGuid(context, SleepAgent.HARDCODED_GUID);
		}
		sa.setStillFor(0);
		PrefsHelper.setPrefLong(context, PREF_STILL_START_TIME, TIME_NOT_SET);
	}
}
