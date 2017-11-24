package com.mobiroo.n.sourcenextcorporation.agent.item;

import android.location.Location;

public interface LocationSaverInterface {
	public void gotLocation(Location location, int triggerType);
}
