package com.mobiroo.n.sourcenextcorporation.agent.receiver;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.MeetingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentTimeRange;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.ActivityRecognitionHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;


public class AlarmReceiver extends BroadcastReceiver {
    public static final String INTENT_NAME = "com.mobiroo.n.sourcenextcorporation.agent.ALARM_RECEIVER";

    public static final String ALARM_AGENT_GUID = "alarm_agent_guid";
    public static final String ALARM_AGENT_ACTION = "alarm_agent_action";
    public static final String ALARM_AGENT_TRIGGER_TYPE = "alarm_trigger_type";

    public static final int ALARM_AGENT_DAILY = 2;
    public static final int ALARM_AGENT_ACTIVATE = 1;
    public static final int ALARM_AGENT_DEACTIVATE = 0;
    public static final int ALARM_AGENT_UNKNOWN = -1;

    public static final int SLEEP_AGENT_CHECKER_ALARM_REQ_ID = 50000;
    public static final int WIFI_CONNECTION_RETRY_REQ_ID = 50002;

    private static final long DAILY_ALARM_DELAY = 4 * AlarmManager.INTERVAL_HOUR;

    private static SimpleDateFormat sLogDateFormat = new SimpleDateFormat("MM-dd-yyyy kk:mm:ss");

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent == null) {
            Logger.d("AlarmReceiver: null intent");
            return;
        }

        String action = intent.getAction();
        Logger.i("AlarmReceiver: Got alarm for intent: " + action);

        if (action.equals(Intent.ACTION_DATE_CHANGED) || action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
            Logger.d("AlarmReceiver: Time, date, or timezone change.");
            Utils.checkReceivers(context);
            return;
        }

        if (! action.equals(INTENT_NAME)) {
            return;
        }

        int agentAction = intent.getIntExtra(ALARM_AGENT_ACTION, ALARM_AGENT_UNKNOWN);
        if (agentAction == ALARM_AGENT_DAILY) {
            handleDailyAction(context, intent);
            return;
        }

        int triggerType = intent.getIntExtra(ALARM_AGENT_TRIGGER_TYPE, Constants.TRIGGER_TYPE_TIME);

        String agentGuid = intent.getStringExtra(ALARM_AGENT_GUID);

        Logger.i("Alarm agent action: " + String.valueOf(agentAction));
        Logger.i("Alarm agent guid: " + (agentGuid == null ? "noguid" : agentGuid));
        Logger.i("Alarm agent triggerType: " + Utils.getTriggerName(context, triggerType));

        if (ALARM_AGENT_UNKNOWN == agentAction) {return;}


        if (ALARM_AGENT_ACTIVATE == agentAction) {
            Logger.i("AlarmReceiver: trigger fired for agentGuid: " + agentGuid);
            DbAgent.setActive(context, agentGuid, triggerType);
        } else {
            Logger.i("AlarmReceiver: trigger unfired for agentGuid: " + agentGuid);
            DbAgent.setInactive(context, agentGuid, triggerType);
        }

    }

    private static enum AgentTodo {NONE, ACTIVATE, DEACTIVATE}

    public static void setAllAlarms(Context context)
    {
        Logger.d("AlarmReceiver: Setting all alarms.");

        SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();
        Cursor c = TaskDatabaseHelper.getEnabledTriggersOfType(db, Constants.TRIGGER_TYPE_TIME);

        int nextRequestCode = 0;

        if (c.moveToFirst()) {
            HashMap<String, AgentTodo> todoMap = new HashMap<String, AgentTodo>();

            do {
                String agentGuid = c.getString(c.getColumnIndex(TaskDatabaseHelper.FIELD_GUID));
                String activateTime = c.getString(c.getColumnIndex(TaskDatabaseHelper.FIELD_KEY_1));
                String deactivateTime = c.getString(c.getColumnIndex(TaskDatabaseHelper.FIELD_KEY_2));

                if ((activateTime == null) || (activateTime.trim().isEmpty())) {
                    Logger.d("AlarmReceiver: null activate time; skipping.");
                    continue;
                }

                int handleTriggerResult = handleNewStyleTimeTriggers(context, agentGuid, activateTime, nextRequestCode);

                // deactivate case
                if (handleTriggerResult < 0) {
                    nextRequestCode += -1 * handleTriggerResult;

                    // don't deactivate if already there or ACTIVATE is there
                    if (!todoMap.containsKey(agentGuid)) {
                        todoMap.put(agentGuid, AgentTodo.DEACTIVATE);
                    }
                }

                //activate case
                if (handleTriggerResult > 0) {
                    nextRequestCode += handleTriggerResult;
                    // activate overrules all
                    todoMap.put(agentGuid, AgentTodo.ACTIVATE);
                }
            } while (c.moveToNext());

            for (String agentGuid:todoMap.keySet()) {
                if (todoMap.get(agentGuid) == AgentTodo.ACTIVATE) {
                    DbAgent.setActive(context, agentGuid, Constants.TRIGGER_TYPE_TIME);
                }
                if (todoMap.get(agentGuid) == AgentTodo.DEACTIVATE) {
                    DbAgent.setInactive(context, agentGuid, Constants.TRIGGER_TYPE_TIME);
                }
            }
        }
        c.close();


        // run daily alarm every day at 3:00PM
        // get correct 3:00PM, either today if morning, or tomorrow if past
        Calendar dailyCal = Calendar.getInstance();
        if (dailyCal.get(Calendar.HOUR_OF_DAY) >= 15) {
            dailyCal.add(Calendar.DAY_OF_YEAR, 1);
        }
        dailyCal.set(Calendar.HOUR_OF_DAY, 15);
        dailyCal.set(Calendar.MINUTE, 0);
        dailyCal.set(Calendar.SECOND, 0);
        dailyCal.set(Calendar.MILLISECOND, 0);

        setRepeatingDailyAlarmAtTime(context, "",
                dailyCal.getTimeInMillis(),
                ALARM_AGENT_DAILY, nextRequestCode);
        Logger.d("Set daily alarm for " + sLogDateFormat.format(dailyCal.getTimeInMillis()) + ", requestCode =" + nextRequestCode);
        nextRequestCode += 1;

        Editor editor = context.getSharedPreferences(AgentPreferences.OTHER_PREFS_FILE, 0).edit();
        editor.putInt(AgentPreferences.AGENT_MAX_ALARM_REQUEST_CODE, nextRequestCode-1);
        editor.commit();
    }

    public static void cancelAllAlarms(Context context) {
        cancelAllAlarms(context, true);
    }

    private static void cancelAllAlarms(Context context, boolean includingSleepChecker)
    {
        Logger.d("AlarmReceiver: Cancelling all alarms.");
        int maxRequestCode = context.getSharedPreferences(AgentPreferences.OTHER_PREFS_FILE, 0).
                getInt(AgentPreferences.AGENT_MAX_ALARM_REQUEST_CODE, 0);

        for (int i=0; i <= maxRequestCode; i++) {
            cancelAlarm(context, i);
        }

        if (includingSleepChecker) {
            cancelAlarm(context, SLEEP_AGENT_CHECKER_ALARM_REQ_ID);
        }
    }


    public static void setAlarmAtTime(Context context, String agentGuid, long timeMillis, int agentAction, int requestCode, int triggerType) {
        PendingIntent pendingIntent = getAgentActionPendingIntent(context, agentGuid, agentAction, requestCode, triggerType);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent);
    }

    private static void cancelAlarm(Context context, int requestCode) {
        PendingIntent pendingIntent = getAgentActionPendingIntent(context, "null", ALARM_AGENT_UNKNOWN, requestCode, Constants.TRIGGER_TYPE_TIME);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
    }

    private static void setRepeatingDailyAlarmAtTime(Context context, String agentGuid, long timeMillis, int agentAction, int requestCode) {
        PendingIntent pendingIntent = getAgentActionPendingIntent(context, agentGuid, agentAction, requestCode, Constants.TRIGGER_TYPE_TIME);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, timeMillis, AlarmManager.INTERVAL_DAY, pendingIntent);
    }



    private static PendingIntent getAgentActionPendingIntent(Context context, String agentGuid, int agentAction, int requestCode, int triggerType) {
        Intent intent = new Intent(INTENT_NAME);
        intent.setAction(INTENT_NAME);
        intent.putExtra(ALARM_AGENT_GUID, agentGuid);
        intent.putExtra(ALARM_AGENT_ACTION, agentAction);
        intent.putExtra(ALARM_AGENT_TRIGGER_TYPE, triggerType);
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void handleDailyAction(Context context, Intent intent) {
        Logger.d("Daily action");
        // do any daily cleanups, checks, or analysis here

        try {
            // probably unnecessary but to be safe
            ActivityRecognitionHelper.startActivityRecognitionIfNeeded(context);
            MeetingAgent agent = (MeetingAgent) AgentFactory.getAgentFromGuid(context, MeetingAgent.HARDCODED_GUID);
            agent.updateScheduledAlarms(context);

            cancelAllAlarms(context, false);
            setAllAlarms(context);
        } catch (Exception e) {
            Logger.d("handleDailyAction error: " + e.toString());
        }
    }



    private static int handleNewStyleTimeTriggers(Context context, String agentGuid,
                                                  String activateTime, int nextRequestCode) {

        AgentTimeRange atr = new AgentTimeRange();
        if (!atr.deserialize(activateTime)) {
            return 0;
        }

        Calendar[] calsToSet = atr.getNextStartEndCalendar();

        String logStr = "Set activate[" + nextRequestCode + "]=" + sLogDateFormat.format(calsToSet[0].getTimeInMillis());

        setAlarmAtTime(context, agentGuid, calsToSet[0].getTimeInMillis(), ALARM_AGENT_ACTIVATE, nextRequestCode, Constants.TRIGGER_TYPE_TIME);
        nextRequestCode += 1;

        logStr += ", deactivate[" + nextRequestCode + "]=" + sLogDateFormat.format(calsToSet[1].getTimeInMillis());
        setAlarmAtTime(context, agentGuid, calsToSet[1].getTimeInMillis(), ALARM_AGENT_DEACTIVATE, nextRequestCode, Constants.TRIGGER_TYPE_TIME);
        nextRequestCode += 1;

        Logger.i(logStr);

        if (atr.nowInRange()) {
            return 2;
        } else {
            return -2;
        }
    }


}
