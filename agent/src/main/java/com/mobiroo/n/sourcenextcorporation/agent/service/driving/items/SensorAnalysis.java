package com.mobiroo.n.sourcenextcorporation.agent.service.driving.items;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

/**
 * Created by krohnjw on 3/6/14.
 */
public class SensorAnalysis {
    public float average;
    public float max;
    public float min;

    private int count;
    private float sum;

    private final double THRESH_SIGNIFICANT_SMALLEST = 0.1;
    private final double THRESH_SIGNIFICANT_SMALL = 0.3;
    private final double THRESH_SIGNIFICANT_MEDIUM = 0.8;
    private final double THRESH_SIGNIFICANT_LARGE = 2;
    private final double THRESH_SIGNIFICANT_VERY_LARGE = 5;

    public int count_smallest;
    public int count_small;
    public int count_medium;
    public int count_large;
    public int count_very_large;

    public SensorAnalysis() {
        average = 0;
        sum = 0;
        count = 0;
        max = 0;
        min = 999;
        count_smallest = 0;
        count_small = 0;
        count_medium = 0;
        count_large = 0;
        count_very_large = 0;
    }

    public void add(float value) {

        if (value > max) {
            max = value;
        }

        if (value < min) {
            min = value;
        }

        count++;
        sum += value;

        average = sum / count;
    }

    public void checkDelta(float current, float previous) {
        float delta = Math.abs(current - previous);
        if (delta > THRESH_SIGNIFICANT_VERY_LARGE) {
            count_very_large++;
        } else if (delta > THRESH_SIGNIFICANT_LARGE) {
            count_large++;
        } else if (delta > THRESH_SIGNIFICANT_MEDIUM) {
            count_medium++;
        } else if (delta > THRESH_SIGNIFICANT_SMALL) {
            count_small++;
        } else if (delta > THRESH_SIGNIFICANT_SMALLEST) {
            count_smallest++;
        }
    }

    public float getDeltaAsPercentage() {
        Logger.d("Max: " + max + ", Min: " + min + " - " + average);
        return Math.abs(Math.abs(max - min) / average);
    }
}