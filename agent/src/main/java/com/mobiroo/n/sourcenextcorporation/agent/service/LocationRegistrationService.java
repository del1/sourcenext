package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

/**
 * Created by krohnjw on 6/19/2014.
 */
public class LocationRegistrationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private final long INTERVAL_LOCATION_UPDATE_MS = 2 * 60 * 1000;
    private final float MINIMUM_DISPLACEMENT_M = 40f;
    private final long LOCATION_REQUEST_EXPIRATION_MS = 60 * 60 * 1000; // 1hr

    private GoogleApiClient mClient;

    @Override
    public void onConnected(Bundle bundle) {
        LocationServices.FusedLocationApi
                .requestLocationUpdates(mClient,
                        new LocationRequest()
                                .setPriority(LocationRequest.PRIORITY_NO_POWER)
                                .setExpirationDuration(LOCATION_REQUEST_EXPIRATION_MS) // Expire in 1 hour - re-register at this time
                                .setSmallestDisplacement(MINIMUM_DISPLACEMENT_M) // 50 meter minimum displacement
                                .setFastestInterval(INTERVAL_LOCATION_UPDATE_MS)
                        , PendingIntent.getService(getApplicationContext(), PassiveLocationIntentService.REQUEST_CODE_FUSED_PROVIDER, new Intent(getApplicationContext(), PassiveLocationIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT)
                );

        // Schedule a wakeup to re-request
        AlarmManager m = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        m.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60 * 60 * 1000, AlarmManager.INTERVAL_HOUR,
                PendingIntent.getService(getApplicationContext(), 9873, new Intent(getApplicationContext(), LocationRegistrationService.class), PendingIntent.FLAG_UPDATE_CURRENT));

        mClient.disconnect();
        stopSelf();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Logger.d("FUSED PROVIDER LOCATION CHANGED");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int id) {

        Logger.d("Registering location services");
        /* Request passive updates from Fused Provider */
        mClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build();

        mClient.connect();
        return START_NOT_STICKY;
    }
}
