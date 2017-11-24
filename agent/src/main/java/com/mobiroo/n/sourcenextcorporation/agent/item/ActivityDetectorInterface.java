package com.mobiroo.n.sourcenextcorporation.agent.item;

public interface ActivityDetectorInterface {
	public boolean needsActivityDetection();
	public void resetActivityDetection();
	public Class<?> getActivityReceiverClass();
}
