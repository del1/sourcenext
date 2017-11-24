package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.ParkingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.widget.AgentToggleWidgetProvider;


public class WidgetIntentService extends IntentService {
	
	public static final String ACTION = "widget_intent_action";
	public static final String AGENT_GUID = MainActivity.EXTRA_AGENT_GUID;

    public static final String WIDGET_PARKING_AGENT = "ParkingAgentWidget";

	public static final int ACTION_UNKNOWN = -1;
	public static final int ACTION_TOGGLE = 1;

	public WidgetIntentService() {
		super("WidgetIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent == null) { return;}
		
		int action = intent.getIntExtra(ACTION, ACTION_UNKNOWN);
		if (action != ACTION_TOGGLE) {
			Logger.d("WidgetIntentService: action not toggle");
			return;
		}
		
		String agentGuid = intent.getStringExtra(AGENT_GUID);
		final Agent agent = AgentFactory.getAgentFromGuid(this, agentGuid);
		if (agent == null) {
			Logger.d("WidgetIntentService: null agent");
			return;
		}
		if (! agent.isInstalled()) {
			Logger.d("WidgetIntentService: agent is not enabled.");
			// should not be called; widget opens up app instead
			return;
		}

        if (ParkingAgent.HARDCODED_GUID.equals(agent.getGuid())) {
            Intent launchIntent = new Intent(this, MainActivity.class);
            launchIntent.putExtra(MainActivity.EXTRA_LAUNCH_CONFIG, true);
            launchIntent.putExtra(MainActivity.EXTRA_AGENT_GUID, agent.getGuid());
            launchIntent.putExtra(MainActivity.EXTRA_FROM_WIDGET, WIDGET_PARKING_AGENT);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
            return;
        }

		int[] ids = AgentToggleWidgetProvider.getIdsForAgentGuid(this, agentGuid);
		RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget_agent_toggle_progress);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);

		for(int id: ids) {
			manager.updateAppWidget(id, views);		
		}
		
		if (agent.isActive()) {
			agent.pause(this);
			return;
		}
		
		if (agent.isPaused()) {
			agent.unPause(this);
			return;
		}
		
		DbAgent.setActive(this, agentGuid, Constants.TRIGGER_TYPE_MANUAL);

        try {
            final Context context = this;
            Handler h = new Handler(context.getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    String toastMsg = String.format(context.getResources().getString(R.string.agent_manual_start_toast), agent.getName());
                    Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Logger.e("WidgetIntentService: error in creating toast:" + e.getMessage());
        }
    }

}
