package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mobiroo.n.sourcenextcorporation.agent.service.driving.lists.ActivityList;
import com.mobiroo.n.sourcenextcorporation.agent.service.driving.lists.SensorList;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.util.location.LocationHelper;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.DriveAgentActivityReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.service.driving.items.SensorData;
import com.mobiroo.n.sourcenextcorporation.agent.service.driving.items.SoundResults;
import com.mobiroo.n.sourcenextcorporation.agent.service.driving.lists.LocationList;
import com.mobiroo.n.sourcenextcorporation.agent.util.ActivityRecognitionHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

/**
 * Created by krohnjw on 2/25/14.
 */
public class DrivingService extends Service implements SensorEventListener, LocationListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    public static final String ACTION_DRIVING_STARTED = "com.mobiroo.n.sourcenextcorporation.agent.driving.DRIVING_STARTED";
    public static final String ACTION_DRIVING_STOPPED = "com.mobiroo.n.sourcenextcorporation.agent.driving.DRIVING_STOPPED";
    public static final String ACTION_WAKE_UP_CHECK = "com.mobiroo.n.sourcenextcorporation.agent.driving.wake_up_check";
    public static final String ACTION_STOP_SENSORS = "com.mobiroo.n.sourcenextcorporation.agent.driving.stop_sensors";
    public static final String ACTION_STOP_LOCATION = "com.mobiroo.n.sourcenextcorporation.agent.driving.stop_location";

    private final int REQUEST_WAKE_UP_CHECK = 8081;

    public static final String  ACTIVITY_DETECTION_TAG = "DrivingService";
    public static final long    ACTIVITY_DETECTION_INTERVAL = ActivityRecognitionHelper.INTERVAL_2_MINUTES;
    public static final long    ACTIVITY_DETECTION_WALKING_INTERVAL = ActivityRecognitionHelper.INTERVAL_1_MINUTE;
    public static final long    ACTIVITY_DETECTION_DRIVING_INTERVAL = ActivityRecognitionHelper.INTERVAL_30_SECONDS;
    public static final long    ACTIVITY_DETECTION_DRIVING_ASSIST_INTERVAL = ActivityRecognitionHelper.INTERVAL_30_SECONDS;
    public static final long    ACTIVITY_DETECTION_AIRPLANE_MODE_INTERVAL = ActivityRecognitionHelper.INTERVAL_5_MINUTES;

    public static final String  PREF_LAST_DRIVEN = "DAARLastDriven";
    private static final String PREF_REASON_STOPPED = "DAARReasonStopped";
    private static final String PREF_STOPPED_TIME = "DAARStoppedTime";

    private final int REASON_UNKNOWN = 0;
    private final int REASON_WALKING = 1;
    private final int REASON_SENSOR_DATA = 2;
    private final int REASON_ACTIVITY_DETECTION = 3;
    private final int REASON_LOCATION = 4;

    private int mStopReason;
    private int mStartReason;

    /* Variable definition for activity detection management */
    private final static String PREF_AD_SPED_UP_AT = "PrefDriveADSpedUpAt";
    private final static int DETECTION_SLOWDOWN_DELAY = 5 * 60 * 1000;


    /* Variable definition for Location management */
    protected GoogleApiClient   mClient;
    protected ConnectionResult  mConnectionResult;
    private int                 mLocationMode = LocationRequest.PRIORITY_HIGH_ACCURACY;
    private int                 mLocationInterval = 10 * 1000;
    private int                 mNumLocationUpdatesReceived = 0;
    private int                 mRequestedLocationUpdates = 2;

    private int                 mPendingLocationAction = 0;
    private final int           LOCATION_ACTION_REQUEST_UPDATES = 1;
    private final int           LOCATION_ACTION_REMOVE_UDPATES = 2;

    /* Variable definition for sensor management */
    private SensorManager       mManager;
    private long                mLastUpdate;

    private final long          RESTART_DRIVING_FROM_AD_TIMEOUT = 60 * 1000; // 60s
    private final long          DRIVING_ASSISTANCE_TIMEOUT = 30 * 1000; // 30s
    private final long          SENSOR_CHECK_TIMEOUT = 5 * 1000; // 5s


    private WifiManager         mWifiManager;

    private boolean mStopServiceWhenComplete = false;

    /* Internal class to maintain state of variables used to calculate driving */
    private class DrivingSession {
        // will wait at least this long before deactivating after we no longer get DetectedActivity.IN_VEHICLE
        public static final long MIN_INACTIVE_DRIVE_TIME = (long) (5 * ActivityRecognitionHelper.INTERVAL_1_MINUTE);

        public static final String PREF_IS_DRIVING = "is_driving";

        public ActivityList activities;
        public LocationList locations;
        public SensorList sensors;
        public SoundResults sound;

        /* State management to track what is running or not */
        public boolean locationActive;
        public boolean sensorsActive;
        public boolean activityDetectionActive;
        public boolean soundActive;

        private boolean mIsDriving;

        public DrivingSession() {
            activities = new ActivityList();
            locations = new LocationList();
            sensors = new SensorList();
            sound = new SoundResults(false);

            locationActive = false;
            sensorsActive = false;
            activityDetectionActive = false;
            soundActive = false;

            mIsDriving = false;
        }

        public boolean detectedDrivingStarted() {

            int confidence = 0;
            // Now requiring two signals that will indicate driving
            if (!isWifiConnected()) {
                // Don't consider driving if wifi is connected
                confidence += activities.getDrivingConfidence();

                confidence += sound.getDrivingConfidence();

                confidence += locations.getDrivingConfidence();

                mStartReason = REASON_ACTIVITY_DETECTION;
                return (confidence >= 2);
            }

            return false;
        }

        public boolean detectedDrivingStopped() {
            logd("Activity list has " + activities.getNodeCount());

            if (sensorsActive && sensors.isWalking()) {
                logd("DS: Found walking.");
                mStopReason = REASON_WALKING;
                return true;
            }

            if ((activities.getNodeCount() > 0) && (activities.getLatestActivityType() == DetectedActivity.UNKNOWN)) {
                logd("DS: skipping unknown activity.");
                return false;
            }

            if (activities.isWalking()) {
                logd("AD: found walking");
                mStopReason = REASON_WALKING;
                return true;
            }

            if ((activities.getNodeCount() > 0) && (activities.getLatestActivityType() == DetectedActivity.IN_VEHICLE)) {
                logd("AD: still driving");
                return false;
            }

            if (activities.getLatestActivityType() == DetectedActivity.ON_BICYCLE) {
                if (activities.containsDriving()) {
                    logd("AD: Received " + ActivityRecognitionHelper.getNameFromType(activities.getLatestActivityType()) + " but driving is still in the list, not stopping driving");
                    return false;
                }
            }

            // Check what activity detection is reporting and ensure that
            // our stillness threshold has been met
            long now = System.currentTimeMillis();
            long lastDriven = PrefsHelper.getPrefLong(getBaseContext(), DriveAgentActivityReceiver.PREF_LAST_DRIVEN, 0);

            boolean doneDriving = ((now - lastDriven) >= MIN_INACTIVE_DRIVE_TIME);
            logd("AD: Done driving = " + doneDriving + " last Drive " + (now - lastDriven) + " ago, timeout is " + MIN_INACTIVE_DRIVE_TIME);

            if (doneDriving) {
                logd("DS: Stopping driving based on AD");
                mStopReason = REASON_ACTIVITY_DETECTION;
                return true;
            } else {
                logd("DS: Scheduling a wakeup to stop driving");
                scheduleRestartForDriving(MIN_INACTIVE_DRIVE_TIME);
            }
            return false;
        }

        private boolean assistDrivingDetection() {
            return activities.containsDrivingHint();
        }

        private boolean speedUpActivityDetection() { return activities.containsDrivingHint();}

        private boolean isDriving() {
            mIsDriving = PrefsHelper.getPrefBool(getBaseContext(), PREF_IS_DRIVING, false);
            return mIsDriving;
        }

        private void saveDrivingState(boolean isDriving) {
            mIsDriving = isDriving;
            PrefsHelper.setPrefBool(getBaseContext(), PREF_IS_DRIVING, isDriving);
        }

    }

    private DrivingSession mSession;

    private DrivingSession getSession() {
        if (mSession == null) {
            mSession = new DrivingSession();
        }
        return mSession;
    }
    /* End session definition */

    private void logd(String message) {
        Logger.d("DRIVING-SERVICE: " + message);
    }

    public boolean isWifiConnected() {
        return (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED && mWifiManager.getConnectionInfo().getSupplicantState() == SupplicantState.COMPLETED);
    }

    /* Service definition */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build();
        mManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ActivityRecognitionResult result;

        if (intent == null) {
            logd("null intent");
            return START_STICKY;
        }

        result = ActivityRecognitionResult.extractResult(intent);
        if (result == null) {
            logd("null result list");
        } else {
            if (!getSession().activityDetectionActive) {
                logd("Start - Adding activity  " + result.getMostProbableActivity().getType() + " to session data");
                updateSessionData(result);
            } else {
                logd("Start - Not adding activity to session data as receiver is running");
            }
        }

        String action = intent.getAction();
        if (ACTION_STOP_SENSORS.equals(action)) {
            logd("Waking up to stop sensors");
            cleanUpListeners();
        } else if (ACTION_WAKE_UP_CHECK.equals(action)) {
            logd("Waking up to check session data");
            checkSessionData();
        } else if (ACTION_STOP_LOCATION.equals(action)) {
            logd("Waking up to stop location updates");
            stopLocationUpdates();
        } else {
            logd("Checking session data");
            checkSessionData();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logd("Service destroyed");

        if (mClient != null && mClient.isConnected()) {
            stopLocationUpdates();
        }
        stopActivityRecognitionReceiver();
        mSession = null;
    }

    private void startListeners() {
        if (!getSession().isDriving()) {
            int ad_confidence = getSession().activities.getLatestConfidence();
            logd("ad confidence is " + ad_confidence);
            // Use location based profiling if we have a high enough confidence level
            if (ad_confidence >= ActivityList.MINIMUM_DRIVING_LOCATION_ASSIST_CONFIDENCE) {
                logd("Starting location updates");
                startLocationUpdates();
            }

            // Use Sound profiling a swell
            /*logd("Starting sound sampling");
            startSoundSampling();*/

            stopSensorUpdates(); // Turn off sensor monitoring when not driving
        } else {
            startSensorUpdates(); // Use accel data when driving
            stopLocationUpdates(); // Turn off location when driving
        }
        startActivityRecognitionReceiver();
    }

    private void cleanUpListeners() {
        cleanUpListeners(false);
    }

    private void cleanUpListeners(boolean force) {
        if (force || getSession().sensorsActive) {
            logd("Stopping sensors");
            stopSensorUpdates();
        }

        if (force || getSession().locationActive) {
            logd("Stopping location updates");
            stopLocationUpdates();
        }
    }

    private void updateSessionData(ActivityRecognitionResult result) {
        getSession().activities.add(result);
        if (result.getMostProbableActivity().getType() == DetectedActivity.IN_VEHICLE) {
            PrefsHelper.setPrefLong(getBaseContext(), PREF_LAST_DRIVEN, System.currentTimeMillis());
        }
    }

    private void updateSessionData(Location location) {
        getSession().locations.add(location);
    }

    private void updateSessionData(SensorData data) {
        getSession().sensors.add(data);
    }

    private void checkSessionData() {
        if (getSession().isDriving()) {
            /* Ensure location is not running */
            if (getSession().locationActive) {
                stopLocationUpdates();
            }

            /* Ensure that sensors ARE running */
            if (!getSession().sensorsActive) {
                startSensorUpdates();
            }

            /* See if any update has signaled driving has ended */
            if (getSession().detectedDrivingStopped()) {
                stopDriving(mStopReason);
            }

        } else {

            if (getSession().detectedDrivingStarted()) {
                /* Signal driving has started. */
                startDriving(mStartReason);
            } else {
                /* Check for a cue that we should spin up listeners to assist AD in grabbing driving started */
                if (getSession().assistDrivingDetection()) {
                    if (!isWifiConnected()) {
                        logd("Assisting driving detection");
                        startListeners();
                    }
                } else {
                    // Our current result does NOT contain any info indicating we may be driving.
                    // Check the penultimate node and if it also does not (2 consecutive signals) then
                    // cancel the service
                    if ((getSession().activities.getNodeCount() > 0)
                            && (!getSession().activities.containsDriving(getSession().activities.getPenultimateNode()))) {
                        logd("Stopping services; last two entries had no driving");
                        stopService();
                    }
                }
            }
        }

        /* Make sure AD is running at an appropriate level */
        if (getSession().activities.getNodeCount() > 0) {
            checkPollingInterval(this, getSession().activities.getLatestActivityType(), getSession().speedUpActivityDetection());
        }
    }

    /* Callback method definition */

    private void stopDriving(int reason) {
        logd("SIGNALING DRIVING STOPPED");
        storeStop(reason);
        getSession().saveDrivingState(false);
        sendBroadcast(new Intent(ACTION_DRIVING_STOPPED));
        stopService();
    }

    private void stopService() {
        stopService(true);
    }

    private void stopService(boolean force) {
        logd("Stopping driving service");
        cleanUpListeners(force);
        cancelOutstandingAlarms();
        stopActivityRecognitionReceiver();
        this.stopSelf();
    }

    private void startDriving(int reason) {
        logd("START REQUESTED: " + reason);
        boolean start = true;
        switch (reason) {
            case REASON_ACTIVITY_DETECTION:
                // Make sure that we haven't stopped within 60s due to walking
                if (getStopReason() == REASON_WALKING) {
                    long stop_time = getStopTime();
                    if (System.currentTimeMillis() - stop_time > RESTART_DRIVING_FROM_AD_TIMEOUT) {
                        start = true;
                    } else {
                        logd("NOT STARTING DUE TO A RECENT STOP VIA WALKING");
                        start = false;
                    }
                }
                break;
            default:
                start = true;
                break;

        }

        if (start) {
            logd("SIGNALING DRIVING STARTING");
            getSession().saveDrivingState(true);
            sendBroadcast(new Intent(ACTION_DRIVING_STARTED));
            startListeners();
            stopLocationUpdates();
        }
    }

    private PendingIntent buildCheckDrivingAssistPendingIntent() {
        Intent service = new Intent(DrivingService.this, DrivingService.class);
        service.setAction(ACTION_WAKE_UP_CHECK);
        return PendingIntent.getService(DrivingService.this, REQUEST_WAKE_UP_CHECK, service, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void scheduleRestartForDriving(long time) {

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + time + 1000, buildCheckDrivingAssistPendingIntent());
    }

    private void cancelOutstandingAlarms() {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(buildCheckDrivingAssistPendingIntent());
    }

    private void storeStop(int reason) {
        PrefsHelper.setPrefInt(this, PREF_REASON_STOPPED, reason);
        PrefsHelper.setPrefLong(this, PREF_STOPPED_TIME, System.currentTimeMillis());
    }

    private int getStopReason() {
        return PrefsHelper.getPrefInt(this, PREF_REASON_STOPPED, REASON_UNKNOWN);
    }

    private long getStopTime() {
        return PrefsHelper.getPrefLong(this, PREF_STOPPED_TIME, 1);
    }

    /* Location callbacks and helper methods */
    private void startLocationUpdates() {
        Location last = LocationHelper.getLastLocation(this);
        if ((last != null) &&
                LocationHelper.isLocationFresh(last) && last.hasSpeed()) {
            logd("Using passive location");
            getSession().locations.add(LocationHelper.getLastLocation(this));
            LocationHelper.clearLocation(this);
            checkSessionData();
            return;
        }

        if (!getSession().locationActive) {
            mPendingLocationAction = LOCATION_ACTION_REQUEST_UPDATES;
            logd("connectiong to location api client");
            mClient.connect();
        }
    }

    private void stopLocationUpdates() {
        mPendingLocationAction = LOCATION_ACTION_REMOVE_UDPATES;
        getSession().locationActive = false;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.cancel(buildLocationStopIntent());
        if (mClient.isConnected()) {
            logd("LOCATION - Connected: Removing location updates");
            removeLocationUpdates();
        } else {
            logd("LOCATION - Location client not connected - Connecting to remove");
            // If not driving and not at a point where we should assist then kill the service.
            mStopServiceWhenComplete = (!getSession().isDriving() && !getSession().assistDrivingDetection());
            mClient.connect();
        }
    }

    private PendingIntent buildLocationStopIntent() {
        Intent i = new Intent(this, DrivingService.class);
        i.setAction(ACTION_STOP_LOCATION);
        return PendingIntent.getService(this, 91, i, PendingIntent.FLAG_ONE_SHOT);
    }

    private void removeLocationUpdates() {
        logd("LOCATION - Removing location updates");
        getSession().locationActive = false;
        LocationServices.FusedLocationApi.removeLocationUpdates(mClient, this);
        if (mStopServiceWhenComplete) {
            mStopServiceWhenComplete = false;
            stopService(false);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        logd("LOCATION - Connected with pending action " + mPendingLocationAction);
        if (mClient.isConnected()) {
            switch (mPendingLocationAction) {
                case LOCATION_ACTION_REQUEST_UPDATES:
                    getSession().locationActive = true;
                    logd("LOCATION - requesting updates at interval " + mLocationInterval);

                    /*Location latest = mLocationClient.getLastLocation();
                    logd("Latest location is " + latest.getLatitude() + "," + latest.getLongitude() + " from " + (System.currentTimeMillis() - latest.getTime()) + " ms ago");
                    // Grab last location and add to list
                    if (getSession().locations.isLocationNewer(latest)
                            && latest.getTime() > (System.currentTimeMillis() - 30 * 1000)) {
                        logd("Adding newer location to list");
                        getSession().locations.add(latest);
                        if (getSession().locations.isDriving()) {
                            checkSessionData();
                            return;
                        }
                    }*/

                    // Schedule a location query and a stop to ensure we don't run too long
                    AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                    am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + (mRequestedLocationUpdates * mLocationInterval * 1000) + 5000, buildLocationStopIntent());
                    mNumLocationUpdatesReceived = 0;

                    if (Utils.isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        LocationServices.FusedLocationApi
                                .requestLocationUpdates(mClient,
                                        new LocationRequest()
                                                .setInterval(mLocationInterval)
                                                .setPriority(mLocationMode)
                                                .setNumUpdates(mRequestedLocationUpdates)
                                                .setExpirationDuration(DRIVING_ASSISTANCE_TIMEOUT),
                                        this);
                    } else {
                        Utils.postNotification(this, new String[] {
                                Manifest.permission.ACCESS_FINE_LOCATION
                        });
                    }
                    break;
                case LOCATION_ACTION_REMOVE_UDPATES:
                    removeLocationUpdates();
                    break;
            }

        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        logd("onConnectionSuspended: " + i);

    }

    @Override
    public void onLocationChanged(Location location) {
        mNumLocationUpdatesReceived++;
        if ((location != null)) { // && (location.hasSpeed())) {
            logd("LOCATION - New location received, updating session data");
            updateSessionData(location);
            checkSessionData();
        } else  {
            if (location == null) {
                logd("LOCATION - Location is null");
            } else {
                logd("LOCATION - Location has no speed");
            }
        }

        // Explicitly stop location updates here in case we have received our 5 updates and have not yet stopped.
        if (mNumLocationUpdatesReceived >= mRequestedLocationUpdates) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        logd("onConnectionFailed: " + connectionResult.getErrorCode() + ": " + connectionResult.toString());
    }

    /* End location callbacks and helper methods */


    /* Sensor callbacks and helper methods */
    final String PREF_SENSORS_STARTED = "DAARSensorsStartedTime";

    private PendingIntent buildSensorStopIntent() {
        Intent i = new Intent(this, DrivingService.class);
        i.setAction(ACTION_STOP_SENSORS);
        return PendingIntent.getService(this, 92, i, PendingIntent.FLAG_ONE_SHOT);
    }

    private void startSensorUpdates() {
        if (!getSession().sensorsActive) {
            logd("Starting sensors");
            PrefsHelper.setPrefLong(this, PREF_SENSORS_STARTED, System.currentTimeMillis());
            getSession().sensorsActive = true;

            mManager.registerListener(this,
                    mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL); // Maybe move to SENSOR_DELAY_GAME here for quicker updates?
        }
    }

    private void stopSensorUpdates() {
        getSession().sensorsActive = false;
        mManager.unregisterListener(this);
    }

    private void markUpdate() {
        mLastUpdate = System.currentTimeMillis();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                getAccelerometer(sensorEvent);
                break;
            case Sensor.TYPE_GYROSCOPE:
                getGyroscope(sensorEvent);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                getLinearAcceleration(sensorEvent);
                break;
        }
    }

    private long mLastLinearUpdate;

    private void getLinearAcceleration(SensorEvent event) {
        float[] values = event.values;

        float x = values[0];
        float y = values[1];
        float z = values[2];



        if ((System.currentTimeMillis() - mLastLinearUpdate) > SENSOR_CHECK_TIMEOUT) {
            logd(String.format("Acceleration along each axis x=%s, y=%s, z=%s", String.valueOf(x), String.valueOf(y), String.valueOf(z)));
            if (mLastLinearUpdate != 0) {
                long now = System.currentTimeMillis();
                long diff = now - mLastLinearUpdate;
            }

            mLastLinearUpdate = System.currentTimeMillis();

        }




    }
    private long mLastGyroUpdate;

    private void getGyroscope(SensorEvent event) {
        float[] values = event.values;

        float yaw = values[0];
        float pitch = values[1];
        float roll = values[2];

        if ((System.currentTimeMillis() - mLastGyroUpdate) > SENSOR_CHECK_TIMEOUT) {
            mLastGyroUpdate = System.currentTimeMillis();
            Logger.d("GYRO: yaw=" + yaw + " pitch=" + pitch + " roll=" + roll);
        }
    }

    private long mLastSensorTime = 1;

    private void getAccelerometer(SensorEvent event) {
        /* LOTS of data is sent. Only update once a second for now */
        if ((System.currentTimeMillis() - mLastUpdate) > 1000) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Removing logging statement of raw data - logd(String.format("ACCEL: x=%1$f y=%2$f z=%3$f", x, y, z));

            markUpdate();
            SensorData data = new SensorData(x, y, z, mLastUpdate);
            updateSessionData(data);
            // Only check data in 5s increments
            if ((System.currentTimeMillis() - mLastSensorTime) > SENSOR_CHECK_TIMEOUT) {
                mLastSensorTime = System.currentTimeMillis();
                checkSessionData();
            }
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /* End sensor callbacks and helper methods */

    /* Receiver for activity detection broadcasts and methods */

    private BroadcastReceiver mActivityReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            if (result != null) {
                logd("Adding activity  " + result.getMostProbableActivity().getType() + " to session data");
                updateSessionData(result);
            }
        }
    };

    private void startActivityRecognitionReceiver() {
        if (!getSession().activityDetectionActive) {
            logd("Registering receiver for activity detection");
            getSession().activityDetectionActive = true;
            try {
                registerReceiver(mActivityReceiver, new IntentFilter(ActivityRecognitionService.ACTION_ACTIVITY_ALL));
            } catch (Exception ignored) {
            }
        } else {
            logd("Skipping registration for activity detection as it is already running");
        }
    }

    private void stopActivityRecognitionReceiver() {
        logd("Removing receiver for activity detection");
        getSession().activityDetectionActive = false;
        try {
            unregisterReceiver(mActivityReceiver);
        } catch (Exception ignored) {

        }
    }

    public static void checkPollingInterval(Context context, int activity) {
        checkPollingInterval(context, activity, false);
    }

    private static void checkPollingInterval(Context context, int activity, boolean assist) {

        SharedPreferences sharedPrefs = ActivityRecognitionHelper.getSharedPrefs(context);

        long current = ActivityRecognitionHelper.getCurrentPollingInterval(context);

        if (PrefsHelper.isAirplaneModeEnabled(context)) {
            // If Airplane mode is enabled then make this detection SLOW
            sharedPrefs.edit().putLong(PREF_AD_SPED_UP_AT, System.currentTimeMillis()).commit();
            if (current < ACTIVITY_DETECTION_AIRPLANE_MODE_INTERVAL) {
                Logger.d("Driving Service: Airplane Mode enabled, setting to 5 minutes");
                ActivityRecognitionHelper.requestActivityRecognition(context, ACTIVITY_DETECTION_AIRPLANE_MODE_INTERVAL, ACTIVITY_DETECTION_TAG);
            }
            return;
        }

        if ((activity == DetectedActivity.IN_VEHICLE) || (activity == DetectedActivity.UNKNOWN && assist)) {
            sharedPrefs.edit().putLong(PREF_AD_SPED_UP_AT, System.currentTimeMillis()).commit();
            if (current > ACTIVITY_DETECTION_DRIVING_INTERVAL) {
                // Speed up detection due to driving condition
                Logger.d("Driving Service: requesting faster detection for driving");
                ActivityRecognitionHelper.requestActivityRecognition(context, ACTIVITY_DETECTION_DRIVING_INTERVAL, ACTIVITY_DETECTION_TAG);
            }
            return;
        }



        if (activity == DetectedActivity.ON_FOOT) {
            sharedPrefs.edit().putLong(PREF_AD_SPED_UP_AT, System.currentTimeMillis()).commit();
            if (current > ACTIVITY_DETECTION_WALKING_INTERVAL) {
                // Speed up detection due to walking condition
                Logger.d("Driving Service: requesting faster detection for walking");
                ActivityRecognitionHelper.requestActivityRecognition(context, ACTIVITY_DETECTION_WALKING_INTERVAL, ACTIVITY_DETECTION_TAG);
            }
            return;
        }

        if (current == ACTIVITY_DETECTION_INTERVAL) {
            return;
        }

        if ((System.currentTimeMillis() - sharedPrefs.getLong(PREF_AD_SPED_UP_AT, 0)) > DETECTION_SLOWDOWN_DELAY) {
            // found non driving condition with decent confidence and delay, slow down detection
            Logger.d("Driving Service: requesting normal detection");
            ActivityRecognitionHelper.requestActivityRecognition(context, ACTIVITY_DETECTION_INTERVAL, ACTIVITY_DETECTION_TAG);
        }

    }

   /* Sound profiling methods */
    /*private void startSoundSampling() {
        try {
            registerReceiver(soundResultsReceiver, new IntentFilter(SoundService.ACTION_DETECTED_VALUE));
        } catch (Exception e) {
            Logger.d("Exception registering sound receiver: " + e);
        }
        Handler h = new Handler();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                stopSoundSampling();
            }
        };
        h.postDelayed(r, 2500);
        SoundService.requestSoundSampling(this);
    }

    private void stopSoundSampling() {
        sendBroadcast(new Intent(SoundService.ACTION_STOP));
    }

    private BroadcastReceiver soundResultsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean in_car = intent.getBooleanExtra(SoundService.EXTRA_IN_CAR, false);
            logd("Sound returned: " + in_car);
            String data = intent.getStringExtra(SoundService.EXTRA_DATA);
            try {unregisterReceiver(this); }
            catch (Exception ignored) { }
            getSession().soundActive = false;
            getSession().sound = new SoundResults(in_car);
        }
    };*/

    /* End sound profiling methods */
}
