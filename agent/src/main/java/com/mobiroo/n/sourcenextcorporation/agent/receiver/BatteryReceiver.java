package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.BatteryAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.SleepAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;


public class BatteryReceiver extends BroadcastReceiver {

    public static final String PREF_POWER_CONNECTED_HISTORY = "BatteryReceiverPCHistory";
    public static final String PREF_POWER_BAD_CORD_AT = "BatteryReceiverBadCordAt";

    @Override

    public void onReceive(Context context, Intent intent) {    	


        long now = System.currentTimeMillis();

        // for power connected/disconnected, ensure broken power cord that constantly toggles
        // is ignored after a while
    	if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())  || Intent.ACTION_POWER_DISCONNECTED.equals(intent.getAction())) {
            String[] prevs = PrefsHelper.getPrefString(context, PREF_POWER_CONNECTED_HISTORY, "").split(AgentPreferences.STRING_SPLIT);
            if (prevs.length != 8) {prevs = new String[] {"0", "0", "0", "0", "0", "0", "0", "0"};}

            long eightAgo = Long.valueOf(prevs[0]);

            String newPrefVal = "";
            for (int i=1; i < prevs.length; i++) {
                newPrefVal += prevs[i] + AgentPreferences.STRING_SPLIT;
            }
            newPrefVal += String.valueOf(now);
            PrefsHelper.setPrefString(context, PREF_POWER_CONNECTED_HISTORY, newPrefVal);

            if ((now - eightAgo) < AlarmManager.INTERVAL_FIFTEEN_MINUTES) {
                Logger.d("BatteryReceiver: got more than 8 power connect/disconnect updates in 15 minutes.  Ignoring.");
                PrefsHelper.setPrefLong(context, PREF_POWER_BAD_CORD_AT, System.currentTimeMillis());
                return;
            }

    	} else {
            // do 10 minute cooloff on any updates that aren't power connected/disconnected
            // if we suspect a bad power cord, i.e. PREF_POWER_BAD_CORD_AT is set recently
            long powerPausedAt = PrefsHelper.getPrefLong(context, PREF_POWER_BAD_CORD_AT, 0);
            if ((now - powerPausedAt) < 600000) {
                Logger.d("BatteryReceiver: still in cooloff after too many power connect/disconnects.  Ignoring.");
                return;
            }
        }


        // check if SA should start back up on power connected
        if (Intent.ACTION_POWER_CONNECTED.equals(intent.getAction())) {
            Logger.d("Attempting to start sleep agent on power connected");
            // should fail on preconditions (e.g. times) a lot; that should be OK
            DbAgent.setActive(context, SleepAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_TIME);
        }


        BatteryAgent.checkActive(context);

        int level = intent.getIntExtra("level", -1);
        double scale = intent.getIntExtra("scale", -1);

        if ((level == -1) || (scale == -1)) {
            IntentFilter filter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batt=context.getApplicationContext().registerReceiver(null,filter);
            if (batt != null) {
                level = batt.getIntExtra("level", 0);
                scale = batt.getIntExtra("scale", -1);
            }
        }

    }
        
}


