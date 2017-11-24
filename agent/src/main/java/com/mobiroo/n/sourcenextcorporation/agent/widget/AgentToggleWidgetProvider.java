package com.mobiroo.n.sourcenextcorporation.agent.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.service.WidgetIntentService;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.MainActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

import java.util.ArrayList;

public class AgentToggleWidgetProvider extends AppWidgetProvider {
	protected static final String PREF_TOGGLE_WIDGET_GUID = "agentToggledWidgetGuid_";
	
	protected static final String AGENT_TOGGLE_WIDGET_ID_KEY = "AgentToggleWidgetProviderWidgetIds";

	public static int[] getIdsForAgentGuid(Context context, String agentGuid) {
		AppWidgetManager man = AppWidgetManager.getInstance(context);
		int[] ids = man.getAppWidgetIds(
	            new ComponentName(context, AgentToggleWidgetProvider.class));
		ArrayList<Integer> idsToUpdate = new ArrayList<Integer>();
		for(int id : ids) {
			String testGuid = getAgentGuidForWidgetId(context, id);
			if((testGuid != null) && (testGuid.equals(agentGuid))) {
				idsToUpdate.add(Integer.valueOf(id));
			}
		}
		int[] finalIds = new int[idsToUpdate.size()];
		for(int i=0; i<idsToUpdate.size(); i++) {
			finalIds[i] = idsToUpdate.get(i);
		}

		return finalIds;
	}
	
	public static void updateAgentWidgets(Context context, String agentGuid) {
		Intent updateIntent = new Intent();
	    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
	    updateIntent.putExtra(AgentToggleWidgetProvider.AGENT_TOGGLE_WIDGET_ID_KEY, getIdsForAgentGuid(context, agentGuid));
	    context.sendBroadcast(updateIntent);
	}
	
	public static void updateAgentWidgetsDelayed(Context context, String agentGuid, long msDelay) {
		int agentWidgetIds[] = getIdsForAgentGuid(context, agentGuid);
		if (agentWidgetIds.length < 1) { return;}

		Intent updateIntent = new Intent();
		updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);	    
		updateIntent.putExtra(AgentToggleWidgetProvider.AGENT_TOGGLE_WIDGET_ID_KEY, agentWidgetIds);

		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, agentWidgetIds[0], updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + msDelay, pendingIntent);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
	    if (intent.hasExtra(AGENT_TOGGLE_WIDGET_ID_KEY)) {
	        int[] ids = intent.getExtras().getIntArray(AGENT_TOGGLE_WIDGET_ID_KEY);
	        this.onUpdate(context, AppWidgetManager.getInstance(context), ids);
	    } else {
	    	super.onReceive(context, intent);
	    }
	}
	
	
	
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Logger.d("AgentToggleWidgetProvider: onUpdate()");
        for (int appWidgetId: appWidgetIds) {
        	updateAppWidgets(context, appWidgetManager, appWidgetId);        	
        }
    }
	
    public void onDeleted(Context context, int[] appWidgetIds) {
    	for (int appWidgetId: appWidgetIds) {
    		PrefsHelper.removePref(context, PREF_TOGGLE_WIDGET_GUID+String.valueOf(appWidgetId));
    	}
    }
	
	public static void setAgentGuidForWidgetId(Context context, String agentGuid, int widgetId) {
		PrefsHelper.setPrefString(context, PREF_TOGGLE_WIDGET_GUID+String.valueOf(widgetId), agentGuid);
	}
	
	public static String getAgentGuidForWidgetId(Context context, int widgetId) {
		return PrefsHelper.getPrefString(context, PREF_TOGGLE_WIDGET_GUID+String.valueOf(widgetId), null);
	}
	
	public static void updateAppWidgets(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
		Logger.d("updateAddWidgets called for widgetId: " + String.valueOf(appWidgetId));
        String widgetAgentGuid = getAgentGuidForWidgetId(context, appWidgetId);
        if (widgetAgentGuid == null) {
        	Logger.d("AgentToggleWidgetProvider: agentGuid not found for: " + String.valueOf(appWidgetId));
        	return;
        }
        
        Logger.d("AgentToggleWidgetProvider: " + widgetAgentGuid + " = " + String.valueOf(appWidgetId));
       
        Intent intent = new Intent(context, WidgetIntentService.class);
        intent.putExtra(WidgetIntentService.ACTION, WidgetIntentService.ACTION_TOGGLE);
        intent.putExtra(WidgetIntentService.AGENT_GUID, widgetAgentGuid);

        PendingIntent pendingIntent = 
        		PendingIntent.getService(context, appWidgetId , intent, PendingIntent.FLAG_UPDATE_CURRENT);
        

        Agent agent = AgentFactory.getAgentFromGuid(context, widgetAgentGuid);
        String statusText;
        int imageRes;
        
        RemoteViews views;
        

        if (!agent.isInstalled()) {
			views = new RemoteViews(context.getPackageName(), R.layout.widget_agent_toggle_dark);
			statusText = context.getResources().getString(
					R.string.agent_not_enabled);
			imageRes = agent.getWidgetOutlineIconId();

			// in the case that the agent is not installed, make the widget do something useful.
			intent = new Intent(context, MainActivity.class);
			pendingIntent = PendingIntent.getActivity(context, appWidgetId , intent, PendingIntent.FLAG_ONE_SHOT);
			
        } else if (agent.isActive()) { // is active
			views = new RemoteViews(context.getPackageName(), R.layout.widget_agent_toggle_dark);
			statusText = context.getResources().getString(
					R.string.agent_status_activated);
			imageRes = agent.getWidgetFillIconId();
		} else if (agent.isPaused()) { // is paused
			views = new RemoteViews(context.getPackageName(), R.layout.widget_agent_toggle_light);
			statusText = context.getResources().getString(
					R.string.agent_status_paused);
			
			imageRes = agent.getWidgetOutlineIconId();
		} else {
			views = new RemoteViews(context.getPackageName(), R.layout.widget_agent_toggle_light);
			statusText = context.getResources().getString(
					R.string.agent_status_idle);
			imageRes = agent.getWidgetOutlineIconId();
		}

        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent);

        views.setTextViewText(R.id.widget_status, statusText);
        views.setImageViewResource(R.id.app_icon, imageRes);
        
        appWidgetManager.updateAppWidget(appWidgetId, views);		
	}
}
