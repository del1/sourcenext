package com.mobiroo.n.sourcenextcorporation.agent.service.driving.items;

/**
 * Created by krohnjw on 3/6/14.
 */
public class SensorData {
    public float x;
    public float y;
    public float z;
    public long time;

    public SensorData(float x, float y, float z, long time) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
    }
}
