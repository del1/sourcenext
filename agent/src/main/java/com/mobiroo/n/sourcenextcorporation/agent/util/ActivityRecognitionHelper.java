package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.android.gms.location.DetectedActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.ActivityDetectorInterface;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;

import java.util.ArrayList;
import java.util.Arrays;

public class ActivityRecognitionHelper {
    public static final long INTERVAL_NOT_SET = 99999999;
    public static final long INTERVAL_5_MINUTES = 300000;
    public static final long INTERVAL_2_MINUTES = 120000;
    public static final long INTERVAL_1_MINUTE = 60000;
    public static final long INTERVAL_30_SECONDS = 30000;
    public static final long INTERVAL_10_SECONDS = 10000;


    public static final String PREFS_NAME = "ActivityRecognitionPrefs";
    public static final long UNKNOWN_LAST_UPDATE = -1;


    private static final String PREF_LAST_KNOWN_ACTIVITY = "prefLastKnownActivity";
    private static final String PREF_CURRENT_ACTIVITY = "prefCurrentActivity";
    private static final String PREF_ACTIVITY_LOG = "prefActivityLog";


    static final String KEY_IS_RUNNING = "prefActivityDetectionRunning";
    static final String KEY_CURRENT_POLLING_INTERVAL = "prefCurrentPollingInterval";
    static final String KEY_LAST_UPDATE = "prefActivityDetectionLastUpdate";


    public static void stopActivityRecognition(Context context, String tag) {
        Logger.d("Activity Recognition: Calling stop detection for " + tag);
        ActivityDetectionClient.getADClient(context).stopDetection(tag);
    }

    public static void stopAllActivityRecognition(Context context) {
        Logger.d("Activity Recognition: Stopping all detection");
        ActivityDetectionClient.getADClient(context).stopAllDetection();
    }

    public static boolean shouldStartActivityRecognition(Context context) {
        for (Agent agent : AgentFactory.getInstalledAgents(context)) {
            if (agent instanceof ActivityDetectorInterface) {
                if (((ActivityDetectorInterface) agent).needsActivityDetection()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void requestActivityRecognition(Context context, long pollingInterval, String tag) {
        ActivityDetectionClient.getADClient(context).setPollingIntervalAndStart(pollingInterval, tag);
    }

    public static void startAllActivityRecognition(Context context) {
        Logger.d("Activity Recognition: Starting activity recognition");
        ActivityDetectionClient.getADClient(context).startAllDetection();
    }

    public static void startActivityRecognitionIfNeeded(Context context) {
        if (shouldStartActivityRecognition(context)) {
            Logger.d("checkActivityRecognition: starting up AD");
            startAllActivityRecognition(context);
        }
    }



    public static int getCurrentActivity(Context context) {
        return getSharedPrefs(context).getInt(PREF_CURRENT_ACTIVITY, -1);
    }

    public static void setCurrentActivity(Context context, int activity) {
        getSharedPrefs(context).edit().putInt(PREF_CURRENT_ACTIVITY, activity).commit();
    }

    public static int getLastActivity(Context context) {
        return getSharedPrefs(context).getInt(PREF_LAST_KNOWN_ACTIVITY, -1);
    }

    public static void setLastActivity(Context context, int activity) {
        getSharedPrefs(context).edit().putInt(PREF_LAST_KNOWN_ACTIVITY, activity).commit();
    }

    public static void logActivity(Context context, int activity) {
        ArrayList<String> list = getActivityLog(context);
        if (list.size() > 4) {
            // Trim 5th element from list
            list.remove(4);
        }
        list.add(0, String.valueOf(activity));
        getSharedPrefs(context).edit().putString(PREF_ACTIVITY_LOG, TextUtils.join(",", list)).commit();
    }

    public static ArrayList<String> getActivityLog(Context context) {
        String[] list = getSharedPrefs(context).getString(PREF_ACTIVITY_LOG, "").split(",");
        return new ArrayList<String>(Arrays.asList(list));
    }

    public static String getNameFromType(int activityType) {
        switch (activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.TILTING:
                return "tilting";
        }
        return "unknown";
    }


    public static boolean isActivityDetectionRunning(Context context) {
        return PrefsHelper.getPrefBool(context, KEY_IS_RUNNING, false);
    }

    public static void clearActivityDetectionRunningFlag(Context context) {
        PrefsHelper.setPrefBool(context, KEY_IS_RUNNING, false);
    }

    public static void setLastDetectionUpdate(Context context) {
        PrefsHelper.setPrefLong(context, KEY_LAST_UPDATE, System.currentTimeMillis());
    }

    public static long getLastDetectionUpdate(Context context) {
        return PrefsHelper.getPrefLong(context, KEY_LAST_UPDATE, UNKNOWN_LAST_UPDATE);
    }

    public static SharedPreferences getSharedPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, 0);
    }

    public static long getCurrentPollingInterval(Context context) {
        return ActivityDetectionClient.getADClient(context).getCurrentPollingInterval();
    }


}
