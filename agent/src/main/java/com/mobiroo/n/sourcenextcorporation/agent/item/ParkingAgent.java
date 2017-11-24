package com.mobiroo.n.sourcenextcorporation.agent.item;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.activity.ParkingAgentConfigurationActivity;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentBluetoothSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentCheckboxSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentPermission;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentUIElement;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentWarningSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.AndChildCheck;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.ChildCheck;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.ParkingAgentActivityReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.service.LocationSaverService;
import com.mobiroo.n.sourcenextcorporation.agent.util.ActivityRecognitionHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.HashedNumberDispenser;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.widget.AgentToggleWidgetProvider;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class ParkingAgent extends StaticAgent implements ActivityDetectorInterface, LocationSaverInterface {
	public static final String HARDCODED_GUID = "tryagent.parking";

	private final static String LOCATIONS_PREF_KEY = "KeyParkingLocations";

    public static final String PREF_IS_DRIVING = "ParkingAgentIsDriving";

	public static final String  ACTIVITY_DETECTION_TAG = "ParkingAgent";
	public static final long ACTIVITY_DETECTION_INTERVAL = ActivityRecognitionHelper.INTERVAL_2_MINUTES;

	private final static int MAX_PARKING_LOCS_SAVED = 50;
	private final static long ANALYTICS_PARKING_INTERVAL = 12 * AlarmManager.INTERVAL_HOUR;

	@Override
	public int getNameId() {
		return R.string.parking_agent_title;
	}

	@Override
	public int getDescriptionId() {
		return R.string.parking_agent_description;
	}

	@Override
	public int getLongDescriptionId() {
		return R.string.parking_agent_description_long;
	}

	public Class<?> getConfigActivity() {
		return ParkingAgentConfigurationActivity.class;
	}




	@Override
	public AgentPermission[] getTriggerArray() {
		AgentPermission[] agentPermissions = new AgentPermission[3];
		agentPermissions[0] = new AgentPermission(R.drawable.ic_drive_agent, R.string.parking_agent_trigger_activity);
		agentPermissions[1] = new AgentPermission(R.drawable.ic_bt, R.string.parking_agent_trigger_bt);
		agentPermissions[2] = new AgentPermission(R.drawable.ic_parking_agent, R.string.parking_agent_trigger_pin);
		return agentPermissions;
	}


	@Override
	public int getIconId() {
		return R.drawable.ic_parking_agent;
	}

	@Override
	public int getWhiteIconId() {
		return R.drawable.ic_parking_agent_white;
	}


	@Override
	public int getColorIconId() {
		return R.drawable.ic_parking_agent_color;
	}

	@Override
	public AgentUIElement[] getSettings(AgentConfigurationProvider acp) {
		HashMap<String, String> agentPreferencesMap = getPreferencesMap();
		AgentUIElement[] settings = new AgentUIElement[3];
        HashedNumberDispenser position = new HashedNumberDispenser();

        settings[position.generate(AgentPreferences.USE_ACTIVITY_DETECTION)] = new AgentCheckboxSetting(acp, R.string.config_use_activity_detection, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.USE_ACTIVITY_DETECTION)), true, AgentPreferences.USE_ACTIVITY_DETECTION);
		settings[position.generate(AgentPreferences.BLUETOOTH_NAME_TRIGGER)] = new AgentBluetoothSetting(acp, R.string.config_drive_bt_network, AgentPreferences.BLUETOOTH_NAME_TRIGGER, agentPreferencesMap.get(AgentPreferences.BLUETOOTH_NAME_TRIGGER));
		settings[position.generate("ACTIVITY_DETECTION_AND_BLUETOOTH_WARNING")] = new AgentWarningSetting(acp, R.string.parking_agent_bt_desc);

        position.lock();

        // add cascades
        ((AgentBluetoothSetting) settings[position.fetch(AgentPreferences.BLUETOOTH_NAME_TRIGGER)]).addCascade(acp.getActivity().getResources().getString(R.string.agent_propogate_bluetooth_driving), DriveAgent.HARDCODED_GUID, AgentPreferences.BLUETOOTH_NAME_TRIGGER);

        // add warnings
        ChildCheck warningCheck = new AndChildCheck();
        warningCheck.addConsequence(settings[position.fetch("ACTIVITY_DETECTION_AND_BLUETOOTH_WARNING")]);
        warningCheck.addConditional(settings[position.fetch(AgentPreferences.BLUETOOTH_NAME_TRIGGER)]);
        warningCheck.addConditional(settings[position.fetch(AgentPreferences.USE_ACTIVITY_DETECTION)]);

        return settings;
	}

	@Override
	public HashMap<String, String> getPreferencesMap() {
		HashMap<String, String> prefs = super.getPreferencesMap();

        defaultPrefs(prefs, AgentPreferences.USE_ACTIVITY_DETECTION, Boolean.toString(true));

        defaultPrefs(prefs, AgentPreferences.BLUETOOTH_NAME_TRIGGER, "");
		return prefs;
	}


	public ArrayList<ParkingInfo> getParkingLocations(Context context) {
		ArrayList<ParkingInfo> spots = new ArrayList<ParkingInfo>();
		ParkingAgent parkingAgent = (ParkingAgent) AgentFactory.getAgentFromGuid(context, HARDCODED_GUID);
		SharedPreferences prefs = parkingAgent.getSession();

		String json = prefs.getString(LOCATIONS_PREF_KEY, "");
		if (json.isEmpty()) { return spots;}

		JSONTokener tokener = new JSONTokener(json);
		JSONArray list = null;
		try {
			list = new JSONArray(tokener);
		} catch (JSONException e) {
			Logger.e("Couldn't load parking list", e);
		}
		if (list == null) {return spots;}

		try {
			JSONArray json_spots = list;
			for (int i = 0; i < json_spots.length(); i++) {
				try {
					JSONObject object = json_spots.getJSONObject(i);
					ParkingInfo info = new ParkingInfo(object.getDouble(ParkingInfo.KEY_LATITUDE), object.getDouble(ParkingInfo.KEY_LONGITUDE), 
							object.getDouble(ParkingInfo.KEY_ACCURACY), object.getLong(ParkingInfo.KEY_TIME), 
							object.optInt(ParkingInfo.KEY_TRIGGER_TYPE, Constants.TRIGGER_TYPE_PARKING));
					spots.add(info);
				} catch (Exception e) {
					Logger.e("Exception parsing list object", e);
				}
			}
		} catch (Exception e) {
			Logger.e("Couldn't get spots", e);
		}

		Logger.d("ParkingAgent: Got " + spots.size() + " parking spots.");

		Collections.sort(spots, new ParkingInfoComparator());

		return spots;
	}

	public class ParkingInfoComparator implements Comparator<ParkingInfo> {
		@Override
		public int compare(ParkingInfo o1, ParkingInfo o2) {
			return Long.valueOf(o1.getTime()).compareTo(Long.valueOf(o2.getTime()));
		}
	}

	public static class ParkingInfo {
		private double mLatitude;
		private double mLongitude;
		private double mAccuracy;
		private long mTime;
		private int mTriggerType;

		protected static final String KEY_TIME = "time";
		protected static final String KEY_LATITUDE = "latitude";
		protected static final String KEY_LONGITUDE = "longitude";
		protected static final String KEY_ACCURACY = "accuracy";
		protected static final String KEY_TRIGGER_TYPE = "trigger_type";

		public ParkingInfo(double latitude, double longitude, double accuracy, long time, int triggerType) {
			mLatitude = latitude;
			mLongitude = longitude;
			mAccuracy = accuracy;
			mTime = time;
			mTriggerType = triggerType;
		}

		public double getLatitude() {
			return mLatitude;
		}

		public double getLongitude() {
			return mLongitude;
		}

		public double getAccuracy() {
			return mAccuracy;
		}

		public long getTime() {
			return mTime;
		}

		public int getTriggerType() {
			if (mTriggerType == 0) { return Constants.TRIGGER_TYPE_PARKING;}
			return mTriggerType;
		}

		private JSONObject generateJsonObject() {
			JSONObject object = new JSONObject();

			try {
				object.put(KEY_TIME, getTime());
				object.put(KEY_LATITUDE, getLatitude());
				object.put(KEY_LONGITUDE, getLongitude());
				object.put(KEY_ACCURACY, getAccuracy());
				object.put(KEY_TRIGGER_TYPE, getTriggerType());
			} catch (Exception e) {
				Logger.e("Couldn't create JSON object for parking info", e);
			}

			return object;
		}

		protected void save(Context context) {
			Logger.d("ParkingAgent saving spot: " + this.generateJsonObject().toString());

			ParkingAgent parkingAgent = (ParkingAgent) AgentFactory.getAgentFromGuid(context, HARDCODED_GUID);
			ArrayList<ParkingInfo> spots = parkingAgent.getParkingLocations(context);
			JSONArray array = new JSONArray();

			if (!spots.contains(this)) {
				spots.add(0, this);
			}

			for (int i = 0; i < Math.min(MAX_PARKING_LOCS_SAVED, spots.size()); i++) {
				try {
					array.put(spots.get(i).generateJsonObject());
				} catch (Exception e) {

				}
			}

			SharedPreferences prefs = parkingAgent.getSession();
			SharedPreferences.Editor edit = prefs.edit();
			edit.putString(LOCATIONS_PREF_KEY, array.toString());
			edit.commit();
		}

		public static void clearAllSpots(Context context) {
			Logger.d("ParkingAgent: clearing all spots");
			ParkingAgent parkingAgent = (ParkingAgent) AgentFactory.getAgentFromGuid(context, HARDCODED_GUID);
			parkingAgent.getSession().edit().remove(LOCATIONS_PREF_KEY).commit();
		}
	}

	@Override
	public void afterInstall(Context context, boolean silent, boolean skipCheckReceivers) {
        if (!skipCheckReceivers) {
            Utils.checkReceivers(context);
        }
		resetActivityDetection();
	}

	@Override
	public void afterUninstall(Context context, boolean silent) {
		resetActivityDetection();
	}

    @Override
    public void afterActivate(int triggerType, boolean unpause) {
        PrefsHelper.setPrefBool(mContext, PREF_IS_DRIVING, false);
    }
    @Override
    public void afterDeactivate(int triggerType, boolean pause) {
        PrefsHelper.setPrefBool(mContext, PREF_IS_DRIVING, true);
    }



	@Override
	public boolean needsActivityDetection() {
		if (!isInstalled()) {return false;}

		return (Boolean.parseBoolean(getPreferencesMap().get(AgentPreferences.USE_ACTIVITY_DETECTION)) &&
                !AgentBluetoothSetting.isNetworkSpecified(getPreferencesMap().get(AgentPreferences.BLUETOOTH_NAME_TRIGGER)));
	}
	@Override 
	public void resetActivityDetection() {
		if (needsActivityDetection()) {
			ActivityRecognitionHelper.requestActivityRecognition(mContext, ACTIVITY_DETECTION_INTERVAL, ACTIVITY_DETECTION_TAG);
		} else {
			ActivityRecognitionHelper.stopActivityRecognition(mContext, ACTIVITY_DETECTION_TAG);
		}
	}
	@Override
	public Class<?> getActivityReceiverClass() {
		return ParkingAgentActivityReceiver.class;
	}


	@Override
	public String getEnabledStatusMessage() {
		return mContext.getResources().getString(R.string.agent_status_bar_enabled_parking_agent);
	}

	@Override
	public String getStartedLogMessage(int triggerType, boolean unpause) {
		String message = mContext.getResources().getString(R.string.agent_started_parking_agent);
		return message;
	}



	@Override
	public void gotLocation(Location location, int triggerType) {

        boolean isDrving = PrefsHelper.getPrefBool(mContext, ParkingAgent.PREF_IS_DRIVING, false);
		if (isDrving) {
			Logger.d("ParkingAgent: not storing location because driving has restarted.");
			return;
		}

		String triggerName = Utils.getTriggerName(mContext, triggerType);
		Logger.d("ParkingAgent storing location for trigger: " + triggerName);
		/* Store this location */
		ParkingAgent.ParkingInfo info = new ParkingAgent.ParkingInfo(location.getLatitude(), location.getLongitude(), 
				location.getAccuracy(), System.currentTimeMillis(), triggerType);
		info.save(mContext);

		ParkingAgent agent = (ParkingAgent) AgentFactory.getAgentFromGuid(mContext, HARDCODED_GUID);
		String message = mContext.getResources().getString(R.string.agent_started_parking_agent);
		message = String.format(message, agent.getName());
		TaskDatabaseHelper.logUsageRecord(mContext, HARDCODED_GUID, message, null);

		long lastAnalytics = PrefsHelper.getPrefLong(mContext, Constants.PREF_LAST_ANALYTICS_PARKING_STARTED, 0);
		if ((System.currentTimeMillis() - lastAnalytics) > ANALYTICS_PARKING_INTERVAL) {
			PrefsHelper.setPrefLong(mContext, Constants.PREF_LAST_ANALYTICS_PARKING_STARTED, System.currentTimeMillis());
			Usage.logEvent(mContext, Usage.Events.AGENT_STARTED, true,
                    new Usage.EventProperty(Usage.Properties.AGENT_NAME, HARDCODED_GUID),
                    new Usage.EventProperty(Usage.Properties.TRIGGER_NAME, triggerName));
		}
		Usage.logGoogleAnalyticsAgentStarted(mContext, HARDCODED_GUID);

	}

	@Override
	public boolean customActivate(int triggerType) {
		if ((triggerType != Constants.TRIGGER_TYPE_MANUAL) && (triggerType != Constants.TRIGGER_TYPE_PARKING)) {
			return true;
		}

		if (triggerType == Constants.TRIGGER_TYPE_MANUAL) {
            if (PrefsHelper.getPrefBool(mContext, Constants.PREF_SOUND_ON_AGENT_START, false)) {
                Utils.playNotificationSound(mContext);
            }
			AgentToggleWidgetProvider.updateAgentWidgetsDelayed(mContext, mGuid, LocationSaverService.getApproxLocationDelayMs());
		}

		queryLocationForSave(mContext, triggerType);
		return true;
	}

    @Override
    public boolean isStartable() { return false; }

	@Override
	public int getWidgetOutlineIconId() {
		return R.drawable.ic_widget_parking;
	}

	@Override
	public int getWidgetFillIconId() {
		return R.drawable.ic_widget_parking;
	}

	@Override
	public boolean customDeactivate(int triggerType) {
		if (triggerType != Constants.TRIGGER_TYPE_BLUETOOTH) {
			return true;
		}

		queryLocationForSave(mContext, triggerType);
		return true;
	}

	private static void queryLocationForSave(Context context, int triggerType) {
		Intent serviceIntent = new Intent(context, LocationSaverService.class);
		serviceIntent.putExtra(LocationSaverService.EXTRA_AGENT_GUID, HARDCODED_GUID);
		serviceIntent.putExtra(LocationSaverService.EXTRA_TRIGGER_TYPE, triggerType);
		context.startService(serviceIntent);
	}

	@Override
	public boolean needsBackendPause() {
		return false;
	}

}
