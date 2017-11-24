package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.LocationSaverInterface;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

public class LocationSaverService extends Service implements LocationListener, 
GoogleApiClient.ConnectionCallbacks,
GoogleApiClient.OnConnectionFailedListener {

	public static final String		EXTRA_AGENT_GUID = MainActivity.EXTRA_AGENT_GUID;
	public static final String		EXTRA_TRIGGER_TYPE = "triggerType";
	
	
	protected ConnectionResult		mConnectionResult;
	protected GoogleApiClient		mClient;
	protected String				mAgentGuid;
	protected int					mTriggerType;
    
    int mUpdatesReceived = 0;

    // we go through multiple location updates to get better accuracy
    private static final int               LOCATION_DESIRED_UPDATES = 3;
    private static final int               LOCATION_UPDATE_INTERVAL = 3000;
    
    
    public static int getApproxLocationDelayMs() {
    	return LOCATION_DESIRED_UPDATES * LOCATION_UPDATE_INTERVAL + 1000;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If we're being started it's because the condition to store
        // a location has been hit. 
        logd("Finding location");
        
        mAgentGuid = intent.getStringExtra(EXTRA_AGENT_GUID);
        mTriggerType = intent.getIntExtra(EXTRA_TRIGGER_TYPE, Constants.TRIGGER_TYPE_UNKNOWN);

        mClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build();
        mClient.connect();
        return Service.START_REDELIVER_INTENT;
    }
    

    @Override
    public void onDestroy() {
        if ((mClient != null) && mClient.isConnected()) {
            logd("Disconnecting location");
            LocationServices.FusedLocationApi
                    .removeLocationUpdates(mClient, this);
        }

        super.onDestroy();
    }
    
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        logd("location connection failed: " + result.getErrorCode() + ": " + result.toString());
        mConnectionResult = result;
    }


    @Override
    public void onConnected(Bundle bundle) {
        logd("location connected");
        
        mUpdatesReceived = 0;
        if (mClient.isConnected()) {
            logd("Requesting location updates");
            if (Utils.isPermissionGranted(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                LocationServices.FusedLocationApi
                        .requestLocationUpdates(mClient,
                                new LocationRequest()
                                        .setInterval(LOCATION_UPDATE_INTERVAL)
                                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                        .setNumUpdates(LOCATION_DESIRED_UPDATES),
                                this);
            } else {
                Utils.postNotification(this, new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION
                });
            }
        } 
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onLocationChanged(Location location) {
        logd("Got new location: " + location.getAccuracy() + " " + location.getLatitude() + "," + location.getLongitude());
        mUpdatesReceived++;
        if (mUpdatesReceived == LOCATION_DESIRED_UPDATES) {
            reportLocation(location);
            this.stopSelf();
        }

    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    } 
    
    private void logd(String message) {
        Logger.d("LocationSaverService: " + message);
    }
    
    private void reportLocation(Location location) {              
    	if (mAgentGuid != null) {
    		Agent agent = AgentFactory.getAgentFromGuid(this, mAgentGuid);
    		if ((agent != null) && (agent instanceof LocationSaverInterface)) {
    			((LocationSaverInterface) agent).gotLocation(location, mTriggerType);
    		}
    	}
    }

}
