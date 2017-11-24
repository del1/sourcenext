package com.mobiroo.n.sourcenextcorporation.agent.service.driving.lists;

import android.location.Location;

/**
 * Created by krohnjw on 3/6/14.
 */
public class LocationList extends QueueList<Location> {
    @Override
    protected int getMinNodes() {
        return 2;
    }

    @Override
    protected int getMaxNodes() {
        return 3;
    }

    public float getDistanceBetweenLocations(Location start, Location end) {
        float[] results = new float[3];

        Location.distanceBetween(
                start.getLatitude(),
                start.getLongitude(),
                end.getLatitude(),
                end.getLongitude(),
                results);

        return results[0];
    }

    public boolean isLocationNewer(Location l) {
        return (tail == null) || (tail.data.getTime() < l.getTime());
    }

    public boolean isFresh() {
        if (tail == null) return false;
        return ((System.currentTimeMillis() - tail.data.getTime()) < 30000);
    }

    public int getDrivingConfidence() {
        int confidence = 0;
        if (isFresh()) {
            confidence = isDriving();
        }
        return confidence;
    }

    public int isDriving() {
        if (getNodeCount() == 0) {
            return 0;
        }

        Location latest = tail.data;

        // Don't use stale location data (30s)
        long latest_age = System.currentTimeMillis() - tail.data.getTime();
        if (latest_age > 30 * 1000) return 0;

        int confidence = 0;

        // If we have a speed attached use that as the preferred method of validation
        if (latest.hasSpeed()) {
            float speed = latest.getSpeed();
            logd("Location has speed of " + speed);
            if (speed >= 6.5 && speed < 65) {
                // 15 mph - 145 mph
                confidence += 1;
            }
        }

        // No speed was found, calculate displacement between two data points if we have two valid data points
        if (getNodeCount() >= 2) {
            Node<Location> node = getPenultimateNode();
            if (node != null) {
                if (node.data.hasSpeed()) {
                    float speed = node.data.getSpeed();
                    logd("Previous location has speed of " + speed);
                    if (speed >= 6.5 && speed < 65) {
                        // 15 mph - 145 mph
                        confidence += 1;
                    }

                    if (confidence == 2) {
                        logd("Returning driving confirmation");
                        return confidence;
                    }
                } else {

                    float distance = getDistanceBetweenLocations(node.data, latest);
                    long time_between = latest.getTime() - node.data.getTime();

                    // Calculate the distance between the last two returned locations and flag driving if distance is above N value
                    logd("Locations are " + distance + " m  and " + time_between + " ms apart");

                    // Ensure that our location samples are not too far apart (> 60s).  Given our 10s polling interval data points
                    // this far apart are not likely to be part of the same location request and may give invalid results
                    if (time_between > 60 * 1000) return confidence;

                    // We may need to consider location accuracy here when calculating distance and invalidate if locations are TOO imprecise
                    logd("Accuracies are " + latest.getAccuracy() + " and " + node.data.getAccuracy());

                    // Looking for distance to meet or exceed 7m/s (16mp) over the given time interval
                    double rate = distance / (time_between / 1000);

                    logd("Calculated rate of travel is " + rate + " m/s");
                    if (rate >= 6.5 && rate <= 100) {
                        confidence += 1;
                    }
                }
            }
        }

        logd("Driving confidence " + confidence);
        return confidence;
    }
}