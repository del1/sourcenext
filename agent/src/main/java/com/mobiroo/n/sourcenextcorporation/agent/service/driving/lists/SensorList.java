package com.mobiroo.n.sourcenextcorporation.agent.service.driving.lists;

import com.mobiroo.n.sourcenextcorporation.agent.service.driving.items.SensorAnalysis;
import com.mobiroo.n.sourcenextcorporation.agent.service.driving.items.SensorData;

import java.util.Iterator;

/**
 * Created by krohnjw on 3/6/14.
 */
public class SensorList extends QueueList<SensorData> {

    @Override
    protected int getMinNodes() {
        return 10;
    }

    @Override
    protected int getMaxNodes() {
        return 30;
    }

    public SensorAnalysis[] getAggregateData() {
        SensorAnalysis[] data = new SensorAnalysis[3];

        data[0] = new SensorAnalysis();
        data[1] = new SensorAnalysis();
        data[2] = new SensorAnalysis();

        Iterator iterator = nodes.iterator();

        Node<SensorData> prev = null;
        while (iterator.hasNext()) {
            Node<SensorData> node = (Node<SensorData>) iterator.next();
            data[0].add(node.data.x);
            data[1].add(node.data.y);
            data[2].add(node.data.z);
            if (prev != null) {
                data[0].checkDelta(node.data.x, prev.data.x);
                data[1].checkDelta(node.data.y, prev.data.y);
                data[2].checkDelta(node.data.z, prev.data.z);
            }
            prev = node;
        }

        return data;
    }

    public boolean isWalking() {

        if (getNodeCount() > getMinNodes()) {
                /* We have 5+ seconds of accelerometer data */
            return isWalking(getAggregateData());
        }

        return false;
    }

    public boolean isDriving() {
        if (getNodeCount() > getMinNodes()) {
            return isDriving(getAggregateData());
        }

        return false;
    }

    final int WALKING_MIN_VERY_LARGE_COUNT = 1;
    public boolean isWalking(SensorAnalysis[] data) {

        // Consider walking if data points for     THRESH_MEDIUM make up more than 60% of points for that axis
        //StringBuilder sb = new StringBuilder();

        int x_sum = data[0].count_very_large + data[0].count_large + data[0].count_medium;
        int x_very_large = data[0].count_very_large;
        //sb.append("x:[" + data[0].count_very_large + "," + data[0].count_large + "," + data[0].count_medium + "], ");
        int y_sum = data[1].count_very_large + data[1].count_large + data[1].count_medium;
        int y_very_large = data[1].count_very_large;
        //sb.append("y:[" + data[1].count_very_large + "," + data[1].count_large + "," + data[1].count_medium + "], ");
        int z_sum = data[2].count_very_large + data[2].count_large + data[2].count_medium;
        int z_very_large = data[2].count_very_large;
        //sb.append("z:[" + data[2].count_very_large + "," + data[2].count_large + "," + data[2].count_medium + "], ");

        //sb.append("tn: " + getNodeCount() + ", [" + x_sum + "," + y_sum + "," + z_sum + "]");
        //logd("SL-IW: " + sb.toString());

        double x_pct = ((double) x_sum / getNodeCount());
        double y_pct = ((double) y_sum / getNodeCount());
        double z_pct = ((double) z_sum / getNodeCount());

        final double WALKING_PERCENTAGE = 0.55; // Try upping this to 0.5X or 0.6 and see if we can still trip the percentage while ACTUALLY walking.

        boolean walking =
                (x_pct >= WALKING_PERCENTAGE) && (y_pct >= WALKING_PERCENTAGE) && (z_pct >= WALKING_PERCENTAGE)
                && (x_very_large > WALKING_MIN_VERY_LARGE_COUNT) && (y_very_large > WALKING_MIN_VERY_LARGE_COUNT) && (z_very_large > WALKING_MIN_VERY_LARGE_COUNT);


        walking &= !isRotating(data);
        logd("SL-IW: Walking = " + walking + " - " + x_pct + ", " + y_pct + ", " + z_pct + " - VL: " + x_very_large + ", " + y_very_large + ", " + z_very_large);

        return walking;

    }

    public boolean isRotating(SensorAnalysis[] data) {
        boolean rotating = false;

        final float ROTATION_FLOOR = 6;

        float x_delta = Math.abs(data[0].max - data[0].min);
        float y_delta = Math.abs(data[1].max - data[1].min);
        float z_delta = Math.abs(data[2].max - data[2].min);

        // For rotation in testing I'm seeing 2 our of 3 axes with LARGE difference in min / max accel readings and one much smaller.  Setting a floor
        // of 6.  If we have two axes over and one under we'll consider this rotation
        int count_over = 0;
        int count_under = 0;

        if (x_delta > 6) {
            count_over++;
        } else {
            count_under++;
        }

        if (y_delta > 6) {
            count_over++;
        } else {
            count_under++;
        }

        if (z_delta > 6) {
            count_over++;
        } else {
            count_under++;
        }

        rotating = (count_over == 2) && (count_under == 1);

        logd("SL-IR: Rotation check found " + count_over + " over and " + count_under + " under " + ROTATION_FLOOR);

        return rotating;
    }

    public boolean isDriving(SensorAnalysis[] data) {
            /* Our data shows that driving has delta as a % of average
             data for one axis in a larger value (1.5+) while the other 2 are significantly
             above still (> 0.1).
              */

        /*int x_sum = data[0].count_very_large + data[0].count_large + data[0].count_medium + data[0].count_small;
        int y_sum = data[1].count_very_large + data[1].count_large + data[1].count_medium + data[1].count_small;
        int z_sum = data[2].count_very_large + data[2].count_large + data[2].count_medium + data[2].count_small;

        logd("SL-ID: Total Nodes: " + getNodeCount() + " x: " + x_sum + " y: " + y_sum + " z:" + z_sum);

        double x_pct = ((double) x_sum / getNodeCount());
        double y_pct = ((double) y_sum / getNodeCount());
        double z_pct = ((double) z_sum / getNodeCount());


        final double MIN_DRIVING_PERCENTAGE = 0.6;
        final double MAX_DRIVING_PERCENTAGE = 0.8;

        boolean driving = (x_pct >= MIN_DRIVING_PERCENTAGE) && (x_pct < MAX_DRIVING_PERCENTAGE)
                && (y_pct >= MIN_DRIVING_PERCENTAGE) && (y_pct < MAX_DRIVING_PERCENTAGE)
                && (z_pct < MAX_DRIVING_PERCENTAGE) && (z_pct >= MIN_DRIVING_PERCENTAGE);

        logd("SL-ID: Driving = " + driving + " - " + x_pct + ", " + y_pct + ", " + z_pct);*/
        //return driving;
        return false; // Saving logic if we ever want to re-implement
    }

}
