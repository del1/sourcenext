package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.ParkingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.service.DrivingService;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

public class ParkingAgentActivityReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
    	if (intent == null) { return;}

        String action = intent.getAction();
        Logger.d(String.format("PAAR: action=%s",action));

        if (DrivingService.ACTION_DRIVING_STOPPED.equals(action)) {
            DbAgent.setActive(context, ParkingAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_PARKING);
        } else if (DrivingService.ACTION_DRIVING_STARTED.equals(action)) {
            DbAgent.setInactive(context, ParkingAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_PARKING);
        }

    }

}
