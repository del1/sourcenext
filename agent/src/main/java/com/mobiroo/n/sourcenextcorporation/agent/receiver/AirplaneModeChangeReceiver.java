package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.ActivityRecognitionHelper;

public class AirplaneModeChangeReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.d("AirplaneModeChangeReceiver");

		if (Utils.isAirplaneModeOn(context)) {
			Logger.d("Shutting down AD in airplane mode.");
			ActivityRecognitionHelper.stopAllActivityRecognition(context);
		} else {
			Logger.d("Starting up AD out of airplane mode (if needed).");
			ActivityRecognitionHelper.startActivityRecognitionIfNeeded(context);
		}
	}

}
