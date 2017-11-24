package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.service.DrivingService;
import com.mobiroo.n.sourcenextcorporation.agent.service.driving.lists.ActivityList;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

/**
 * Created by krohnjw on 2/25/14.
 */
public class DrivingActivityReceiver extends BroadcastReceiver {

    private void logd(String message) {
        Logger.d("DRIVING-DETECTION: " + message);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) { return;}

        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        if (result == null) {
            logd("null result");
            return;
        }

        boolean sendActivity = false;
        for (DetectedActivity a: result.getProbableActivities()) {
            sendActivity |= a.getType() == DetectedActivity.IN_VEHICLE && a.getConfidence() >= ActivityList.MINIMUM_DRIVING_ASSIST_CONFIDENCE;
        }

        // If we find an indication that we may be driving then spin up the service for driving with extras sent in as long as we are not in Airplane mode
        if (sendActivity && !PrefsHelper.isAirplaneModeEnabled(context)) {
            Intent service = new Intent(context, DrivingService.class);
            service.putExtras(intent.getExtras());
            context.startService(service);
        } else {
            // Ensure that AD is running at an appropriate polling interval
            DrivingService.checkPollingInterval(context, result.getMostProbableActivity().getType());
        }

    }
}
