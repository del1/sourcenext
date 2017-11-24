package com.mobiroo.n.sourcenextcorporation.agent.service.driving.lists;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Created by krohnjw on 3/6/14.
 */
public class ActivityList extends QueueList<ActivityRecognitionResult> {

    // Sets the minimum confidence in driving needed to spin up the driving service and assist detection

    public static final int MINIMUM_DRIVING_START_CONFIDENCE = 65; // 85
    public static final int MINIMUM_DRIVING_CONTINUATION_CONFIDENCE = 65;
    public static final int MINIMUM_DRIVING_ASSIST_CONFIDENCE = 35;
    public static final int MINIMUM_DRIVING_LOCATION_ASSIST_CONFIDENCE = 50; //60 // was 25


    public static final int MINIMUM_WALKING_CONFIDENCE = 80;

    @Override
    protected int getMaxNodes() {
        return 3;
    }

    public DetectedActivity getLatestActivity() {
        if (tail == null) logd("ACL-GLA: Tail is null, skipping check");

        return tail.data.getMostProbableActivity();
    }

    public int getLatestConfidence() {
        if (tail == null) return 0;

        return tail.data.getMostProbableActivity().getConfidence();
    }

    public int getLatestActivityType() {
        if ((tail == null) || (tail.data == null)) { return DetectedActivity.UNKNOWN; }

        return tail.data.getMostProbableActivity().getType();
    }

    // Is currently ONLY used to detect driving started
    public boolean isLatestDriving() {
        if (getNodeCount() == 0) {
            return false;
        }
        return ((tail.data.getMostProbableActivity().getType() == DetectedActivity.IN_VEHICLE)
                && (tail.data.getMostProbableActivity().getConfidence() > MINIMUM_DRIVING_START_CONFIDENCE));
    }

    public int getDrivingConfidence() {
        int c = (isLatestDriving() && wasPreviousDriving()) ? 1: 0;
        logd("Driving confidence: " + c);
        return c;
    }

    // Is currently ONLY used to detect driving started
    public boolean wasPreviousDriving() {
        if ((getNodeCount() == 0) || (getNodeCount() < 2)) {
            return false;
        }

        // Grab the second from the last node
        Node<ActivityRecognitionResult> penultimate = getPenultimateNode();

        return (penultimate != null) && ((penultimate.data.getMostProbableActivity().getType() == DetectedActivity.IN_VEHICLE)
                && (penultimate.data.getMostProbableActivity().getConfidence() > MINIMUM_DRIVING_LOCATION_ASSIST_CONFIDENCE));
    }

    public boolean containsDriving() {
        if (getNodeCount() == 0) {
            return false;
        }
        return containsDriving(tail.data, MINIMUM_DRIVING_LOCATION_ASSIST_CONFIDENCE);
    }

    public boolean containsDrivingHint() {
        if (getNodeCount() == 0) {
            return false;
        }
        return containsDriving(tail.data, MINIMUM_DRIVING_ASSIST_CONFIDENCE);
    }

    public boolean containsDriving(ActivityRecognitionResult result, int min_confidence) {
        boolean hasDriving = false;
        for (DetectedActivity a : result.getProbableActivities()) {
            hasDriving |= (((a.getType() == DetectedActivity.IN_VEHICLE) && (a.getConfidence() > min_confidence))
                    && ((result.getMostProbableActivity().getType() == DetectedActivity.UNKNOWN) ||
                    (result.getMostProbableActivity().getType() == DetectedActivity.IN_VEHICLE)));
        }

        logd("ACL-CD: Should assist: " + hasDriving);
        return hasDriving;
    }

    public boolean containsDriving(Node<ActivityRecognitionResult> node) {
        return (node != null) ? containsDriving(node.data, MINIMUM_DRIVING_ASSIST_CONFIDENCE) : false;
    }

    public boolean isWalking() {
        if (getNodeCount() == 0) {
            return false;
        }
        // Accept on_foot, walking, or running above our min confidence level
        return ((tail.data.getMostProbableActivity().getType() == DetectedActivity.ON_FOOT
                    || tail.data.getMostProbableActivity().getType() == DetectedActivity.RUNNING
                    || tail.data.getMostProbableActivity().getType() == DetectedActivity.WALKING)
                && (tail.data.getMostProbableActivity().getConfidence() > MINIMUM_WALKING_CONFIDENCE));
    }
}
