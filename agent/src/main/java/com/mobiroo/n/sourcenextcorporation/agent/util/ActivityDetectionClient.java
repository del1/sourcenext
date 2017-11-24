package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.mobiroo.n.sourcenextcorporation.agent.service.DrivingService;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.DriveAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.ParkingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.service.ActivityRecognitionService;

import java.util.ArrayList;

public class ActivityDetectionClient implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
    private static final String PREF_INTERVAL_PREFIX = "prefRequestedInterval_";
    private static final String PREF_INTERVAL_TAG_LIST = "prefIntervalTagList";


    private boolean mInProgress;
    private PendingIntent mActivityRecognitionPendingIntent;
    private GoogleApiClient mClient;

    private REQUEST_TYPE mRequestType;
    private Context mContext;
    private long mIntervalMilliseconds = ActivityRecognitionHelper.INTERVAL_2_MINUTES;

    private final String mLogPrefix = "Activity Recognition: ";
    private static final String NO_TAG = "";

    private enum REQUEST_TYPE {START, STOP}

    private SharedPreferences mSharedPrefs;

    private static ActivityDetectionClient sADClient;


    static ActivityDetectionClient getADClient(Context context) {
        if (sADClient == null) {
            sADClient = new ActivityDetectionClient(context);
        }

        // refresh context and shared prefs
        sADClient.mContext = context;
        sADClient.mSharedPrefs = ActivityRecognitionHelper.getSharedPrefs(context);
        return sADClient;
    }

    private ActivityDetectionClient(Context context) {
        mContext = context;
        
        /*
         * If a client doesn't already exist, create a new one, otherwise
         * return the existing one. This allows multiple attempts to send
         * a request without causing memory leaks by constantly creating
         * new clients.
         *
         */
        getClient();
        Intent intent = new Intent(context, ActivityRecognitionService.class);
        mActivityRecognitionPendingIntent = PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mSharedPrefs = ActivityRecognitionHelper.getSharedPrefs(mContext);
    }

    private GoogleApiClient getClient() {
        if (mClient == null) {
            mClient = new GoogleApiClient.Builder(mContext)
                    .addApi(ActivityRecognition.API)
                    .addOnConnectionFailedListener(this)
                    .addConnectionCallbacks(this)
                    .build();
        }
        return mClient;
    }

    void startAllDetection() {
        setCurrentPollingInterval(ActivityRecognitionHelper.INTERVAL_NOT_SET);
        setPollingInterval(getLongestAllowedPollingInterval(), "");
        startUpdates();
    }

    void stopAllDetection() {
        stopUpdates();
    }

    void stopDetection(String tag) {
        Logger.d(mLogPrefix + "Stopping Updates for " + tag);
        if ((tag != null) && (!tag.isEmpty())) {
            setPollingInterval(ActivityRecognitionHelper.INTERVAL_NOT_SET, tag);
            removeInterval(tag);
        }

        ArrayList<String> tagList = getTagList();

        /* Add a simple check for now to see if DrivingService is left but PA and DA removed */
        if (tagList.contains(DrivingService.ACTIVITY_DETECTION_TAG) &&
                !tagList.contains(DriveAgent.ACTIVITY_DETECTION_TAG) &&
                !tagList.contains(ParkingAgent.ACTIVITY_DETECTION_TAG)) {
            removeInterval(DrivingService.ACTIVITY_DETECTION_TAG);
            tagList.remove(DrivingService.ACTIVITY_DETECTION_TAG);
        }

        if (tagList.size() < 1) {
            Logger.d(mLogPrefix + "Last tag removed; stopping updates.");
            stopUpdates();
        } else {
            Logger.d(mLogPrefix + "One or more tags present " + TextUtils.join(",", tagList));
            startUpdates();  // will reset to correct poll interval for other agents
        }
    }


    long getCurrentPollingInterval() {
        return mSharedPrefs.getLong(ActivityRecognitionHelper.KEY_CURRENT_POLLING_INTERVAL, ActivityRecognitionHelper.INTERVAL_NOT_SET);
    }

    boolean setPollingInterval(long ms, String tag) {
        if ((tag != null) && (!tag.isEmpty())) {
            storeInterval(ms, tag);
        }

        long current = getCurrentPollingInterval();
        long longest_allowed = getLongestAllowedPollingInterval();
        long desired = (ms < longest_allowed) ? ms : longest_allowed;

        mIntervalMilliseconds = desired;

        if (desired != current) {
            Logger.d(mLogPrefix + "updating interval to " + desired + " from " + current);
            setCurrentPollingInterval(desired);
            return true;
        } else {
            Logger.d(mLogPrefix + "requested interval " + desired + " is not quicker or different than current interval " + current);
            return false;
        }
    }

    void setPollingIntervalAndStart(long ms, String tag) {
        boolean needToChangeInterval = setPollingInterval(ms, tag);
        if (needToChangeInterval) {
            // We need to update the polling interval for the client
            if (getClient().isConnected()) {
                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClient, mIntervalMilliseconds, mActivityRecognitionPendingIntent);
            } else {
                startUpdates();
            }
        }
    }


    // ----------------------------------------
    // connection interfaces
    // ----------------------------------------


    @Override
    public void onConnected(Bundle bundle) {
        if (mRequestType == REQUEST_TYPE.START) {
            Logger.d(mLogPrefix + "Requesting updates");
            PrefsHelper.setPrefBool(mContext, ActivityRecognitionHelper.KEY_IS_RUNNING, true);
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mClient,
                    mIntervalMilliseconds,
                    mActivityRecognitionPendingIntent);
        } else if (mRequestType == REQUEST_TYPE.STOP) {
            Logger.d(mLogPrefix + "Removing updates");
            PrefsHelper.setPrefBool(mContext, ActivityRecognitionHelper.KEY_IS_RUNNING, false);
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mClient, mActivityRecognitionPendingIntent);
        } else {
            Logger.d(mLogPrefix + "onConnected, but neither start nor stop");
        }

        mInProgress = false;
        mClient.disconnect();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Turn off the request flag
        mInProgress = false;
        // Delete the client
        mClient = null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Logger.d(mLogPrefix + "onConnectionFailed called in BootReceiver " + result);
        // Turn off the request flag
        mInProgress = false;
    }


    // -------------------------------------
    // private methods
    // -------------------------------------


    private void setCurrentPollingInterval(long ms) {
        mSharedPrefs.edit().putLong(ActivityRecognitionHelper.KEY_CURRENT_POLLING_INTERVAL, ms).commit();
    }

    private long getPollingIntervalFor(String tag) {
        return mSharedPrefs.getLong(PREF_INTERVAL_PREFIX + tag, ActivityRecognitionHelper.INTERVAL_NOT_SET);
    }


    private void storeInterval(long ms, String tag) {
        mSharedPrefs.edit().putLong(PREF_INTERVAL_PREFIX + tag, ms).commit();
        ArrayList<String> list = getTagList();
        if (!list.contains(String.valueOf(tag))) {
            list.add(tag);
            storeTagList(list);
        }
    }

    private ArrayList<String> getTagList() {
        String[] list = mSharedPrefs.getString(PREF_INTERVAL_TAG_LIST, "").split(",");
        ArrayList<String> tagList = new ArrayList<String>(2);
        for (String s : list) {
            if ((s == null) || (s.trim().isEmpty())) {
                continue;
            }
            tagList.add(s);
        }
        return tagList;
    }

    private void storeTagList(ArrayList<String> list) {
        mSharedPrefs.edit().putString(PREF_INTERVAL_TAG_LIST, TextUtils.join(",", list)).commit();
    }

    private long removeInterval(String tag) {
        long interval = ActivityRecognitionHelper.INTERVAL_NOT_SET;

        ArrayList<String> list = getTagList();
        if (list.contains(String.valueOf(tag))) {
            list.remove(String.valueOf(tag));
            interval = getPollingIntervalFor(tag);
            mSharedPrefs.edit().remove(PREF_INTERVAL_PREFIX + tag).commit();
            storeTagList(list);
        }
        return interval;
    }

    private long getLongestAllowedPollingInterval() {
        ArrayList<String> list = getTagList();
        long allowed = ActivityRecognitionHelper.INTERVAL_5_MINUTES;

        for (String s : list) {
            if (getPollingIntervalFor(s) < allowed) {
                allowed = getPollingIntervalFor(s);
            }
        }

        return allowed;
    }

    private void startUpdates() {
        // Check for Google Play services
        mRequestType = REQUEST_TYPE.START;

        if (!isGooglePlayServicesAvailable()) {
            return;
        }
        // If a request is not already underway
        if (!mInProgress) {
            Logger.d(mLogPrefix + "Connecting to play services to START");
            // Indicate that a request is in progress
            mInProgress = true;
            // Request a connection to Location Services
            getClient().connect();
        } else {
        }
    }

    private void stopUpdates() {
        // Set the request type to STOP
        mRequestType = REQUEST_TYPE.STOP;
        setCurrentPollingInterval(ActivityRecognitionHelper.INTERVAL_NOT_SET);
        /*
         * Test for Google Play services after setting the request type.
         * If Google Play services isn't present, the request can be
         * restarted.
         */
        if (!isGooglePlayServicesAvailable()) {
            return;
        }
        // If a request is not already underway
        if (!mInProgress) {
            Logger.d(mLogPrefix + "Connecting to play services to STOP");
            // Indicate that a request is in progress
            mInProgress = true;
            // Request a connection to Location Services
            getClient().connect();
            //
        } else {
            /*
             * A request is already underway. You can handle
             * this situation by disconnecting the client,
             * re-setting the flag, and then re-trying the
             * request.
             */
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.
                        isGooglePlayServicesAvailable(mContext);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // Continue
            return true;
            // Google Play services was not available for some reason
        } else {
            Logger.d(mLogPrefix + "Google Play services is not available");
            return false;
        }
    }

}
