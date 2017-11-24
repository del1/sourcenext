package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.location.LocationServices;
import com.mobiroo.n.sourcenextcorporation.agent.util.location.LocationHelper;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

/**
 * Created by krohnjw on 6/19/2014.
 */
public class PassiveLocationIntentService extends IntentService {

    public static final int REQUEST_CODE_LOCATION_MANAGER   = 8543;
    public static final int REQUEST_CODE_FUSED_PROVIDER     = 8544;

    private void logd(String message) {
        Logger.d("PassiveLocationIntentService: " + message);
    }
    public PassiveLocationIntentService() {
        super("PassiveLocationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Location l = null;
        String provider = "Unknown";
        if (intent.hasExtra(LocationManager.KEY_LOCATION_CHANGED)) {
            /* Update from LocationManager */
            provider = "LocationManager";
            l = (Location) intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED);
        } else if (intent.hasExtra(LocationServices.FusedLocationApi.KEY_LOCATION_CHANGED)) {
            provider = "FusedProvider";
            l = (Location) intent.getParcelableExtra(LocationServices.FusedLocationApi.KEY_LOCATION_CHANGED);
        }

        if (l != null) {
            if (!LocationHelper.isLocationAccurateEnough(l)) {
                logd("New location not accurate enough from " + provider + ": " + LocationHelper.getLocationLogString(l));
                return;
            }

            Location oldLocation = LocationHelper.getLastLocation(this);
            LocationHelper.saveLocation(this, l);

            float distance = 100000;  // if no old location, set to 100km
            if (oldLocation != null) {
                distance = LocationHelper.getDistanceBetween(oldLocation, l);
            }
            logd("New location from " + provider + ": " + LocationHelper.getLocationLogString(l) + ".  Distance from saved location = " + distance + "m.");
        }
    }
}
