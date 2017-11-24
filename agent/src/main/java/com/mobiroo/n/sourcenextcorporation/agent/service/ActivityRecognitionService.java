package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.ActivityRecognitionHelper;


public class ActivityRecognitionService extends IntentService {

    public static final String      ACTION_ACTIVITY_UPDATED         = "com.mobiroo.n.sourcenextcorporation.agent.launcher.ACTION_ACTIVITY_UPDATED";
    public static final String      EXTRA_ACTIVITY_TYPE             = "com.mobiroo.n.sourcenextcorporation.agent.launcher.ACTIVITY_TYPE";
    public static final String      ACTION_TILTING                  = "com.mobiroo.n.sourcenextcorporation.agent.launcher.ACTION_TILTING";
    public static final String      ACTION_ACTIVITY_ALL             = "com.mobiroo.n.sourcenextcorporation.agent.launcher.ACTION_ACTIVITY_ALL";

    private static final int        MIN_CONFIDENCE = 55;

    public ActivityRecognitionService() {
        super("ActivityRecognitionIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    	ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
    	if (result == null) {
    		Logger.d("DETECTION: null result");
            return;
    	}

        StringBuilder sb = new StringBuilder();
        sb.append("DETECTION: ");
        for (DetectedActivity a: result.getProbableActivities()) {
            sb.append(ActivityRecognitionHelper.getNameFromType(a.getType())  + "=" + a.getConfidence() + ", ");
        }
        Logger.d(sb.toString());

        /* Broadcast full activity update to any receivers that want all results
            regardless of confidence level (driving detection for example)
         */
        Intent all = new Intent(ACTION_ACTIVITY_ALL);
        all.putExtras(intent.getExtras());
        sendBroadcast(all);

        /* Grab most probable result and send broadcast if
            above minimum confidence level
         */
    	ActivityRecognitionHelper.setLastDetectionUpdate(this);
    	DetectedActivity mostProbableActivity = result.getMostProbableActivity();

        int activityType = mostProbableActivity.getType();
        int activityConfidence = mostProbableActivity.getConfidence();

    	String activityName = ActivityRecognitionHelper.getNameFromType(activityType);

    	// don't do anything in cases where the detected activity is a transition state
    	if (activityConfidence < MIN_CONFIDENCE) {
    		Logger.d("DETECTION: Not broadcasting " + activityName + 
    				"(" + activityType + ").  Confidence only " + activityConfidence);
    		return;
    	}


    	// TILTING is special case; don't log it but fire special intent
    	if (activityType == DetectedActivity.TILTING) {
    		Intent update = new Intent(ACTION_TILTING);
    		update.putExtra(EXTRA_ACTIVITY_TYPE, activityType);
    		this.sendBroadcast(update);
    		return;
    	}


    	// Store this as the current activity and write to the activity log
    	ActivityRecognitionHelper.setLastActivity(this, 
    			ActivityRecognitionHelper.getCurrentActivity(this));
    	ActivityRecognitionHelper.setCurrentActivity(this, activityType);
    	ActivityRecognitionHelper.logActivity(this, activityConfidence);

    	Intent update = new Intent(ACTION_ACTIVITY_UPDATED);
        update.putExtras(intent.getExtras());
    	update.putExtra(EXTRA_ACTIVITY_TYPE, activityType);
    	this.sendBroadcast(update);

        return;
    }
}
