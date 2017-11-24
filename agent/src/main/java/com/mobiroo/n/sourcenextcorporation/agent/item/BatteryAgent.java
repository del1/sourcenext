package com.mobiroo.n.sourcenextcorporation.agent.item;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentIntSliderSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentLabelSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentSpacerSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentWifiSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.OrChildCheck;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentNotificationInterface;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.BatteryReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.HashedNumberDispenser;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.util.WifiWrapper;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentCheckboxSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentPermission;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentUIElement;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;


public class BatteryAgent extends StaticAgent implements AgentNotificationInterface {
	public static final String HARDCODED_GUID = "tryagent.battery";

	protected static final int DEFAULT_TRIGGER_PERCENT = 10;
	protected static final int DEFAULT_BRIGHTNESS_DIM_PERCENT = 10;

    protected static final String PREF_PREV_BATTERY_LEVEL = "BatteryAgentPrevSavedLevel";


    protected String mBatteryFinishNotificationMessage;

	@Override
	public int getNameId() {
		return R.string.battery_agent_title;
	}

	@Override
	public int getDescriptionId() {
		return R.string.battery_agent_description;
	}

	@Override
	public int getIconId() {
		return R.drawable.ic_battery_agent;
	}

	@Override
	public int getWhiteIconId() {
		return R.drawable.ic_stat_battery_agent;
	}

	@Override
	public int getColorIconId() {
		return R.drawable.ic_battery_agent_color;
	}

	@Override
	public int getWidgetOutlineIconId() {
		return R.drawable.ic_widget_battery_inactive;
	}

	@Override
	public int getWidgetFillIconId() {
		return R.drawable.ic_widget_battery_active;
	}





	@Override
	public int getLongDescriptionId() {
		return R.string.battery_agent_description_long;
	}
	@Override
	public AgentPermission[] getTriggerArray() {
		AgentPermission[] agentPermissions = new AgentPermission[4];
		agentPermissions[0] = new AgentPermission(R.drawable.ic_battery_agent, R.string.battery_agent_trigger_bat);
		agentPermissions[1] = new AgentPermission(R.drawable.ic_sync, R.string.battery_agent_trigger_sync);
		agentPermissions[2] = new AgentPermission(R.drawable.ic_settings, R.string.battery_agent_trigger_screen);
		agentPermissions[3] = new AgentPermission(R.drawable.ic_bt, R.string.battery_agent_trigger_bt);

		return agentPermissions;
	}

	@Override
	public void afterInstall(Context context, boolean silent, boolean skipCheckReceivers) {
		SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();

		TaskDatabaseHelper.createAction(db, getGuid(), "BatteryAgentAction", Constants.TRIGGER_TYPE_BATTERY);
		TaskDatabaseHelper.createAction(db, getGuid(), "BatteryAgentAction", Constants.TRIGGER_TYPE_MANUAL);

        if (! skipCheckReceivers) {
            Utils.checkReceivers(context);
        }
	}


	@Override
	public HashMap<String, String> getPreferencesMap() {
		HashMap<String, String> prefs = super.getPreferencesMap();

		defaultPrefs(prefs, AgentPreferences.BATTERY_BT, String.valueOf(false));
		defaultPrefs(prefs, AgentPreferences.BATTERY_WIFI, String.valueOf(false));
		defaultPrefs(prefs, AgentPreferences.BATTERY_MOBILE_DATA, String.valueOf(false));

		defaultPrefs(prefs, AgentPreferences.BATTERY_DISPLAY, String.valueOf(true));
		defaultPrefs(prefs, AgentPreferences.BATTERY_BRIGHTNESS_LEVEL, String.valueOf(DEFAULT_BRIGHTNESS_DIM_PERCENT));

		defaultPrefs(prefs, AgentPreferences.BATTERY_SYNC, String.valueOf(true));

        defaultPrefs(prefs, AgentPreferences.WIFI_HOME_NAME, "");



        defaultPrefs(prefs, AgentPreferences.BATTERY_DISABLE_WHEN_CHARGING, String.valueOf(true));
		defaultPrefs(prefs, AgentPreferences.BATTERY_PERCENT_TRIGGER, String.valueOf(DEFAULT_TRIGGER_PERCENT));

		defaultPrefs(prefs, AgentPreferences.START_ONLY_WHEN_SCREEN_OFF, String.valueOf(false));


        // disable mobile data switch by force in Lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            prefs.put(AgentPreferences.BATTERY_MOBILE_DATA, String.valueOf(false));
        }

		return prefs;
	}

	@Override
	public AgentUIElement[] getSettings(AgentConfigurationProvider acp) {
		HashMap<String,String> agentPreferencesMap = getPreferencesMap();

        int numSettings = 15;

        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)) {
            numSettings += 1;
        }

		AgentUIElement[] settings = new AgentUIElement[numSettings];

        HashedNumberDispenser position = new HashedNumberDispenser();

		settings[position.generate(AgentPreferences.BATTERY_PERCENT_TRIGGER)] = new AgentIntSliderSetting(acp, R.string.battery_agent_percent_trigger, Integer.parseInt(agentPreferencesMap.get(AgentPreferences.BATTERY_PERCENT_TRIGGER)), 100, AgentPreferences.BATTERY_PERCENT_TRIGGER, acp.getActivity().getResources().getString(R.string.config_percentage));
		settings[position.generate(null)] = new AgentLabelSetting(acp, R.string.battery_agent_trigger_explanation);

		settings[position.generate(null)] = null;

		settings[position.generate(AgentPreferences.BATTERY_DISABLE_WHEN_CHARGING)] = new AgentCheckboxSetting(acp, R.string.battery_agent_disable_when_charging, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.BATTERY_DISABLE_WHEN_CHARGING)), true, AgentPreferences.BATTERY_DISABLE_WHEN_CHARGING);
        settings[position.generate(AgentPreferences.START_ONLY_WHEN_SCREEN_OFF)] = new AgentCheckboxSetting(acp, R.string.battery_agent_no_start_screen_on, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.START_ONLY_WHEN_SCREEN_OFF)), true, AgentPreferences.START_ONLY_WHEN_SCREEN_OFF);
        settings[position.generate(AgentPreferences.WIFI_HOME_NAME)] = new AgentWifiSetting(acp, R.string.battery_agent_no_start_wifi, AgentPreferences.WIFI_HOME_NAME, agentPreferencesMap.get(AgentPreferences.WIFI_HOME_NAME));
        settings[position.generate("BATTERY_WIFI_HOME_NAME_LABEL")] = new AgentLabelSetting(acp, R.string.battery_agent_wifi_explanation);

        settings[position.generate(null)] = new AgentSpacerSetting(acp);

		settings[position.generate(AgentPreferences.BATTERY_SYNC)] = new AgentCheckboxSetting(acp, R.string.config_turn_off_syncing, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.BATTERY_SYNC)), true, AgentPreferences.BATTERY_SYNC);
		settings[position.generate(AgentPreferences.BATTERY_BT)] = new AgentCheckboxSetting(acp, R.string.config_turn_off_bt, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.BATTERY_BT)), true, AgentPreferences.BATTERY_BT);
		settings[position.generate(AgentPreferences.BATTERY_WIFI)] = new AgentCheckboxSetting(acp, R.string.config_turn_off_wifi, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.BATTERY_WIFI)), true, AgentPreferences.BATTERY_WIFI);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            settings[position.generate(AgentPreferences.BATTERY_MOBILE_DATA)] = new AgentCheckboxSetting(acp, R.string.config_turn_off_mobile_data, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.BATTERY_MOBILE_DATA)), true, AgentPreferences.BATTERY_MOBILE_DATA);
        }

		settings[position.generate(null)] = new AgentSpacerSetting(acp);

		settings[position.generate(AgentPreferences.BATTERY_DISPLAY)] = new AgentCheckboxSetting(acp, R.string.config_dim_display, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.BATTERY_DISPLAY)), true, AgentPreferences.BATTERY_DISPLAY);
		settings[position.generate(AgentPreferences.BATTERY_BRIGHTNESS_LEVEL)] = new AgentIntSliderSetting(acp, R.string.config_dim_display_level, Integer.parseInt(agentPreferencesMap.get(AgentPreferences.BATTERY_BRIGHTNESS_LEVEL)), 100, AgentPreferences.BATTERY_BRIGHTNESS_LEVEL, acp.getActivity().getResources().getString(R.string.config_percentage));
		settings[position.generate("BATTERY_BRIGHTNESS_LEVEL_LABEL")] = new AgentLabelSetting(acp, R.string.battery_agent_brightness_level_explanation);

        position.lock();

        OrChildCheck dim_cascade = new OrChildCheck();
        dim_cascade.addConsequence(settings[position.fetch(AgentPreferences.BATTERY_BRIGHTNESS_LEVEL)]);
        dim_cascade.addConsequence(settings[position.fetch("BATTERY_BRIGHTNESS_LEVEL_LABEL")]);
        dim_cascade.addConditional(settings[position.fetch(AgentPreferences.BATTERY_DISPLAY)]);

		return settings;
	}


	@Override
	public HashMap<String, String> updatePreference(String prefName, String prefVal) {
		HashMap<String, String> prefs = super.updatePreference(prefName, prefVal);

		if (prefName.equals(AgentPreferences.BATTERY_PERCENT_TRIGGER) || prefName.equals(AgentPreferences.BATTERY_DISABLE_WHEN_CHARGING)) {
            BatteryAgent.checkActive(mContext);
			return prefs;
		}

		return prefs;
	}

	public boolean havePreconditionsBeenMet() {
		if (isActive()) {return false;}  // optimization

        if (Utils.isOnPhoneCall(mContext)) {
            Logger.d("On phone call. Not activating BA.");
            return false;
        }

		HashMap<String, String> prefsMap = getPreferencesMap();

        String currentSsid = Utils.getCurrentSsid(mContext);
        if ((currentSsid != null) && !currentSsid.isEmpty()) {
            for (WifiWrapper ww:AgentWifiSetting.parseNetworkSsidNames(prefsMap.get(AgentPreferences.WIFI_HOME_NAME))) {
                if (currentSsid.equals(ww.ssid)) {
                    Logger.d("Connected to ssid: " + currentSsid + ".  Not activating BA");
                    return false;
                }
            }
        }

		boolean onlyWhenScreenOff = Boolean.parseBoolean(prefsMap.get(AgentPreferences.START_ONLY_WHEN_SCREEN_OFF));		
		return ((!onlyWhenScreenOff) || (!Utils.isScreenOn(mContext)));
	}




    private void buildFinishedNotificationMessage(Context context, int triggerType, long ranForMillis) {
        mBatteryFinishNotificationMessage = String.format(mContext.getResources().getString(R.string.agent_deactivated), getName());

        if (triggerType == Constants.TRIGGER_TYPE_BOOT) {
            return;
        }

        double factor = 0.20 + 0.33 * (new Random()).nextDouble();
        int minutesExtended = (int) (factor * ranForMillis / 60000.0);
        while (minutesExtended > 600) {minutesExtended *= 0.75;}

        if (minutesExtended > 1) {
            mBatteryFinishNotificationMessage = String.format(context.getResources().getString(R.string.agent_finished_battery_agent_percent), minutesExtended);
        }
    }


	@Override
	public String getEnabledStatusMessage() {
		return mContext.getResources().getString(R.string.agent_status_bar_enabled_battery_agent);
	}


	@Override
	public String getStartedStatusMessage() {
        if (mTriggeredBy == Constants.TRIGGER_TYPE_MANUAL) {
            return mContext.getResources().getString(R.string.agent_status_bar_started_manual);
        }
		return mContext.getResources().getString(R.string.agent_status_bar_started_battery_agent);
	}

    @Override
    public String getFinishedLogMessage(int triggerType, long ranForMillis) {
        if (mBatteryFinishNotificationMessage == null) {
            buildFinishedNotificationMessage(mContext, triggerType, ranForMillis);
        }
        return mBatteryFinishNotificationMessage;
    }


    @Override
    public String[] getNotifActionLines(Context context, int triggerType, boolean unpause) {
        ArrayList<String> actions = new ArrayList<String>();

        HashMap<String, String> prefs = getPreferencesMap();

        boolean bluetooth = Boolean.parseBoolean(prefs.get(AgentPreferences.BATTERY_BT));
        boolean wifi = Boolean.parseBoolean(prefs.get(AgentPreferences.BATTERY_WIFI));
        boolean mobileData = Boolean.parseBoolean(prefs.get(AgentPreferences.BATTERY_MOBILE_DATA));
        boolean display = Boolean.parseBoolean(prefs.get(AgentPreferences.BATTERY_DISPLAY));
        boolean sync = Boolean.parseBoolean(prefs.get(AgentPreferences.BATTERY_SYNC));

        if(bluetooth) actions.add(context.getResources().getString(R.string.battery_agent_bt_off));
        if(display) actions.add(context.getResources().getString(R.string.battery_agent_brightness_down));
        if(sync) actions.add(context.getResources().getString(R.string.battery_agent_autosync_off));
        if(wifi) actions.add(context.getResources().getString(R.string.battery_agent_wifi_off));
        if(mobileData) actions.add(context.getResources().getString(R.string.battery_agent_mobile_data_off));


        return actions.toArray(new String[actions.size()]);
    }

	@Override
	public String getNotifStartTitle(int triggerType, boolean unpause) {
		int title_res;
		if(Constants.TRIGGER_TYPE_MANUAL == triggerType) {
			title_res = R.string.agent_started_title_battery_manual;
		} else {
			title_res = R.string.agent_started_title_battery;
		}

		return mContext.getResources().getString(title_res);
	}

    @Override
    public String getNotifStartMessage(int triggerType, boolean unpause) {
        return String.format(mContext.getResources().getString(R.string.agent_activated), getName());
    }


    @Override
    public String getNotifFirstStartTitle(int triggerType, boolean unpause) {
        return getNotifStartTitle(triggerType, unpause);
    }

    @Override
    public String getNotifFirstStartMessage(int triggerType, boolean unpause) {

        int res = R.string.first_notification_battery;

        return mContext.getResources().getString(res);
    }

    @Override
    public String getNotifFirstStartDialogDescription() {
        return mContext.getResources().getString(R.string.battery_agent_description_first);
    }

    @Override
    public String getNotifPauseActionString(int triggerType) {

        int title_res;
        if(Constants.TRIGGER_TYPE_MANUAL == triggerType) {
            title_res = R.string.agent_action_pause_manual;
        } else {
            title_res = R.string.agent_action_pause_battery;
        }

        return mContext.getResources().getString(title_res);
    }

    private static final int  CHECKER_REQ_ID = 20001;
    private static final long INTERVAL_CLOSE_TO_THRESHOLD = 5 * 60 * 1000;
    private static final long INTERVAL_FAR_FROM_THRESHOLD = 15 * 60 * 1000;

    public static void resetAlarms(Context context) {
        resetAlarms(context, INTERVAL_CLOSE_TO_THRESHOLD);
    }
    public static void cancelAlarms(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(getCheckerPendingIntent(context));
    }

    private static void resetAlarms(Context context, long intervalMillis) {
        cancelAlarms(context);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + intervalMillis, intervalMillis, getCheckerPendingIntent(context));
    }

    private static PendingIntent getCheckerPendingIntent(Context context) {
        Intent intent = new Intent(context, BatteryReceiver.class);
        return PendingIntent.getBroadcast(context, CHECKER_REQ_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static void checkActive(Context context) {
        int level = -1;
        double scale = -1;

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryIntent = context.getApplicationContext().registerReceiver(null,filter);
        if (batteryIntent == null) {
            Logger.d("BATTERY: Null action battery changed intent; doing nothing.");
            resetAlarms(context, INTERVAL_CLOSE_TO_THRESHOLD);
            return;
        }

        level = batteryIntent.getIntExtra("level", 0);
        scale = batteryIntent.getIntExtra("scale", -1);


        float scaledLevel = level;
        if (scale > 0) {
            scaledLevel = (level / (float) scale) * 100;
        }

        if (scaledLevel <= 0) {
            Logger.d("BATTERY: scaledLevel <= 0");
            resetAlarms(context, INTERVAL_CLOSE_TO_THRESHOLD);
            return;
        }

        boolean isPluggedIn = Utils.isPhonePluggedIn(batteryIntent);

        String prevLevelStr = PrefsHelper.getPrefString(context, PREF_PREV_BATTERY_LEVEL, "");
        String curLevelStr = String.valueOf(scaledLevel) + "|" + String.valueOf(isPluggedIn);
        PrefsHelper.setPrefString(context, PREF_PREV_BATTERY_LEVEL, curLevelStr);

        if (prevLevelStr.equals(curLevelStr)) {
            Logger.d("BATTERY: prevLevel = curLevel = " + curLevelStr);
            resetAlarms(context, INTERVAL_FAR_FROM_THRESHOLD);
            return;
        }

        BatteryAgent ba = (BatteryAgent) AgentFactory.getAgentFromGuid(context, HARDCODED_GUID);
        if (!ba.isInstalled()) {
            Logger.d("BATTERY: BatteryAgent not installed!");
            return;
        }

        HashMap<String, String> prefs = ba.getPreferencesMap();
        int levelToFire = Integer.parseInt(prefs.get(AgentPreferences.BATTERY_PERCENT_TRIGGER));
        boolean disableWhenCharging = Boolean.parseBoolean(prefs.get(AgentPreferences.BATTERY_DISABLE_WHEN_CHARGING));

        Logger.d("BATTERY: levelToFire = " + levelToFire + "; disableWhenCharging: " + disableWhenCharging + " vs. curScaledLevel =" + scaledLevel);

        if (disableWhenCharging && isPluggedIn) {
            setInactive(context, BatteryAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_BATTERY);
            resetAlarms(context, INTERVAL_FAR_FROM_THRESHOLD);
            return;
        }

        if (scaledLevel <= levelToFire) {
            setActive(context, BatteryAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_BATTERY);
        } else {
            setInactive(context, BatteryAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_BATTERY);
        }

        resetAlarms(context, (Math.abs(scaledLevel - levelToFire) >= 10) ? INTERVAL_FAR_FROM_THRESHOLD : INTERVAL_CLOSE_TO_THRESHOLD);
    }

}
