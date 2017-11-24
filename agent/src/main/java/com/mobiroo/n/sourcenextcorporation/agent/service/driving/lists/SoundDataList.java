package com.mobiroo.n.sourcenextcorporation.agent.service.driving.lists;


import com.mobiroo.n.sourcenextcorporation.agent.service.driving.items.Complex;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by krohnjw on 5/27/2014.
 */
public class SoundDataList extends QueueList<Complex[]> {

    final static int SAMPLE_RATE = 8000;


    final double PCT_STATIONARY_LOWER_MIN = 0.70;
    final double PCT_STATIONARY_MID_MAX = 0.11;
    final double PCT_STATIONARY_UPPER_MAX = 0.18;

    final double PCT_DRIVING_LOWER_MIN = 0.95;
    final double PCT_DRIVING_MID_MAX = 0.35;
    final double PCT_DRIVING_UPPER_MAX = 0.35;

    final double PCT_QUIET_STATIONARY_LOWER_MIN = 0.65;
    final double PCT_QUIET_STATIONARY_MID_MAX = 0.05;
    final double PCT_QUIET_STATIONARY_UPPER_MAX = 0.15;

    final int MAX_LOWER_FREQUENCY_ACCPTED = 500;
    final int MAX_MID_FREQUENCY_ACCPTED = 50;

    final long FREQUENCY_LOWER_REGISGER_TOP_BOUND = 200;
    final long FREQUENCY_MID_REGISTER_LOWER_BOUND = 300;
    final long FREQUENCY_MID_REGISTER_UPPER_BOUND = 1700;

    double FREQUENCY_LOWER_THRESH = 2;
    double FREQUENCY_UPPER_THRESH = 2;

    private boolean last_result = false;

    public class Analysis {
        public boolean driving;
        public String data;

        public Analysis(boolean driving, String data) {
            this.data = data;
            this.driving = driving;
        }
    }


    @Override
    protected int getMinNodes() {
        return 5;
    }

    @Override
    protected int getMaxNodes() {
        return 30;
    }

    final boolean logAnalysis = false;

    public Analysis isInCar() {

        if (getNodeCount() < getMinNodes()) { return new Analysis(false, "Not enough nodes."); }

        Iterator iterator = nodes.iterator();
        StringBuilder output = new StringBuilder();
        int confidence = 0;

        // Iterate over the full node list and build a data set of frequency, value, count to average data
        while (iterator.hasNext()) {
            Node<Complex[]> node = (Node<Complex[]>) iterator.next();
            Complex[] data = node.data;

            HashMap<Integer, Integer> median = new HashMap<Integer, Integer>();
            for (int i = 0; i < data.length; i += 2) {
                Complex c = data[i];
                double magnitude = Math.sqrt(c.re() * c.re() + c.im() * c.im()) / 1000;
                // Frequency = i/2 * SAMPLE_RATE / bytes.lengh/2
                long frequency = (i / 2) * SAMPLE_RATE / (data.length);

                if ((frequency >= FREQUENCY_MID_REGISTER_LOWER_BOUND) && (frequency <= FREQUENCY_MID_REGISTER_UPPER_BOUND)) {
                    int ceiling = (int) Math.ceil(magnitude);
                    if (median.containsKey(ceiling)) {
                        median.put(ceiling, median.get(ceiling) + 1);
                    } else {
                        //logd("Adding ceiling " + ceiling);
                        median.put(ceiling, 1);
                    }
                }
            }

            // We should now be able to find the median ceiling value for all data points in the "mid range" window (which should be relatively stable and silent in our desired case)
            int ceiling = 0;
            int ceiling_count = 0;
            for (int c : median.keySet()) {
                int count = median.get(c);
                //logd(c + "=" + count);
                if (count > ceiling_count) {
                    ceiling_count = count;
                    ceiling = c;
                }
            }

            double divisor = 1;

            ceiling += 1; // Pad up 1 to contain more values
            if (logAnalysis) {
                logd("Mid range ceiling is " + ceiling);
                output.append("Mid range ceiling is " + ceiling + "\n");
            }
            if (ceiling > 2) {
                // Adjust ceiling down to 2 and use this value to scale all other magnitudes for analysis.
                //logd("Ceiling is set above 2");
                divisor = (double) ceiling / (double) 2;
            }

            // Walk data set again and see if adjusted values are within our bounds

            int lower_above = 0;
            int lower_below = 0;
            int lower_count = 0;
            double lower_max = 0;

            int lower_above_thresh_count = 0;
            int top_above_thresh_count = 0;

            int mid_above = 0;
            int mid_below = 0;
            int mid_count = 0;
            double mid_max = 0;
            double mid_total = 0;

            int top_above = 0;
            int top_below = 0;
            int top_count = 0;
            double top_max = 0;


            FREQUENCY_LOWER_THRESH = ceiling * 5;
            FREQUENCY_UPPER_THRESH = ceiling * 5;

            for (int i = 0; i < data.length; i += 2) {
                Complex c = data[i];
                double magnitude = Math.sqrt(c.re() * c.re() + c.im() * c.im()) / 1000;
                magnitude = magnitude / divisor;

                // Frequency = i/2 * SAMPLE_RATE / bytes.lengh/2
                long frequency = (i / 2) * SAMPLE_RATE / (data.length);

                if (frequency < FREQUENCY_LOWER_REGISGER_TOP_BOUND) {
                    lower_count += 1;
                    if (magnitude > ceiling) {
                        lower_above += 1;
                        if (magnitude > FREQUENCY_LOWER_THRESH) {
                            lower_above_thresh_count += 1;
                        }
                    } else {
                        lower_below += 1;
                    }

                    if (magnitude > lower_max) {
                        lower_max = magnitude;
                    }

                } else if ((frequency >= FREQUENCY_MID_REGISTER_LOWER_BOUND) && (frequency <= FREQUENCY_MID_REGISTER_UPPER_BOUND)) {
                    mid_count += 1;
                    if (magnitude < ceiling) {
                        mid_below += 1;
                    } else {
                        mid_above += 1;
                    }

                    if (magnitude > mid_max) {
                        mid_max = magnitude;
                    }

                } else {
                    // Upper register frequencies
                    top_count += 1;
                    if (magnitude < ceiling) {
                        top_below += 1;
                    } else {
                        top_above += 1;
                        if (magnitude > FREQUENCY_UPPER_THRESH) {
                            top_above_thresh_count += 1;
                        }
                    }

                    if (magnitude > top_max) {
                        top_max = magnitude;
                    }
                }
            }

            if (logAnalysis) {
                output.append("-----------------------\n");
                DecimalFormat f = new DecimalFormat("#.##");
                String s = String.format("Lower register data: Total: %s\nAbove: %s, Above pct %s\nBelow %s, Below pct %s,\nAbove thresh %s\nMax %s\n\n",
                        lower_count,
                        lower_above,
                        f.format(lower_above / (double) lower_count),
                        lower_below,
                        f.format(lower_below / (double) lower_count),
                        lower_above_thresh_count,
                        f.format(lower_max));
                output.append(s);
                logd(s);

                s = String.format("Mid register data: Total: %s\nAbove: %s, Above pct %s\nBelow %s, Below pct %s\nmid max %s\n\n",
                        mid_count,
                        mid_above,
                        f.format(mid_above / (double) mid_count),
                        mid_below,
                        f.format(mid_below / (double) mid_count),
                        f.format(mid_max));
                output.append(s);
                logd(s);

                s = String.format("Upper register data: Total: %s\nAbove: %s, Above pct %s\nBelow %s, Below pct %s\nAbove thresh %s\n\n",
                        top_count,
                        top_above,
                        f.format(top_above / (double) top_count),
                        top_below,
                        f.format(top_below / (double) top_count),
                        top_above_thresh_count);

                output.append(s);
                logd(s);
            }

            boolean result = false;
            // Assumptions.  Driving sees 90+ % of lower register data above thresh and sub 10% for each of mid and upper
            // This is for a stationary motor

            double lower_above_pct = lower_above / (double) lower_count;
            double mid_above_pct = mid_above / (double) mid_count;
            double top_above_pct = top_above / (double) top_count;

            if ((lower_above_pct >= PCT_STATIONARY_LOWER_MIN)
                    && (mid_above_pct <= PCT_STATIONARY_MID_MAX)
                    && (top_above_pct <= PCT_STATIONARY_UPPER_MAX)
                    && (lower_max < MAX_LOWER_FREQUENCY_ACCPTED)
                    && (lower_max > 30)) {
                // This should test for in a car but not driving (idling or stationary but engine running)
                confidence += 1;
                result = true;
            } else if ((lower_above_pct >= PCT_DRIVING_LOWER_MIN)
                    && (mid_above_pct <= PCT_DRIVING_MID_MAX)
                    && (top_above_pct <= PCT_DRIVING_UPPER_MAX)
                    && (lower_max < MAX_LOWER_FREQUENCY_ACCPTED)
                    && (mid_max < MAX_MID_FREQUENCY_ACCPTED)
                    && (lower_max > 30)) {
                // This should test for in a car and driving
                confidence += 1;
                result = true;
            } else if ((lower_above_pct >= PCT_QUIET_STATIONARY_LOWER_MIN)
                    && (mid_above_pct <= PCT_QUIET_STATIONARY_MID_MAX)
                    && (top_above_pct <= PCT_QUIET_STATIONARY_UPPER_MAX)
                    && (lower_max < MAX_LOWER_FREQUENCY_ACCPTED)
                    && (lower_max > 30)) {
                // Idling in car with a quieter engine.  We see less hits on the low end and very few on the middle
                confidence += 1;
                result = true;
            } else if (confidence > 0) {
                confidence -= 1;
            }

            output.append("In car confidence: " + confidence + "\n\n\n\n");
            if ((confidence > 4) || (!iterator.hasNext() && (confidence == 4) && (last_result && result))) {
                return new Analysis(true, output.toString());
            }

            last_result = result;
        }


        return new Analysis(false, output.toString());
    }
}
