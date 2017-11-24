package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.DriveAgent;
import com.mobiroo.n.sourcenextcorporation.agent.service.DrivingService;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

public class DriveAgentActivityReceiver extends BroadcastReceiver {
	
	public static final String PREF_LAST_DRIVEN = "DAARLastDriven";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) { return;}

        String action = intent.getAction();
        Logger.d(String.format("DAAR: action=%s", action));
        if (DrivingService.ACTION_DRIVING_STARTED.equals(action)) {
            DbAgent.setActive(context, DriveAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_DRIVING);
        } else if (DrivingService.ACTION_DRIVING_STOPPED.equals(action)) {
            DbAgent.setInactive(context, DriveAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_DRIVING);
        }
    }

}
