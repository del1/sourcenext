package com.mobiroo.n.sourcenextcorporation.agent.item;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.provider.CalendarContract.Instances;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentCheckboxSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentCheckboxWithColorSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentContactsSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentDayOfWeekSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentIntSliderSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentLabelSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentPermission;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentRadioBooleanSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentSpacerSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentTextLineSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentTimeSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentUIElement;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.ChildCheck;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.dependancy.OrChildCheck;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentNotificationInterface;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.DelayableNotificationInterface;
import com.mobiroo.n.sourcenextcorporation.agent.service.MeetingAgentIntentService;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.HashedNumberDispenser;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.UserCalendarUtils;
import com.mobiroo.n.sourcenextcorporation.agent.util.UserCalendarUtils.EventInfo;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class MeetingAgent extends StaticAgent
        implements UserCalendarUtils.EventTimeChecker, AgentNotificationInterface, DelayableNotificationInterface {
    public static final String HARDCODED_GUID = "tryagent.meeting";

    public static final String DEFAULT_WORKDAY_START_TIME = "08:00";
    public static final String DEFAULT_WORKDAY_END_TIME = "18:00";

    private static final String    PREF_CURRENT_START      	= "meeting.agent.curent_start";
    private static final String    PREF_CURRENT_END        	= "meeting.agent.current_end";
    private static final String    PREF_CURRENT_START_ID   	= "metting.agent.current_start_id";
    private static final String    PREF_CURRENT_END_ID     	= "meeting.agent.current_end_id";
    private static final String    PREF_CURRENT_MEETING_NAME   = "meeting.agent.curent_meeting_name";
    private static final String    PREF_MEETING_SKIPPED_AT = "meeting.agent.skipped_at";

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("MM-dd-yyyy kk:mm");

    private final static String DEFAULT_DAYS = "2,3,4,5,6";

    @Override
    public int getNameId() {
        return R.string.meeting_agent_title;
    }

    @Override
    public int getDescriptionId() {
        return R.string.meeting_agent_description;
    }

    @Override
    public int getLongDescriptionId() {
        return R.string.meeting_agent_description_long;
    }



    @Override
    public AgentPermission[] getTriggerArray() {
        AgentPermission[] agentPermissions = new AgentPermission[1];
        agentPermissions[0] = new AgentPermission(R.drawable.ic_time, R.string.meeting_agent_calendar);
        return agentPermissions;
    }

    @Override
    public int getIconId() {
        return R.drawable.ic_meeting_agent;
    }

    @Override
    public int getWhiteIconId() {
        return R.drawable.ic_stat_meeting_white;
    }

    @Override
    public int getColorIconId() {
        return R.drawable.ic_meeting_agent_color;
    }


    @Override
    public int getWidgetOutlineIconId() {
        return R.drawable.ic_widget_meeting_inactive;
    }

    @Override
    public int getWidgetFillIconId() {
        return R.drawable.ic_widget_meeting_active;
    }


    @Override
    public HashMap<String, String> getPreferencesMap() {
        HashMap<String, String> prefs = super.getPreferencesMap();

        defaultPrefs(prefs, AgentPreferences.TIME_START_TRIGGER, DEFAULT_WORKDAY_START_TIME);
        defaultPrefs(prefs, AgentPreferences.TIME_END_TRIGGER, DEFAULT_WORKDAY_END_TIME);
        defaultPrefs(prefs, AgentPreferences.DAYS_OF_WEEK, DEFAULT_DAYS);
        defaultPrefs(prefs, AgentPreferences.MEETING_BUSY_ONLY, String.valueOf(true));
        defaultPrefs(prefs, AgentPreferences.MEETING_ACCEPTED_ONLY, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.SOUND_SILENCE_DEVICE, String.valueOf(true));


        defaultPrefs(prefs, AgentPreferences.SMS_AUTORESPOND, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.PHONE_CALL_AUTORESPOND, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.SMS_AUTORESPOND_CONTACTS, AgentPreferences.SMS_AUTORESPOND_CONTACT_NOONE);
        defaultPrefs(prefs, AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL, String.valueOf(false));
        defaultPrefs(prefs, AgentPreferences.MEETING_START_EARLY_M, String.valueOf(0));

        prefs.put(AgentPreferences.SMS_AUTORESPOND_ONCE, String.valueOf(true));
        prefs.put(AgentPreferences.PHONE_CALL_AUTORESPOND_MODE, AgentPreferences.AUTORESPOND_MODE_WAKE);
        prefs.put(AgentPreferences.SMS_AUTORESPOND_MODE, AgentPreferences.AUTORESPOND_MODE_WAKE);
        prefs.put(AgentPreferences.MEETING_IGNORE_ALL_DAY, String.valueOf(true));

        String smsAutoRespondMsg = mContext.getResources().getString(R.string.meeting_agent_default_autorespond);
        defaultPrefs(prefs, AgentPreferences.SMS_AUTORESPOND_MESSAGE, smsAutoRespondMsg);

        return prefs;
    }

    @Override
    public AgentUIElement[] getSettings(AgentConfigurationProvider acp) {
        HashMap<String, String> agentPreferencesMap = getPreferencesMap();
        Account[] accounts = MeetingAgent.getAccounts(acp.getActivity());

        HashedNumberDispenser position = new HashedNumberDispenser();

        // Get calendars for these accounts

        int offset = accounts.length;
        for (int i=0; i < accounts.length; i++) {
            offset += UserCalendarUtils.getCalendarsForAccount(acp.getActivity(), accounts[i]).length;
        }

        AgentUIElement[] settings = new AgentUIElement[25 + offset];

        settings[position.generate(null)] = new AgentLabelSetting(acp, R.string.accounts_description);

        settings[offset + position.generate(null)] = null;

        settings[offset + position.generate("MEETING_AGENT_TIME_RANGE_LABEL")] = new AgentLabelSetting(acp, R.string.meeting_agent_time_title);
        settings[offset + position.generate(AgentPreferences.TIME_START_TRIGGER)] = new AgentTimeSetting(acp, R.string.start_time, AgentPreferences.TIME_START_TRIGGER, agentPreferencesMap.get(AgentPreferences.TIME_START_TRIGGER));
        settings[offset + position.generate(AgentPreferences.TIME_END_TRIGGER)] = new AgentTimeSetting(acp, R.string.end_time, AgentPreferences.TIME_END_TRIGGER, agentPreferencesMap.get(AgentPreferences.TIME_END_TRIGGER));

        settings[offset + position.generate(null)] = new AgentSpacerSetting(acp);

        settings[offset + position.generate(AgentPreferences.MEETING_BUSY_ONLY)] = new AgentCheckboxSetting(acp, R.string.config_busy_only, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.MEETING_BUSY_ONLY)), true, AgentPreferences.MEETING_BUSY_ONLY);

        settings[offset + position.generate(null)] = new AgentSpacerSetting(acp);

        settings[offset + position.generate(AgentPreferences.MEETING_START_EARLY_M)] = new AgentIntSliderSetting(acp, R.string.meeting_start_early, Integer.parseInt(agentPreferencesMap.get(AgentPreferences.MEETING_START_EARLY_M)), 30, AgentPreferences.MEETING_START_EARLY_M, acp.getActivity().getResources().getString(R.string.meeting_format));

        settings[offset + position.generate(null)] = new AgentSpacerSetting(acp);

        settings[offset + position.generate("DAYS_OF_WEEK_LABEL")] = new AgentLabelSetting(acp, R.string.days_of_week_description_calendar);
        settings[offset + position.generate(AgentPreferences.DAYS_OF_WEEK)] = new AgentDayOfWeekSetting(acp, AgentPreferences.DAYS_OF_WEEK, agentPreferencesMap.get(AgentPreferences.DAYS_OF_WEEK));

        settings[offset + position.generate(null)] = new AgentSpacerSetting(acp);

        settings[offset + position.generate(AgentPreferences.SOUND_SILENCE_DEVICE)] = new AgentRadioBooleanSetting(acp, R.string.config_silence_volume, R.string.config_silence_on, R.string.config_silence_off, true, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SOUND_SILENCE_DEVICE)), AgentPreferences.SOUND_SILENCE_DEVICE);

        settings[offset + position.generate(null)] = new AgentSpacerSetting(acp);

        settings[offset + position.generate(AgentPreferences.PHONE_CALL_AUTORESPOND)] = new AgentCheckboxSetting(acp, R.string.meeting_agent_config_phone, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.PHONE_CALL_AUTORESPOND)), true, AgentPreferences.PHONE_CALL_AUTORESPOND);
        settings[offset + position.generate(AgentPreferences.SMS_AUTORESPOND)] = new AgentCheckboxSetting(acp, R.string.meeting_agent_config_text, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SMS_AUTORESPOND)), true, AgentPreferences.SMS_AUTORESPOND);
        settings[offset + position.generate(AgentPreferences.SMS_AUTORESPOND_CONTACTS)] = new AgentContactsSetting(acp, R.string.meeting_agent_config_contacts, AgentPreferences.SMS_AUTORESPOND_CONTACTS, agentPreferencesMap.get(AgentPreferences.SMS_AUTORESPOND_CONTACTS));

        settings[offset + position.generate(null)] = new AgentSpacerSetting(acp);

        settings[offset + position.generate(AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT)] = new AgentCheckboxSetting(acp, R.string.config_urgent_mode, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT)), true, AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT);


        String mVal = agentPreferencesMap.get(AgentPreferences.SMS_AUTORESPOND_MESSAGE);
        settings[offset + position.generate(AgentPreferences.SMS_AUTORESPOND_MESSAGE)] = new AgentTextLineSetting(acp, R.string.config_urgent_verification, AgentPreferences.SMS_AUTORESPOND_MESSAGE, mVal);

        settings[offset + position.generate("URGENCY_MESSAGE_LABEL")] = new AgentLabelSetting(acp, R.string.config_urgent_meeting_description);

        settings[offset + position.generate(null)] = new AgentSpacerSetting(acp);

        settings[offset + position.generate(AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL)] = new AgentCheckboxSetting(acp, R.string.config_allow_repeat, Boolean.parseBoolean(agentPreferencesMap.get(AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL)), true, AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL);
        settings[offset + position.generate("REPEAT_PHONE_CALL_LABEL")] = new AgentLabelSetting(acp, R.string.config_allow_repeat_description_through);


        position.lock();

        OrChildCheck phone_calls_cascade = new OrChildCheck();
        phone_calls_cascade.addConsequence(settings[offset + position.fetch(AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL)]);
        phone_calls_cascade.addConsequence(settings[offset + position.fetch("REPEAT_PHONE_CALL_LABEL")]);
        phone_calls_cascade.addConditional(settings[offset + position.fetch(AgentPreferences.PHONE_CALL_AUTORESPOND)]);

        ChildCheck wl_check = new OrChildCheck();
        wl_check.addConditional(settings[offset + position.fetch(AgentPreferences.PHONE_CALL_AUTORESPOND)]);
        wl_check.addConditional(settings[offset + position.fetch(AgentPreferences.SMS_AUTORESPOND)]);
        wl_check.addConsequence(settings[offset + position.fetch(AgentPreferences.SMS_AUTORESPOND_CONTACTS)]);
        wl_check.addConsequence(settings[offset + position.fetch(AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT)]);

        OrChildCheck urgency_cascade = new OrChildCheck();
        urgency_cascade.addConsequence(settings[offset + position.fetch(AgentPreferences.SMS_AUTORESPOND_MESSAGE)]);
        urgency_cascade.addConditional(settings[offset + position.fetch("URGENCY_MESSAGE_LABEL")]);


        int count = 0;
        for (int i=0; i < accounts.length; i++) {
            settings[1 + count++] = new AgentLabelSetting(acp, accounts[i].name, AgentLabelSetting.LABEL_TYPE.SUBHEADER);

            SubCalendar[] calendars = UserCalendarUtils.getCalendarsForAccount(acp.getActivity(), accounts[i]);
            for (int j=0; j < calendars.length; j++) {
                String pref = AgentPreferences.MEETING_ACCOUNTS + accounts[i].name + "_" + calendars[j].id;
                boolean mainCalendar = accounts[i].name.equals(calendars[j].name);
                boolean value = Utils.getBoolPref(agentPreferencesMap, pref, mainCalendar);
                String name = (mainCalendar ? acp.getActivity().getString(R.string.main_calendar) : calendars[j].name);
                settings[1 + count] = new AgentCheckboxWithColorSetting(acp, name, calendars[j].color, value, true, pref);
                count++;
            }

        }

        return settings;
    }

    public static class SubCalendar {
        public int color;
        public String name;
        public String id;

        public SubCalendar(String id, String name, int color) {
            this.id = id;
            this.color = color;
            this.name = name;
        }
    }

    @Override
    public void afterInstall(Context context, boolean silent, boolean skipCheckReceivers) {
        SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();
        TaskDatabaseHelper.createAction(db, getGuid(), "PhoneSilenceAction", Constants.TRIGGER_TYPE_TIME);
        TaskDatabaseHelper.createAction(db, getGuid(), "PhoneSilenceAction", Constants.TRIGGER_TYPE_MANUAL);

        TaskDatabaseHelper.createAction(db, getGuid(), "AutorespondSmsAction", Constants.TRIGGER_TYPE_SMS);
        TaskDatabaseHelper.createAction(db, getGuid(), "AutorespondPhoneCallAction", Constants.TRIGGER_TYPE_PHONE_CALL);
        TaskDatabaseHelper.createAction(db, getGuid(), "VerifyUrgentSmsAction", Constants.TRIGGER_TYPE_MISSED_CALL);

        TaskDatabaseHelper.createTrigger(db, getGuid(), Constants.TRIGGER_TYPE_MISSED_CALL);
        TaskDatabaseHelper.createTrigger(db, getGuid(), Constants.TRIGGER_TYPE_SMS);
        TaskDatabaseHelper.createTrigger(db, getGuid(), Constants.TRIGGER_TYPE_PHONE_CALL);


        updateScheduledAlarms(context);
    }

    @Override
    public void afterDeactivate(int triggerType, boolean pause) {
        if (pause) {
            Logger.d("Paused; updating scheduled alarms to pick up sub events in composite event");
            updateScheduledAlarms(mContext);
        }
    }

    public static Account[] getAccounts(Context context) {
        if(Utils.isPermissionGranted(context, Manifest.permission.GET_ACCOUNTS)) {
            AccountManager manager = AccountManager.get(context);

            StringBuilder sb = new StringBuilder();
            sb.append("Accounts: ");

            ArrayList<Account> accounts = new ArrayList<Account>();
            for (Account account : manager.getAccounts()) {
                int calCount = (UserCalendarUtils.getCalendarsForAccount(context, account)).length;
                sb.append(account.name + "=" + account.type + "=" + calCount + ", ");
                if (calCount > 0) {
                    accounts.add(account);
                }
            }
            logd(sb.toString());
            return accounts.toArray(new Account[accounts.size()]);
        }

        return new Account[0];
    }



    public void updateScheduledAlarms(Context context) {
        if (!isInstalled()) { return;}

        HashMap<String, String> prefs = getPreferencesMap();

        EventInfo pending = new EventInfo("fake", -1, -1, Instances.AVAILABILITY_FREE, -1, -1, 0);
        boolean busyOnly = Boolean.parseBoolean(prefs.get(AgentPreferences.MEETING_BUSY_ONLY));
        boolean acceptedOnly = false;
        boolean ignoreAllDay = Boolean.parseBoolean(prefs.get(AgentPreferences.MEETING_IGNORE_ALL_DAY));

        // Iterate over each account and find the next event we need to schedule for
        for (Account account: getAccounts(context)) {

            EventInfo event = UserCalendarUtils.getNextCompositeEventForUser(context, account,
                    busyOnly, acceptedOnly, ignoreAllDay, this);
            if (event == null) { continue;}  // no events in this account

            // event start is in past; skip
            if (event.getStart() < System.currentTimeMillis()) { continue;}

            if (pending.getStart() == -1) {
                logd("Setting current event to pending as pending was not initilized");
                pending = event;
                continue;
            }

            if (event.getStart() < pending.getStart()) {
                logd("Found event that starts before pending starts.");

                if (event.getEnd() < pending.getStart()) {
                    logd("This event is entirely before pending event.");
                    pending.setEndTime(event.getEnd());
                    pending.setEndId(event.getEndId());
                } else if (event.getEnd() < pending.getEnd()) {
                    logd("This event ends before pending ends.");
                    // keep pending end time and end id
                } else {
                    logd("This event ends after pending ends (entirely encompasses it).");
                    pending.setEndTime(event.getEnd());
                    pending.setEndId(event.getEndId());
                }

                // Set start time to this event and update title
                // so that notifications show title of earlier event
                pending.setStartId(event.getStartId());
                pending.setStartTime(event.getStart());
                pending.setTitle(event.getTitle());
                continue;
            }

            if (event.getStart() < pending.getEnd()) {
                logd("Found event that starts before pending ends.");
                if (event.getEnd() > pending.getEnd()) {
                    logd("This event ends after pending.");
                    pending.setEndTime(event.getEnd());
                    pending.setEndId(event.getEndId());
                }
                continue;
            }
        }

        // addresses github issue #1740
        // problems when meeting start and end are same
        if (pending.getEnd() < (pending.getStart() + 30*1000)) {
            Logger.d("Event is less than 30s long; adding 30s to pending end.");
            pending.setEndTime(pending.getEnd() + 30*1000);
        }

        Logger.d("SCHEDULE: current time = " + mDateFormat.format(System.currentTimeMillis()) +
                ", start = " + mDateFormat.format(pending.getStart()) +
                ", startId = " + pending.getStartId() +
                ", end = " + mDateFormat.format(pending.getEnd()) +
                ", endId = " + pending.getEndId() +
                ", title = " + pending.getTitle());

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // get old schedules
        long scheduledStartId = PrefsHelper.getPrefLong(context, PREF_CURRENT_START_ID, -1);
        long scheduledStartTime = PrefsHelper.getPrefLong(context, PREF_CURRENT_START, -1);
        long scheduledEndId = PrefsHelper.getPrefLong(context, PREF_CURRENT_END_ID, -1);
        long scheduledEndTime = PrefsHelper.getPrefLong(context, PREF_CURRENT_END, -1);

        Logger.d("scheduledStartId = " + scheduledStartId + ", scheduledStartTime = " + scheduledStartTime +
                ", scheduledEndId = " + scheduledEndId + ", scheduledEndTime = " + scheduledEndTime);


        // set prefs to new schedules
        PrefsHelper.setPrefLong(context, PREF_CURRENT_START_ID, pending.getStartId());
        PrefsHelper.setPrefLong(context, PREF_CURRENT_END_ID, pending.getEndId());
        PrefsHelper.setPrefLong(context, PREF_CURRENT_END, pending.getEnd());
        PrefsHelper.setPrefLong(context, PREF_CURRENT_START, pending.getStart());
        PrefsHelper.setPrefString(context, PREF_CURRENT_MEETING_NAME, pending.getTitle());


        if (pending.getStartId() == -1) {
            Logger.d("No outstanding events.");
            if (scheduledStartId != -1) {
                Logger.d("Cancelling final outstanding start event " + scheduledStartId);
                manager.cancel(buildPendingIntent(context, scheduledStartId, Operation.START));

                if (scheduledStartTime > System.currentTimeMillis()) {
                    // Only cancel outstanding end if this event has not yet started
                    Logger.d("Cancelling final outstanding end event " + scheduledEndId);
                    manager.cancel(buildPendingIntent(context, scheduledEndId, Operation.STOP));
                } else {
                    Logger.d("Not cancelling final outstanding end event " + scheduledEndId);
                }
            }
            return;
        }


        // cancel old start
        if (scheduledStartId != -1) {
            Logger.d("Cancelling outstanding start event " + scheduledStartId);
            manager.cancel(buildPendingIntent(context, scheduledStartId, Operation.START));
        }
        // Schedule new start
        long early_start = 0;
        try {
            early_start = Long.parseLong(getPreferencesMap().get(AgentPreferences.MEETING_START_EARLY_M)) * 60 * 1000;
        } catch (Exception ignored) {

        }

        long adjustedStart = pending.getStart() - early_start;

        if (adjustedStart >= System.currentTimeMillis() || (adjustedStart > PrefsHelper.getPrefLong(context, PREF_MEETING_SKIPPED_AT, -1))) {
            Logger.d("Scheduling start for " + pending.getStartId() + " at " + adjustedStart + ", " + UserCalendarUtils.sdf.format(new Date(adjustedStart)));
            manager.set(AlarmManager.RTC_WAKEUP, adjustedStart,
                    buildPendingIntent(context, pending.getStartId(), Operation.START));
        } else {
            Logger.d("Not scheduling past-time start for " + pending.getStartId() + " at " + adjustedStart + ", " + UserCalendarUtils.sdf.format(new Date(adjustedStart)));
        }


        if (scheduledEndId != -1) {
            // We have an outstanding END time.  This end can occur:
            // 1.) Prior to the start of our new pending event
            // 2.) During our pending event
            // 3.) After our pending event
            // In cases 2 and 3 it should be deleted.
            if (scheduledEndTime >= pending.getStart()) {
                // We know that our current outstanding END exists either during of after this meeting
                // As it wasn't picked up when building the composite event we can safely delete
                // as it has been removed or the event associated with it changed to start after
                // this event ends
                Logger.d("Cancelling outstanding end event " + scheduledEndId);
                manager.cancel(buildPendingIntent(context, scheduledEndId, Operation.STOP));
            } else {
                Logger.d("Not cancelling pending end " + scheduledEndId);
            }
        }

        Logger.d("Scheduling end for   " + pending.getStartId() + " at " + pending.getEnd() + ", " + UserCalendarUtils.sdf.format(new Date(pending.getEnd())));
        manager.set(AlarmManager.RTC_WAKEUP,
                pending.getEnd(),
                buildPendingIntent(context, pending.getStartId(), Operation.STOP));
    }

    private final String 	PREFIX_START   	= "91";
    private final String 	PREFIX_END     	= "92";
    private final String	PREFIX_SCAN		= "93";

    private enum Operation {START, STOP, SCAN}

    private PendingIntent buildPendingIntent(Context context, long id, final Operation operation) {
        Intent intent = new Intent(context, MeetingAgentIntentService.class);
        intent.putExtra(MeetingAgentIntentService.EXTRA_MEETING_ID, id);
        String prefix = "";

        switch (operation) {
            case START:
                intent.putExtra(MeetingAgentIntentService.EXTRA_ACTION, MeetingAgentIntentService.ACTION_TRIGGER);
                prefix = PREFIX_START;
                break;
            case STOP:
                intent.putExtra(MeetingAgentIntentService.EXTRA_ACTION, MeetingAgentIntentService.ACTION_UNTRIGGER);
                prefix = PREFIX_END;
                break;
            case SCAN:
                intent.putExtra(MeetingAgentIntentService.EXTRA_ACTION, MeetingAgentIntentService.ACTION_SCAN);
                prefix = PREFIX_SCAN;
                break;
        }

        long longRequestId = Long.parseLong(prefix + String.valueOf(id));
        int requestId = (int) (longRequestId % ((long) Integer.MAX_VALUE));

        logi(operation + " request ID is " + requestId);
        return PendingIntent.getService(context, requestId, intent, PendingIntent.FLAG_ONE_SHOT);
    }



    public HashMap<String, String> updatePreferenceNoUpdateAlarms(String prefName, String prefVal) {
        HashMap<String, String> rv = super.updatePreference(prefName, prefVal);
        return rv;
    }

    @Override
    public HashMap<String, String> updatePreference(String prefName, String prefVal) {
        HashMap<String, String> rv = updatePreferenceNoUpdateAlarms(prefName, prefVal);
        updateScheduledAlarms(mContext);
        return rv;
    }


    public boolean isAllowedStartTime(long eventStartMillis) {
        HashMap<String, String> prefs = getPreferencesMap();

        // calculate allowed days
        String allowedDaysPref = prefs.get(AgentPreferences.DAYS_OF_WEEK);
        if ((allowedDaysPref == null) || (allowedDaysPref.isEmpty())) {
            Logger.e("Empty allowed days.");
            return false;
        }

        ArrayList<Integer> allowedDays = new ArrayList<Integer>(5);
        for (String s:allowedDaysPref.split(",")) {
            allowedDays.add(Integer.parseInt(s));
        }

        // check if in allowed days
        Calendar eventCal = Calendar.getInstance();
        eventCal.setTimeInMillis(eventStartMillis);
        if (!(allowedDays.contains(eventCal.get(Calendar.DAY_OF_WEEK)))) {
            Logger.d("Not an allowed item_multiselect_selected.");
            return false;
        }

        String userStart = prefs.get(AgentPreferences.TIME_START_TRIGGER);
        String userEnd = prefs.get(AgentPreferences.TIME_END_TRIGGER);

        Calendar userStartCal = Utils.getCalendarInstanceFromTimeString(userStart);
        Calendar userEndCal = Utils.getCalendarInstanceFromTimeString(userEnd);
        eventCal.set(userStartCal.get(Calendar.YEAR), userStartCal.get(Calendar.MONTH), userStartCal.get(Calendar.DAY_OF_MONTH));

        long userStartMillis = userStartCal.getTimeInMillis();
        long userEndMillis = userEndCal.getTimeInMillis();
        long eventTodayMillis = eventCal.getTimeInMillis();

        boolean during = false;

        if (userEndMillis >= userStartMillis) {
            during = (((eventTodayMillis + 1000) >= userStartMillis) && ((eventTodayMillis - 1000) < userEndMillis));
        } else {
            during = (((eventTodayMillis + 1000) >= userStartMillis) || ((eventTodayMillis - 1000) <= userEndMillis));
        }

        Logger.d("U: " + userStart + "-" + userEnd + " (" + userStartMillis + "-" + userEndMillis + ")" +
                " vs E: " + mDateFormat.format(eventStartMillis) + " (" + eventStartMillis + "," + eventTodayMillis + "), D:" + during);

        return during;
    }


    @Override
    public String getEnabledStatusMessage() {
        return mContext.getResources().getString(R.string.agent_status_bar_enabled_meeting_agent);
    }



    private String getCurrentMeetingName() {
        return PrefsHelper.getPrefString(mContext, PREF_CURRENT_MEETING_NAME, null);
    }

    @Override
    public String getStartedStatusMessage() {
        if (mTriggeredBy == Constants.TRIGGER_TYPE_MANUAL) {
            return mContext.getResources().getString(R.string.agent_status_bar_started_manual);
        }
        return mContext.getResources().getString(R.string.agent_status_bar_started_meeting_agent);
    }

    @Override
    public String getPausedStatusMessage() {
        return mContext.getResources().getString(R.string.agent_status_bar_paused_meeting_agent);
    }






    protected static void logi(String message) {
        Logger.i("MeetingAgent: " + message);
    }

    protected static void logd(String message) {
        Logger.d("MeetingAgent: " + message);
    }

    protected static void loge(String message, Exception e) {
        Logger.e("MeetingAgent: " + message, e);
    }


    @Override
    public String[] getNotifActionLines(Context context, int triggerType, boolean unpause) {
        ArrayList<String> actions = new ArrayList<String>();
        HashMap<String, String> prefs = getPreferencesMap();

        if (!unpause && (triggerType != Constants.TRIGGER_TYPE_MANUAL)) {
            String meetingName = getCurrentMeetingName();
            if ((meetingName != null) && (!meetingName.trim().isEmpty())) {
                actions.add(String.format(mContext.getResources().getString(R.string.agent_activated_meeting_name), meetingName));
            }
        }

        boolean silence = Boolean.parseBoolean(prefs.get(AgentPreferences.SOUND_SILENCE_DEVICE));
        if(silence) actions.add(context.getResources().getString(R.string.sleep_agent_silence));

        return actions.toArray(new String[actions.size()]);
    }

    @Override
    public String getNotifStartTitle(int triggerType, boolean unpause) {
        int title_res;
        if(unpause || Constants.TRIGGER_TYPE_MANUAL == triggerType) {
            title_res = R.string.agent_started_title_meeting_manual;
        } else {
            title_res = R.string.agent_started_title_meeting;
        }

        return mContext.getResources().getString(title_res);
    }


    @Override
    public String getNotifStartMessage(int triggerType, boolean unpause) {
        if (unpause || (triggerType == Constants.TRIGGER_TYPE_MANUAL)) {
            return String.format(mContext.getResources().getString(R.string.agent_activated), getName());
        }

        String meetingName = getCurrentMeetingName();

        if ((meetingName == null) || (meetingName.equals("")))
            return mContext.getResources().getString(R.string.agent_activated_meeting_name_unknown);
        else
            return String.format(mContext.getResources().getString(R.string.agent_activated_meeting), meetingName);
    }

    @Override
    public String getNotifFirstStartTitle(int triggerType, boolean unpause) {
        return getNotifStartTitle(triggerType, unpause);
    }

    @Override
    public String getNotifFirstStartMessage(int triggerType, boolean unpause) {

        int res = R.string.first_notification_meeting;

        return mContext.getResources().getString(res);
    }

    @Override
    public String getNotifFirstStartDialogDescription() {
        return mContext.getResources().getString(R.string.meeting_agent_description_first);
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
        return mContext.getResources().getString(R.string.agent_delayed_title_meeting);
    }

    @Override
    public String getNotifDelayMessage(int triggerType) {
        return mContext.getResources().getString(R.string.agent_delayed_message_meeting);
    }

    @Override
    public boolean needsUIPause() { return true;}


    @Override
    public void skip(Context context, int triggerType) {
        super.skip(context, triggerType);

        long now = System.currentTimeMillis();
        logd("Setting skipped at to: " + now);
        PrefsHelper.setPrefLong(context, PREF_MEETING_SKIPPED_AT, now);
        // clear backend pause, so nothing shows in UI
        // next meeting will correctly trigger
        setInactive(context, getGuid(), mLastTriggeredBy, false);
    }

}
