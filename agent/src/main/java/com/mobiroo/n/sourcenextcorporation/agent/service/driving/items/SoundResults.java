package com.mobiroo.n.sourcenextcorporation.agent.service.driving.items;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

/**
 * Created by krohnjw on 6/3/2014.
 */
public class SoundResults {

    private boolean foundDriving;
    private long time;

    public SoundResults(boolean foundDriving) {
        this.foundDriving = foundDriving;
        time = System.currentTimeMillis();
    }

    public long getTime() {
        return time;
    }

    public boolean isInCar() {
        Logger.d("DRIVING-SERVICE: returning in car " + foundDriving);
        return foundDriving;
    }

    public boolean isFresh() {
        Logger.d("DRIVING-SERVICE: returning isFresh: " + ((System.currentTimeMillis() - time) < 30000));
        return (System.currentTimeMillis() - time) < 30000;
    }

    public int getDrivingConfidence() {
        int c = (isFresh() && isInCar()) ? 1: 0;
        Logger.d("DRIVING-SERVICE: Sound driving confidence " + c);
        return c;
    }
}
