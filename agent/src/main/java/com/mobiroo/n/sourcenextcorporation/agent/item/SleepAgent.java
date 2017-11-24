package com.mobiroo.n.sourcenextcorporation.agent.item;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentCheckboxSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentContactsSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentLabelSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentPermission;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentSpacerSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentTextLineSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentTimeRangeSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentUIElement;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.ChildCheck;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.OrChildCheck;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentNotificationInterface;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.DelayableNotificationInterface;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.AlarmReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentTimeRange;
import com.mobiroo.n.sourcenextcorporation.agent.util.HashedNumberDispenser;
import com.mobiroo.n.sourcenextcorporation.agent.util.KeyValue;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentRadioBooleanSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentSpinnerSetting;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.SleepAgentActivityReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.util.ActivityRecognitionHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;


public class SleepAgent extends StaticAgent
        implements ActivityDetectorInterface, AgentNotificationInterface, DelayableNotificationInterface {
    public static final String HARDCODED_GUID = "tryagent.sleep";

    public static final String DEFAULT_WEEKDAY_SLEEP_START_TIME    = "23:00";
    public static final String DEFAULT_WEEKDAY_SLEEP_END_TIME      = "07:00";

    public static final String DEFAULT_WEEKEND_SLEEP_START_TIME    = "23:00";
    public static final String DEFAULT_WEEKEND_SLEEP_END_TIME      = "09:00";

    public static long DEFAULT_INACTIVITY_PERIOD = 20 * 60 * 1000;
    public static long DEFAULT_REPEAT_CALL_PERIOD = 5 * 60 * 1000;

    public static final String ACTIVITY_DETECTION_TAG = "SleepAgent";
    public static final long ACTIVITY_DETECTION_INTERVAL = ActivityRecognitionHelper.INTERVAL_5_MINUTES;


    @Override
    public int getNameId() {
        return R.string.sleep_agent_title;
    }

    @Override
    public int getDescriptionId() {
        return R.string.sleep_agent_description;
    }

    @Override
    public int getLongDescriptionId() {
        return R.string.sleep_agent_description_long;
    }


    @Override
    public AgentPermission[] getTriggerArray() {
        AgentPermission[] agentPermissions = new AgentPermission[3];
        agentPermissions[0] = new AgentPermission(R.drawable.ic_time, R.string.sleep_agent_trigger_time);
        agentPermissions[1] = new AgentPermission(R.drawable.ic_volume, R.string.sleep_agent_trigger_volume);
        agentPermissions[2] = new AgentPermission(R.drawable.ic_sms, R.string.sleep_agent_trigger_sms);
        return agentPermissions;
    }

    @Override
    public int getIconId() {
        return R.drawable.ic_sleep_agent;
    }

    @Override
    public int getWhiteIconId() {
        return R.drawable.ic_stat_sleep_agent;
    }

    @Override
    public int getColorIconId() {
        return R.drawable.ic_sleep_agent_color;
    }


    @Override
    public int getWidgetOutlineIconId() {
        return R.drawable.ic_widget_sleep_inactive;
    }

    @Override
    public int getWidgetFillIconId() {
        return R.drawable.ic_widget_sleep_active;
    }

    @Override
    public HashMap<String, String> getPreferencesMap() {
        HashMap<String, String> prefs = super.getPreferencesMap();

        defaultPrefs(prefs, AgentPreferences.SILENCE_PHONE, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.SOUND_SILENCE_DEVICE, String.valueOf(true));

        defaultPrefs(prefs, AgentPreferences.TIME_RANGE_TRIGGER, getDefaultActivationRanges(mContext));

        defaultPrefs(prefs, AgentPreferences.SMS_AUTORESPOND, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.PHONE_CALL_AUTORESPOND, String.valueOf(true));
        defaultPrefs(prefs, AgentPreferences.SMS_AUTORESPOND_CONTACTS, AgentPreferences.SMS_AUTORESPOND_CONTACT_NOONE);
        defaultPrefs(prefs, AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL, String.valueOf(false));

        defaultPrefs(prefs, AgentPreferences.BATTERY_SYNC, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.BATTERY_BT, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.BATTERY_WIFI, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.BATTERY_MOBILE_DATA, String.valueOf(false));


        // sleep agent SMS urgency verification always respond only once
        // sleep agent is always using the actions in "wake" mode, i.e. wake the user up
        prefs.put(AgentPreferences.SMS_AUTORESPOND_ONCE, String.valueOf(true));
        prefs.put(AgentPreferences.PHONE_CALL_AUTORESPOND_MODE, AgentPreferences.AUTORESPOND_MODE_WAKE);
        prefs.put(AgentPreferences.SMS_AUTORESPOND_MODE, AgentPreferences.AUTORESPOND_MODE_WAKE);


        // disable mobile data switch by force in Lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            prefs.put(AgentPreferences.BATTERY_MOBILE_DATA, String.valueOf(false));
        }


        String smsAutoRespondMsg = mContext.getResources().getString(R.string.sleep_agent_default_autorespond);
        defaultPrefs(prefs, AgentPreferences.SMS_AUTORESPOND_MESSAGE, smsAutoRespondMsg);

        defaultPrefs(prefs, AgentPreferences.ACTIVATE_ONLY_ON_INACTIVITY, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.MIN_INACTIVE_TIME, String.valueOf(DEFAULT_INACTIVITY_PERIOD));
        defaultPrefs(prefs, AgentPreferences.DEVICE_STILL_FOR, String.valueOf(0));

        defaultPrefs(prefs, AgentPreferences.START_ONLY_WHEN_CHARGING, String.valueOf(false));

        upgradePrefs122(prefs);

        return prefs;
    }

    @Override
    public AgentUIElement[] getSettings(AgentConfigurationProvider acp) {
        HashMap<String, String> agentPreferencesMap = getPreferencesMap();

        int numSettings = 29;
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)) {
            numSettings += 1;
        }

        AgentUIElement[] settings = new AgentUIElement[numSettings];
        HashedNumberDispenser position = new HashedNumberDispenser();

        settings[position.generate(AgentPreferences.TIME_RANGE_TRIGGER)] = new AgentTimeRangeSetting(acp, AgentPreferences.TIME_RANGE_TRIGGER, agentPreferencesMap.get(AgentPreferences.TIME_RANGE_TRIGGER), R.string.sleep_ranges_description);

        settings[position.generate(null)] = new AgentSpacerSetting(acp);

        settings[position.generate(AgentPreferences.SMS_AUTORESPOND_CONTACTS)] = new AgentContactsSetting(acp, R.string.contacts_allowed, AgentPreferences.SMS_AUTORESPOND_CONTACTS, agentPreferencesMap.get(AgentPreferences.SMS_AUTORESPOND_CONTACTS));
        settings[position.generate(AgentPreferences.PHONE_CALL_AUTORESPOND)] = new AgentCheckboxSetting(acp, R.string.config_wake_phone, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.PHONE_CALL_AUTORESPOND)), true, AgentPreferences.PHONE_CALL_AUTORESPOND);
        settings[position.generate(AgentPreferences.SMS_AUTORESPOND)] = new AgentCheckboxSetting(acp, R.string.config_wake_sms, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SMS_AUTORESPOND)), true, AgentPreferences.SMS_AUTORESPOND);

        settings[position.generate(null)] = new AgentLabelSetting(acp, R.string.config_describe_wake_me);

        settings[position.generate(null)] = null;

        settings[position.generate(AgentPreferences.ACTIVATE_ONLY_ON_INACTIVITY)] = new AgentCheckboxSetting(acp, R.string.sleep_agent_activate_on_inactivity, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.ACTIVATE_ONLY_ON_INACTIVITY)), true, AgentPreferences.ACTIVATE_ONLY_ON_INACTIVITY);

        ArrayList<KeyValue> options = new ArrayList<KeyValue>();
        String minutesString = acp.getActivity().getResources().getString(R.string.minutes);
        String[] minuteKeys = {"5", "10", "20", "30", "60"};
        String[] minuteVals = {"300000", "600000", "1200000", "1800000", "3600000"};

        for(int i=0; i<minuteKeys.length; i++) {
            KeyValue kv = new KeyValue(minuteKeys[i] + " " + minutesString, minuteVals[i]);
            options.add(kv);
        }

        settings[position.generate(AgentPreferences.MIN_INACTIVE_TIME)] = new AgentSpinnerSetting(acp, R.string.sleep_agent_inactivity_time, options, AgentPreferences.MIN_INACTIVE_TIME, agentPreferencesMap.get(AgentPreferences.MIN_INACTIVE_TIME));
        settings[position.generate("INACTIVITY_LABEL")] = new AgentLabelSetting(acp, R.string.config_describe_inactivity);

        settings[position.generate(null)] = new AgentSpacerSetting(acp);

        settings[position.generate(AgentPreferences.START_ONLY_WHEN_CHARGING)] = new AgentCheckboxSetting(acp, R.string.config_start_only_when_charging, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.START_ONLY_WHEN_CHARGING)), true, AgentPreferences.START_ONLY_WHEN_CHARGING);

        settings[position.generate(null)] = new AgentSpacerSetting(acp);

        settings[position.generate(AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT)] = new AgentCheckboxSetting(acp, R.string.config_urgent_mode, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT)), true, AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT);
        settings[position.generate(AgentPreferences.SMS_AUTORESPOND_MESSAGE)] = new AgentTextLineSetting(acp, R.string.config_urgent_verification, AgentPreferences.SMS_AUTORESPOND_MESSAGE, agentPreferencesMap.get(AgentPreferences.SMS_AUTORESPOND_MESSAGE));
        settings[position.generate("SMS_AUTORESPONSE_LABEL")] = new AgentLabelSetting(acp, R.string.config_urgent_sleep_description);

        settings[position.generate(null)] = new AgentSpacerSetting(acp);

        settings[position.generate(AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL)] = new AgentCheckboxSetting(acp, R.string.config_allow_repeat, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL)), true, AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL);
        settings[position.generate("REPEAT_PHONE_CALL_LABEL")] = new AgentLabelSetting(acp, R.string.config_allow_repeat_description_wake);

        settings[position.generate(null)] = new AgentSpacerSetting(acp);

        settings[position.generate(AgentPreferences.SILENCE_PHONE)] = new AgentCheckboxSetting(acp, R.string.config_silence_phone, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SILENCE_PHONE)), true, AgentPreferences.SILENCE_PHONE);
        settings[position.generate(AgentPreferences.SOUND_SILENCE_DEVICE)] = new AgentRadioBooleanSetting(acp, R.string.config_silence_volume, R.string.config_silence_on, R.string.config_silence_off,  true, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SOUND_SILENCE_DEVICE)), AgentPreferences.SOUND_SILENCE_DEVICE);

        settings[position.generate(null)] = new AgentSpacerSetting(acp);

        settings[position.generate(AgentPreferences.BATTERY_SYNC)] = new AgentCheckboxSetting(acp, R.string.config_turn_off_syncing, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.BATTERY_SYNC)), true, AgentPreferences.BATTERY_SYNC);
        settings[position.generate(AgentPreferences.BATTERY_BT)] = new AgentCheckboxSetting(acp, R.string.config_turn_off_bt, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.BATTERY_BT)), true, AgentPreferences.BATTERY_BT);
        settings[position.generate(AgentPreferences.BATTERY_WIFI)] = new AgentCheckboxSetting(acp, R.string.config_turn_off_wifi, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.BATTERY_WIFI)), true, AgentPreferences.BATTERY_WIFI);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            settings[position.generate(AgentPreferences.BATTERY_MOBILE_DATA)] = new AgentCheckboxSetting(acp, R.string.config_turn_off_mobile_data, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.BATTERY_MOBILE_DATA)), true, AgentPreferences.BATTERY_MOBILE_DATA);
        }

        settings[position.generate("BATTERY_SAVING_OPTIONS_LABEL")] = new AgentLabelSetting(acp, R.string.config_sleep_actions_battery_desc);

        position.lock();

        OrChildCheck inactivity_cascade = new OrChildCheck();
        inactivity_cascade.addConsequence(settings[position.fetch(AgentPreferences.MIN_INACTIVE_TIME)]);
        inactivity_cascade.addConditional(settings[position.fetch(AgentPreferences.ACTIVATE_ONLY_ON_INACTIVITY)]);

        OrChildCheck response_cascade = new OrChildCheck();
        response_cascade.addConsequence(settings[position.fetch(AgentPreferences.SMS_AUTORESPOND_MESSAGE)]);
        response_cascade.addConditional(settings[position.fetch(AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT)]);

        OrChildCheck sound_cascade = new OrChildCheck();
        sound_cascade.addConsequence(settings[position.fetch(AgentPreferences.SOUND_SILENCE_DEVICE)]);
        sound_cascade.addConditional(settings[position.fetch(AgentPreferences.SILENCE_PHONE)]);

        OrChildCheck phone_calls_cascade = new OrChildCheck();
        phone_calls_cascade.addConsequence(settings[position.fetch(AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL)]);
        phone_calls_cascade.addConsequence(settings[position.fetch("REPEAT_PHONE_CALL_LABEL")]);
        phone_calls_cascade.addConditional(settings[position.fetch(AgentPreferences.PHONE_CALL_AUTORESPOND)]);

        ChildCheck responseCheck = new OrChildCheck();
        responseCheck.addConsequence(settings[position.fetch(AgentPreferences.SMS_AUTORESPOND_CONTACTS)]);
        responseCheck.addConsequence(settings[position.fetch(AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT)]);
        responseCheck.addConditional(settings[position.fetch(AgentPreferences.SMS_AUTORESPOND)]);
        responseCheck.addConditional(settings[position.fetch(AgentPreferences.PHONE_CALL_AUTORESPOND)]);

        return settings;
    }

    @Override
    public void afterInstall(Context context, boolean silent, boolean skipCheckReceivers) {
        SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();

        TaskDatabaseHelper.createAction(db, getGuid(), "PhoneSilenceAction", Constants.TRIGGER_TYPE_TIME);
        TaskDatabaseHelper.createAction(db, getGuid(), "BatteryAgentAction", Constants.TRIGGER_TYPE_TIME);
        TaskDatabaseHelper.createAction(db, getGuid(), "PhoneSilenceAction", Constants.TRIGGER_TYPE_MANUAL);
        TaskDatabaseHelper.createAction(db, getGuid(), "BatteryAgentAction", Constants.TRIGGER_TYPE_MANUAL);

        TaskDatabaseHelper.createAction(db, getGuid(), "AutorespondSmsAction", Constants.TRIGGER_TYPE_SMS);
        TaskDatabaseHelper.createAction(db, getGuid(), "AutorespondPhoneCallAction", Constants.TRIGGER_TYPE_PHONE_CALL);
        TaskDatabaseHelper.createAction(db, getGuid(), "VerifyUrgentSmsAction", Constants.TRIGGER_TYPE_MISSED_CALL);

        resetTimeTriggers();

        TaskDatabaseHelper.createTrigger(db, getGuid(), Constants.TRIGGER_TYPE_SMS);
        TaskDatabaseHelper.createTrigger(db, getGuid(), Constants.TRIGGER_TYPE_PHONE_CALL);
        TaskDatabaseHelper.createTrigger(db, getGuid(), Constants.TRIGGER_TYPE_MISSED_CALL);

        if (! skipCheckReceivers) {
            Utils.checkReceivers(context);
        }
        // optimization HACK: since AD is off by default; no need to activate/check AD:
        //resetActivityDetection();
    }

    @Override
    public void afterUninstall(Context context, boolean silent) {
        resetActivityDetection();
    }




    @Override
    public boolean havePreconditionsBeenMet() {
        if (!isInstalled()) {return false;}  // handles stray alarms after uninstall
        if (isActive()) {return false;}  // optimization

        HashMap<String, String> prefsMap = getPreferencesMap();

        boolean correctTime = isWithinUserTime();
        if (!correctTime) {return false;}

        boolean startOnlyWhenCharging = prefsMap.get(AgentPreferences.START_ONLY_WHEN_CHARGING).equals("true");
        if (startOnlyWhenCharging && !Utils.isPhonePluggedIn(mContext)) {
            Logger.d("precondition failed: phone not plugged in.");
            return false;
        }


        boolean activateOnInactivityOnly = prefsMap.get(AgentPreferences.ACTIVATE_ONLY_ON_INACTIVITY).equals("true");
        if (!activateOnInactivityOnly) {return true;}

        boolean screenOn = Utils.isScreenOn(mContext);
        if (screenOn) {
            Logger.d("precondition failed: screen is on; not activating.");
            setActivateRetryAlarm();
            return false;
        }

        long lastDetection = ActivityRecognitionHelper.getLastDetectionUpdate(mContext);
        long deviceStillFor = Long.valueOf(prefsMap.get(AgentPreferences.DEVICE_STILL_FOR));
        long minInactiveTime = Long.valueOf(prefsMap.get(AgentPreferences.MIN_INACTIVE_TIME));
        long inactiveADFor = (lastDetection == ActivityRecognitionHelper.UNKNOWN_LAST_UPDATE) ? -1 : System.currentTimeMillis() - lastDetection;
        Logger.d("inactiveADFor: " + inactiveADFor + ", deviceStillFor: " + deviceStillFor + " vs minInactiveTime: " + minInactiveTime);

        if ((inactiveADFor == -1) || (inactiveADFor >= minInactiveTime)) {
            Logger.d("inactive AD; assuming still.");
            return true;
        }

        if (deviceStillFor < minInactiveTime) {
            setActivateRetryAlarm();
            return false;
        }

        return true;
    }


    public void setStillFor(long stillForMillis) {
        updatePreference(AgentPreferences.DEVICE_STILL_FOR, String.valueOf(stillForMillis));
    }

    public boolean isWithinUserTime() {
        HashMap<String, String> prefsMap = getPreferencesMap();

        List<AgentTimeRange> timeRanges = AgentTimeRange.getTimeRanges(prefsMap.get(AgentPreferences.TIME_RANGE_TRIGGER));
        for (AgentTimeRange atr:timeRanges) {
            if (atr.nowInRange()) {
                Logger.d("isWithinUserTime: true; atr=" + atr.serialize());
                return true;
            }
        }

        Logger.d("isWithinUserTime: false");
        return false;
    }



    @Override
    public HashMap<String, String> updatePreference(String prefName, String prefVal) {
        HashMap<String, String> prefs = super.updatePreference(prefName, prefVal);

        if (prefName.equals(AgentPreferences.ACTIVATE_ONLY_ON_INACTIVITY)) {
            prefs = updatePreference(AgentPreferences.DEVICE_STILL_FOR, String.valueOf(0));
            Utils.checkReceivers(mContext);
            resetActivityDetection();
            return prefs;
        }

        if (prefName.equals(AgentPreferences.TIME_RANGE_TRIGGER)) {
            resetTimeTriggers();
            Utils.checkReceivers(mContext);
            return prefs;
        }

        return prefs;
    }

    @Override
    public boolean needsUIPause() { return true;}


    @Override
    public boolean needsActivityDetection() {
        return isInstalled() && Boolean.parseBoolean(getPreferencesMap().get(AgentPreferences.ACTIVATE_ONLY_ON_INACTIVITY));
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
        return SleepAgentActivityReceiver.class;
    }


    @Override
    public String getEnabledStatusMessage() {
        return mContext.getResources().getString(R.string.agent_status_bar_enabled_sleep_agent);
    }


    @Override
    public String getStartedStatusMessage() {
        if (mTriggeredBy == Constants.TRIGGER_TYPE_MANUAL) {
            return mContext.getResources().getString(R.string.agent_status_bar_started_manual);
        }
        return mContext.getResources().getString(R.string.agent_status_bar_started_sleep_agent);
    }

    @Override
    public String getPausedStatusMessage() {
        return mContext.getResources().getString(R.string.agent_status_bar_paused_sleep_agent);
    }



    @Override
    public String getStartedLogMessage(int triggerType, boolean unpause) {
        String message = mContext.getResources().getString(R.string.agent_started_sleep_agent);
        return message;
    }

    @Override
    public String getFinishedLogMessage(int triggerType, long ranForMillis) {
        String message = mContext.getResources().getString(R.string.agent_finished_sleep_agent);
        return message;
    }

    @Override
    public String[] getNotifActionLines(Context context, int triggerType, boolean unpause) {
        ArrayList<String> actions = new ArrayList<String>();

        HashMap<String, String> prefs = getPreferencesMap();

        boolean silence = Boolean.parseBoolean(prefs.get(AgentPreferences.SILENCE_PHONE));
        boolean text = Boolean.parseBoolean(prefs.get(AgentPreferences.SMS_AUTORESPOND));
        boolean phone = Boolean.parseBoolean(prefs.get(AgentPreferences.PHONE_CALL_AUTORESPOND));
        boolean verifyUrgent = Boolean.parseBoolean(prefs.get(AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT));
        boolean sync = Boolean.parseBoolean(prefs.get(AgentPreferences.BATTERY_SYNC));
        boolean bluetooth = Boolean.parseBoolean(prefs.get(AgentPreferences.BATTERY_BT));
        boolean wifi = Boolean.parseBoolean(prefs.get(AgentPreferences.BATTERY_WIFI));
        boolean mobileData = Boolean.parseBoolean(prefs.get(AgentPreferences.BATTERY_MOBILE_DATA));


        if(silence) actions.add(context.getResources().getString(R.string.sleep_agent_silence));
        if(text) actions.add(context.getResources().getString(R.string.sleep_agent_text));
        if(phone) actions.add(context.getResources().getString(R.string.sleep_agent_phone));
        if(verifyUrgent) actions.add(context.getResources().getString(R.string.sleep_agent_verify_urgent));
        if(sync) actions.add(context.getResources().getString(R.string.battery_agent_autosync_off));
        if(bluetooth) actions.add(context.getResources().getString(R.string.battery_agent_bt_off));
        if(wifi) actions.add(context.getResources().getString(R.string.battery_agent_wifi_off));
        if(mobileData) actions.add(context.getResources().getString(R.string.battery_agent_mobile_data_off));


        return actions.toArray(new String[actions.size()]);
    }


    @Override
    public String getNotifStartTitle(int triggerType, boolean unpause) {
        int title_res;
        if (unpause || (Constants.TRIGGER_TYPE_MANUAL == triggerType)) {
            title_res = R.string.agent_started_title_sleep_manual;
        } else {
            title_res = R.string.agent_started_title_sleep;
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

        int res = R.string.first_notification_sleep;

        return mContext.getResources().getString(res);
    }

    @Override
    public String getNotifFirstStartDialogDescription() {
        return mContext.getResources().getString(R.string.sleep_agent_description_first);
    }


    @Override
    public String getNotifPauseActionString(int triggerType) {

        int title_res;
        if(Constants.TRIGGER_TYPE_MANUAL == triggerType) {
            title_res = R.string.agent_action_pause_manual;
        } else {
            title_res = R.string.agent_action_skip;
        }

        return mContext.getResources().getString(title_res);
    }

    @Override
    public String getNotifDelayActionString(int triggerType) {
        return mContext.getResources().getString(R.string.agent_action_delay);
    }

    @Override
    public String getNotifDelayTitle(int triggerType) {
        return mContext.getResources().getString(R.string.agent_delayed_title_sleep);
    }

    @Override
    public String getNotifDelayMessage(int triggerType) {
        return mContext.getResources().getString(R.string.agent_delayed_message_sleep);
    }




    private void setActivateRetryAlarm() {
        Logger.d("SleepAgent: setting active retry alarm");
        // fire off alarm in 4 minutes to check again
        // AD will poll normally but this ensures that it will still work if AD is broken or off
        AlarmReceiver.setAlarmAtTime(mContext, HARDCODED_GUID,
                System.currentTimeMillis() + 240000,
                AlarmReceiver.ALARM_AGENT_ACTIVATE, AlarmReceiver.SLEEP_AGENT_CHECKER_ALARM_REQ_ID,
                Constants.TRIGGER_TYPE_TIME);

    }


    private static String getDefaultActivationRanges(Context context) {
        List<AgentTimeRange> defaultRanges = new ArrayList<AgentTimeRange>();

        for (int i=0; i < 7; i++) {
            AgentTimeRange atr = new AgentTimeRange();
            int startDay = Calendar.SUNDAY + i;
            atr.setStartDay(startDay);
            atr.setEndDay(startDay == Calendar.SATURDAY ? Calendar.SUNDAY : startDay + 1);
            if (i < 5) {
                atr.setStartTimeOfDay(DEFAULT_WEEKDAY_SLEEP_START_TIME);
                atr.setEndTimeOfDay(DEFAULT_WEEKDAY_SLEEP_END_TIME);
            } else {
                atr.setStartTimeOfDay(DEFAULT_WEEKEND_SLEEP_START_TIME);
                atr.setEndTimeOfDay(DEFAULT_WEEKEND_SLEEP_END_TIME);
            }
            defaultRanges.add(atr);
        }

        return AgentTimeRange.serializeList(defaultRanges);
    }

    //Added in 1.2.2; remove when enough people upgrade
    @Deprecated
    private void upgradePrefs122(HashMap<String, String> prefs) {

        if (!(prefs.containsKey(AgentPreferences.TIME_START_TRIGGER) ||
                prefs.containsKey(AgentPreferences.TIME_END_TRIGGER) ||
                prefs.containsKey(AgentPreferences.DAYS_OF_WEEK)))  {
            return;
        }

        Logger.d("SleepAgent: doing prefs upgrade for 1.2.2");

        String DEFAULT_SLEEP_DAYS = "1,2,3,4,5,6,7"; // old default sleep days
        defaultPrefs(prefs, AgentPreferences.TIME_START_TRIGGER, DEFAULT_WEEKDAY_SLEEP_START_TIME);
        defaultPrefs(prefs, AgentPreferences.TIME_END_TRIGGER, DEFAULT_WEEKDAY_SLEEP_END_TIME);
        defaultPrefs(prefs, AgentPreferences.DAYS_OF_WEEK, DEFAULT_SLEEP_DAYS);

        long startMillis = Utils.getMillisFromTimeString(prefs.get(AgentPreferences.TIME_START_TRIGGER));
        long endMillis = Utils.getMillisFromTimeString(prefs.get(AgentPreferences.TIME_END_TRIGGER));


        String[] dayStrings = prefs.get(AgentPreferences.DAYS_OF_WEEK).split(",");
        ArrayList<AgentTimeRange> timeRanges = new ArrayList<AgentTimeRange>(7);
        for (String dayStr:dayStrings) {
            if ((dayStr==null) || (dayStr.trim().isEmpty())) {continue;}
            int day;
            try {
                day = Integer.parseInt(dayStr);
            } catch (Exception e) {
                day = -1;
            }
            if ((day < 1) || (day > 7)) {
                Logger.d("Bad conversion: " + dayStr);
                continue;
            }

            AgentTimeRange tr = new AgentTimeRange();
            tr.setStartDay(day);
            if (startMillis <= endMillis) {
                tr.setEndDay(day);
            } else {
                tr.setEndDay(day == 7 ? 1 : day+1);
            }
            tr.setStartTimeOfDay(prefs.get(AgentPreferences.TIME_START_TRIGGER));
            tr.setEndTimeOfDay(prefs.get(AgentPreferences.TIME_END_TRIGGER));
            timeRanges.add(tr);
        }

        deletePreferenceForUpgrade(prefs, AgentPreferences.TIME_START_TRIGGER);
        deletePreferenceForUpgrade(prefs, AgentPreferences.TIME_END_TRIGGER);
        deletePreferenceForUpgrade(prefs, AgentPreferences.DAYS_OF_WEEK);

        String newTimePrefVal = AgentTimeRange.serializeList(timeRanges);
        updatePreference(AgentPreferences.TIME_RANGE_TRIGGER, newTimePrefVal); // will update in db
        prefs.put(AgentPreferences.TIME_RANGE_TRIGGER, newTimePrefVal);
    }

    private void resetTimeTriggers() {
        SQLiteDatabase db = TaskDatabaseHelper.getInstance(mContext).getReadableDatabase();
        TaskDatabaseHelper.deleteTriggersOfType(db, getGuid(), Constants.TRIGGER_TYPE_TIME);

        List<AgentTimeRange> timeRanges = AgentTimeRange.getTimeRanges(getPreferencesMap().get(AgentPreferences.TIME_RANGE_TRIGGER));
        for (AgentTimeRange tr:timeRanges) {
            TaskDatabaseHelper.createTrigger(db, getGuid(), Constants.TRIGGER_TYPE_TIME, tr.serialize(), null);
        }
    }

    @Override
    protected boolean shouldPlayStartNotification() {
        return false;
    }

}
