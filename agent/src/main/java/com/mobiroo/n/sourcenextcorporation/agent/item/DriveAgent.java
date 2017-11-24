package com.mobiroo.n.sourcenextcorporation.agent.item;

import android.app.AlarmManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentBluetoothSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentCheckboxSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentContactsSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentLabelSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentPermission;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentSpacerSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentTextLineSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentWarningSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.ChildCheck;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.OrChildCheck;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentNotificationInterface;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.HashedNumberDispenser;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentRadioBooleanSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentUIElement;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.AndChildCheck;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.DriveAgentActivityReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.util.ActivityRecognitionHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;

import java.util.ArrayList;
import java.util.HashMap;


public class DriveAgent extends StaticAgent implements ActivityDetectorInterface, AgentNotificationInterface {
	public static final String HARDCODED_GUID = "tryagent.drive";

	public static final String  ACTIVITY_DETECTION_TAG = "DriveAgent";
	public static final long ACTIVITY_DETECTION_INTERVAL = ActivityRecognitionHelper.INTERVAL_2_MINUTES;


	@Override
	public int getNameId() {
		return R.string.drive_agent_title;
	}

	@Override
	public int getDescriptionId() {
		return R.string.drive_agent_description;
	}




	@Override
	public int getLongDescriptionId() {
		return R.string.drive_agent_description_long;
	}


	@Override
	public AgentPermission[] getTriggerArray() {
		AgentPermission[] agentPermissions = new AgentPermission[2];
		agentPermissions[0] = new AgentPermission(R.drawable.ic_bt, R.string.drive_agent_trigger_bt);
		agentPermissions[1] = new AgentPermission(R.drawable.ic_sms, R.string.drive_agent_trigger_sms);
		return agentPermissions;
	}


	@Override
	public int getIconId() {
		return R.drawable.ic_drive_agent;
	}

	@Override
	public int getWhiteIconId() {
		return R.drawable.ic_stat_drive_agent;
	}


	@Override
	public int getColorIconId() {
		return R.drawable.ic_drive_agent_color;
	}


	@Override
	public int getWidgetOutlineIconId() {
		return R.drawable.ic_widget_drive_inactive;
	}

	@Override
	public int getWidgetFillIconId() {
		return R.drawable.ic_widget_drive_active;
	}


	@Override
	public void afterInstall(Context context, boolean silent, boolean skipCheckReceivers) {
		SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();

		TaskDatabaseHelper.createAction(db, getGuid(), "PhoneSilenceAction", Constants.TRIGGER_TYPE_BLUETOOTH);
		TaskDatabaseHelper.createAction(db, getGuid(), "PhoneSilenceAction", Constants.TRIGGER_TYPE_DRIVING);
		TaskDatabaseHelper.createAction(db, getGuid(), "PhoneSilenceAction", Constants.TRIGGER_TYPE_MANUAL);


		TaskDatabaseHelper.createAction(db, getGuid(), "AutorespondSmsAction", Constants.TRIGGER_TYPE_SMS);
		TaskDatabaseHelper.createAction(db, getGuid(), "AutorespondPhoneCallAction", Constants.TRIGGER_TYPE_MISSED_CALL);
		TaskDatabaseHelper.createAction(db, getGuid(), "ReadTextAction", Constants.TRIGGER_TYPE_SMS);

		TaskDatabaseHelper.createTrigger(db, getGuid(), Constants.TRIGGER_TYPE_SMS);
		TaskDatabaseHelper.createTrigger(db, getGuid(), Constants.TRIGGER_TYPE_MISSED_CALL);

        if (! skipCheckReceivers) {
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

        SleepAgent sa = (SleepAgent) AgentFactory.getAgentFromGuid(mContext, SleepAgent.HARDCODED_GUID);
        if (sa.isActive()) {
            Logger.d("DriveAgent: Pausing sleep agent; cannot be driving and sleeping at same time.");
            sa.pause(mContext);
        }
    }

	@Override
	public HashMap<String, String> getPreferencesMap() {
		HashMap<String, String> prefs = super.getPreferencesMap();

		defaultPrefs(prefs, AgentPreferences.BLUETOOTH_NAME_TRIGGER, "");

		defaultPrefs(prefs, AgentPreferences.SILENCE_PHONE, String.valueOf(false));
		defaultPrefs(prefs, AgentPreferences.SOUND_SILENCE_DEVICE, String.valueOf(true));

		defaultPrefs(prefs, AgentPreferences.SMS_AUTORESPOND, String.valueOf(true));
        defaultPrefs(prefs, AgentPreferences.NOTIFICATIONS_READ_ALOUD_VOICE_RESPONSE, String.valueOf(false));

		defaultPrefs(prefs, AgentPreferences.PHONE_CALL_AUTORESPOND, String.valueOf(true));
		defaultPrefs(prefs, AgentPreferences.SMS_AUTORESPOND_CONTACTS, AgentPreferences.SMS_AUTORESPOND_CONTACT_EVERYONE);
        defaultPrefs(prefs, AgentPreferences.SMS_READ_USING_SPEAKERPHONE, String.valueOf(false));

		defaultPrefs(prefs, AgentPreferences.USE_ACTIVITY_DETECTION, String.valueOf(true));

		// drive agent SMS auto-respond is never in urgent mode but always respond once
		// drive agent also does autorespond mode, not wake mode
		prefs.put(AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT, String.valueOf(false));
		prefs.put(AgentPreferences.SMS_AUTORESPOND_ONCE, String.valueOf(true));
		prefs.put(AgentPreferences.PHONE_CALL_AUTORESPOND_MODE, AgentPreferences.AUTORESPOND_MODE_RESPOND);
		prefs.put(AgentPreferences.SMS_AUTORESPOND_MODE, AgentPreferences.AUTORESPOND_MODE_RESPOND);
		prefs.put(AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL, String.valueOf(false));

		String smsAutoRespondMsg = mContext.getResources().getString(R.string.drive_agent_default_autorespond);
		defaultPrefs(prefs, AgentPreferences.SMS_AUTORESPOND_MESSAGE, smsAutoRespondMsg);

		defaultPrefs(prefs, AgentPreferences.SMS_READ_ALOUD, String.valueOf(true));
		defaultPrefs(prefs, AgentPreferences.SMS_READ_ALOUD_VOICE_RESPONSE, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.SMS_RESPOND_WITH_HEADSET, String.valueOf(false));

		return prefs;
	}

	@Override
	public AgentUIElement[] getSettings(AgentConfigurationProvider acp) {
		HashMap<String, String> agentPreferencesMap = getPreferencesMap();
		AgentUIElement[] settings = new AgentUIElement[(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) ? 21 : 20];
        HashedNumberDispenser position = new HashedNumberDispenser();

		settings[position.generate(AgentPreferences.BLUETOOTH_NAME_TRIGGER)] = new AgentBluetoothSetting(acp, R.string.config_drive_bt_network, AgentPreferences.BLUETOOTH_NAME_TRIGGER, agentPreferencesMap.get(AgentPreferences.BLUETOOTH_NAME_TRIGGER));
		settings[position.generate(AgentPreferences.USE_ACTIVITY_DETECTION)] = new AgentCheckboxSetting(acp, R.string.config_use_activity_detection, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.USE_ACTIVITY_DETECTION)), true, AgentPreferences.USE_ACTIVITY_DETECTION);
        settings[position.generate("ACTIVITY_BLUETOOTH_CONFLICT_WARNING")] = new AgentWarningSetting(acp, R.string.exclusion_drive_agent_ad_bt);

		settings[position.generate(null)] = null;

		settings[position.generate(AgentPreferences.SILENCE_PHONE)] = new AgentCheckboxSetting(acp, R.string.config_silence_phone, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SILENCE_PHONE)), true, AgentPreferences.SILENCE_PHONE);
		settings[position.generate(AgentPreferences.SOUND_SILENCE_DEVICE)] = new AgentRadioBooleanSetting(acp, R.string.config_silence_volume, R.string.config_silence_on, R.string.config_silence_off,  true, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SOUND_SILENCE_DEVICE)), AgentPreferences.SOUND_SILENCE_DEVICE);

		settings[position.generate(null)] = new AgentSpacerSetting(acp);

		settings[position.generate(AgentPreferences.SMS_READ_ALOUD)] = new AgentCheckboxSetting(acp, R.string.config_read_sms_aloud, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SMS_READ_ALOUD)), true, AgentPreferences.SMS_READ_ALOUD);
        settings[position.generate(AgentPreferences.NOTIFICATIONS_READ_ALOUD_VOICE_RESPONSE)] = new AgentCheckboxSetting(acp, R.string.config_read_other_messages, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.NOTIFICATIONS_READ_ALOUD_VOICE_RESPONSE)), false, AgentPreferences.NOTIFICATIONS_READ_ALOUD_VOICE_RESPONSE);
		settings[position.generate(AgentPreferences.SMS_READ_ALOUD_VOICE_RESPONSE)] = new AgentCheckboxSetting(acp, R.string.config_read_sms_aloud_voice_response, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SMS_READ_ALOUD_VOICE_RESPONSE)), true, AgentPreferences.SMS_READ_ALOUD_VOICE_RESPONSE);
        settings[position.generate(AgentPreferences.SMS_READ_USING_SPEAKERPHONE)] = new AgentCheckboxSetting(acp, R.string.config_read_sms_using_speaker, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SMS_READ_USING_SPEAKERPHONE)), true, AgentPreferences.SMS_READ_USING_SPEAKERPHONE);
        //settings[position.generate(AgentPreferences.SMS_RESPOND_WITH_HEADSET)] = new AgentCheckboxSetting(acp, R.string.config_use_bluetooth_headset, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SMS_RESPOND_WITH_HEADSET)), false, AgentPreferences.SMS_RESPOND_WITH_HEADSET);
        settings[position.generate("READING_SMS_WARNING_1")] = new AgentWarningSetting(acp, R.string.exclusion_respond_read);
		settings[position.generate("VOICE_RESPONSE_LABEL")] = new AgentLabelSetting(acp, R.string.config_read_sms_aloud_voice_desc);

		settings[position.generate(null)] = new AgentSpacerSetting(acp);

		settings[position.generate(AgentPreferences.SMS_AUTORESPOND_CONTACTS)] = new AgentContactsSetting(acp, R.string.contacts_allowed_drive, AgentPreferences.SMS_AUTORESPOND_CONTACTS, agentPreferencesMap.get(AgentPreferences.SMS_AUTORESPOND_CONTACTS));
		settings[position.generate(AgentPreferences.PHONE_CALL_AUTORESPOND)] = new AgentCheckboxSetting(acp, R.string.config_auto_respond_phone, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.PHONE_CALL_AUTORESPOND)), true, AgentPreferences.PHONE_CALL_AUTORESPOND);
		settings[position.generate(AgentPreferences.SMS_AUTORESPOND)] = new AgentCheckboxSetting(acp, R.string.config_auto_respond_text, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SMS_AUTORESPOND)), true, AgentPreferences.SMS_AUTORESPOND);
        settings[position.generate("READING_SMS_WARNING_2")] = new AgentWarningSetting(acp, R.string.exclusion_respond_sms);
		settings[position.generate(AgentPreferences.SMS_AUTORESPOND_MESSAGE)] = new AgentTextLineSetting(acp, R.string.auto_response,AgentPreferences.SMS_AUTORESPOND_MESSAGE, agentPreferencesMap.get(AgentPreferences.SMS_AUTORESPOND_MESSAGE));

        position.lock();

        // setup dependencies

        // add cascades
        ((AgentBluetoothSetting) settings[position.fetch(AgentPreferences.BLUETOOTH_NAME_TRIGGER)]).addCascade(acp.getActivity().getResources().getString(R.string.agent_propogate_bluetooth_parking), ParkingAgent.HARDCODED_GUID, AgentPreferences.BLUETOOTH_NAME_TRIGGER);

        // reading sms and voice response are dependent on reading sms aloud
        OrChildCheck read_voice_cascade = new OrChildCheck();
        read_voice_cascade.addConsequence(settings[position.fetch(AgentPreferences.SMS_READ_ALOUD_VOICE_RESPONSE)]);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			read_voice_cascade.addConsequence(settings[position.fetch(AgentPreferences.NOTIFICATIONS_READ_ALOUD_VOICE_RESPONSE)]);
		}
        read_voice_cascade.addConsequence(settings[position.fetch("VOICE_RESPONSE_LABEL")]);
        read_voice_cascade.addConsequence(settings[position.fetch(AgentPreferences.SMS_READ_USING_SPEAKERPHONE)]);
        read_voice_cascade.addConditional(settings[position.fetch(AgentPreferences.SMS_READ_ALOUD)]);

        // phone silencing options dependent on silencing phone
        OrChildCheck phone_sound_cascade = new OrChildCheck();
        phone_sound_cascade.addConsequence(settings[position.fetch(AgentPreferences.SOUND_SILENCE_DEVICE)]);
        phone_sound_cascade.addConditional(settings[position.fetch(AgentPreferences.SILENCE_PHONE)]);

        // dependencies for message sending
		ChildCheck responseCheck = new OrChildCheck();
        responseCheck.addConsequence(settings[position.fetch(AgentPreferences.SMS_AUTORESPOND_MESSAGE)]);
        responseCheck.addConsequence(settings[position.fetch(AgentPreferences.SMS_AUTORESPOND_CONTACTS)]);
        responseCheck.addConditional(settings[position.fetch(AgentPreferences.SMS_AUTORESPOND)]);
        responseCheck.addConditional(settings[position.fetch(AgentPreferences.PHONE_CALL_AUTORESPOND)]);

        // dependencies for warnings on speak / response
        ChildCheck autorespondWarningCheck = new AndChildCheck();
        autorespondWarningCheck.addConsequence(settings[position.fetch("READING_SMS_WARNING_1")]);
        autorespondWarningCheck.addConsequence(settings[position.fetch("READING_SMS_WARNING_2")]);
        autorespondWarningCheck.addConditional(settings[position.fetch(AgentPreferences.SMS_READ_ALOUD_VOICE_RESPONSE)]);
        autorespondWarningCheck.addConditional(settings[position.fetch(AgentPreferences.SMS_AUTORESPOND)]);

        // dependencies for warning on bt / ad selection
        ChildCheck bluetoothActivityWarningCheck = new AndChildCheck();
        bluetoothActivityWarningCheck.addConsequence(settings[position.fetch("ACTIVITY_BLUETOOTH_CONFLICT_WARNING")]);
        bluetoothActivityWarningCheck.addConditional(settings[position.fetch(AgentPreferences.USE_ACTIVITY_DETECTION)]);
        bluetoothActivityWarningCheck.addConditional(settings[position.fetch(AgentPreferences.BLUETOOTH_NAME_TRIGGER)]);

        return settings;
	}


	@Override
	public String getEnabledStatusMessage() {
		return mContext.getResources().getString(R.string.agent_status_bar_enabled_drive_agent);
	}


	@Override
	public String getStartedStatusMessage() {
        if (mTriggeredBy == Constants.TRIGGER_TYPE_MANUAL) {
            return mContext.getResources().getString(R.string.agent_status_bar_started_manual);
        }
        return mContext.getResources().getString(R.string.agent_status_bar_started_drive_agent);
	}


    @Override
    public String[] getNotifActionLines(Context context, int triggerType, boolean unpause) {
        ArrayList<String> actions = new ArrayList<String>();

        HashMap<String, String> prefs = getPreferencesMap();

        boolean silence = Boolean.parseBoolean(prefs.get(AgentPreferences.SILENCE_PHONE));
        boolean text = Boolean.parseBoolean(prefs.get(AgentPreferences.SMS_AUTORESPOND));
        boolean reading = Boolean.parseBoolean(prefs.get(AgentPreferences.SMS_READ_ALOUD));

        if(text) actions.add(context.getResources().getString(R.string.drive_agent_text));
        if(silence) actions.add(context.getResources().getString(R.string.drive_agent_silence));
        if(reading) actions.add(context.getResources().getString(R.string.drive_agent_reading));

        return actions.toArray(new String[actions.size()]);
    }

	@Override
	public String getNotifStartTitle(int triggerType, boolean unpause) {
		int title_res;

		if(Constants.TRIGGER_TYPE_MANUAL == triggerType) {
			title_res = R.string.agent_started_title_drive_manual;
		} else {
			title_res = R.string.agent_started_title_drive;
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

		int res;
		if(Constants.TRIGGER_TYPE_DRIVING == triggerType) {
			res = R.string.first_notification_drive_ad;
		} else {
			res = R.string.first_notification_drive_bt;
		}

		return mContext.getResources().getString(res);

	}

    @Override
    public String getNotifFirstStartDialogDescription() {
        return mContext.getResources().getString(R.string.drive_agent_description_first);
    }

	@Override
	public String getNotifPauseActionString(int triggerType) {

		int title_res;
		if(Constants.TRIGGER_TYPE_MANUAL == triggerType) {
			title_res = R.string.agent_action_pause_manual;
		} else {
			title_res = R.string.agent_action_pause_drive;
		}

		return mContext.getResources().getString(title_res);
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
		return DriveAgentActivityReceiver.class;
	}




	// setting minPausedTime=maxPausedTime makes sure pause takes exactly this long.
	@Override
	public long getMaxPausedTime() {
		return AlarmManager.INTERVAL_HOUR;
	}
	@Override
	public long getMinPausedTime() {
		return getMaxPausedTime();
	}

}
