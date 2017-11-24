package com.mobiroo.n.sourcenextcorporation.agent.util.location;

import android.content.Context;
import android.location.Location;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by krohnjw on 6/25/2014.
 */
public class LocationHelper {

    public static long MAX_AGE_MS = 1 * 60 * 1000;
    public static final float DEFINITELY_INSIDE = 100;
    public static final String CUSTOM_PROVIDER = "AGENT_LOCATION_PROVIDER";

    public static String getLocationLogString(Location l) {
        SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
        return "[" + l.getLatitude() + ", " + l.getLongitude() + ", " + l.getAccuracy() + "m, " + f.format(new Date(l.getTime())) + "]";
    }

    public static boolean isLocationFresh(Location l) {
        return (System.currentTimeMillis() - l.getTime()) < MAX_AGE_MS;
    }

    public static boolean isLocationAccurateEnough(Location l) {
        return l.hasAccuracy() && (l.getAccuracy() < 0.66* DEFINITELY_INSIDE);
    }

    public static synchronized void clearLocation(Context context) {
        PrefsHelper.setPrefString(context, Constants.PREF_LAST_KNOWN_LOCATION, "");
    }

    public static synchronized void saveLocation(Context context, Location l) {
        PrefsHelper.setPrefString(context, Constants.PREF_LAST_KNOWN_LOCATION,
                l.getLatitude()
                        + Constants.STRING_SPLIT + l.getLongitude()
                        + Constants.STRING_SPLIT + l.getAccuracy()
                        + Constants.STRING_SPLIT + l.getTime()
                        + Constants.STRING_SPLIT + l.getSpeed()
        );
    }

    public static synchronized Location getLastLocation(Context context) {
        String s = PrefsHelper.getPrefString(context, Constants.PREF_LAST_KNOWN_LOCATION, "");
        if ((s == null) || (s.isEmpty())) {
            return null;
        }

        String[] data = s.split(Constants.STRING_SPLIT);
        if (data.length < 4) {
            return null;
        }

        Location l = new Location(CUSTOM_PROVIDER);

        try {
            l.setLatitude(Double.parseDouble(data[0]));
            l.setLongitude(Double.parseDouble(data[1]));
            l.setAccuracy(Float.parseFloat(data[2]));
            l.setTime(Long.parseLong(data[3]));
            l.setSpeed(Float.parseFloat(data[4]));
        } catch (Exception e) {
            Logger.d("LocationHelper: Exception parsing last known location: " + e);
            return null;
        }

        return l;
    }

    public static float getDistanceBetween(Location loc1, Location loc2) {
        float[] results = new float[3];

        Location.distanceBetween(loc1.getLatitude(),
                loc1.getLongitude(),
                loc2.getLatitude(),
                loc2.getLongitude(),
                results);

        return results[0];
    }
}
