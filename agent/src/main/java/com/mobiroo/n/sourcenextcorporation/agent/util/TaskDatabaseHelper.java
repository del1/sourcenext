package com.mobiroo.n.sourcenextcorporation.agent.util;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mobiroo.n.sourcenextcorporation.agent.item.BatteryAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.DriveAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.MeetingAgent;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.ParkingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.SleepAgent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;


public class TaskDatabaseHelper extends SQLiteOpenHelper {

    private static TaskDatabaseHelper instance;
    @SuppressWarnings("unused")
    private static Context mContext;
    
    public static synchronized TaskDatabaseHelper getInstance(Context context) {
        mContext = context;
        if (instance == null) {
            instance = new TaskDatabaseHelper(context);
        }
        return instance;
    }
    
    private static final int DATABASE_VERSION = 9;
    private static final String DATABASE_NAME = "AgentTasks";
    
        
    public static final String FIELD_ID = "id";
    public static final String FIELD_GUID = "guid";
    public static final String FIELD_STATIC_CLASS = "static_class";
    public static final String FIELD_ACTION_CLASS = "action_class";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_ICON = "icon";
    public static final String FIELD_PRIORITY = "priority";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_VERSION = "version";
    public static final String FIELD_INSTALLED_AT = "installed_at";
    public static final String FIELD_ACTION_NUMBER = "action_number";
    public static final String FIELD_COMMAND       = "command";
    public static final String FIELD_REASON        = "reason";
    public static final String FIELD_STATS         = "stats";
    public static final String FIELD_TIME_EXECUTED = "time_executed";
    public static final String FIELD_KEY_1 = "key1";
    public static final String FIELD_KEY_2 = "key2";
    public static final String FIELD_DATE = "date";
    public static final String FIELD_PREVIOUS_STATE = "previous_state";
    public static final String FIELD_PREFERENCES = "preferences";
    public static final String FIELD_TRIGGER_ON = "trigger_on";
    public static final String FIELD_TRIGGERED_AT = "triggered_at";
    public static final String FIELD_TRIGGERED_BY = "triggered_by";
    public static final String FIELD_LAST_TRIGGERED_AT = "last_triggered_at";
    public static final String FIELD_LAST_TRIGGERED_BY = "last_triggered_by";
    public static final String FIELD_DISABLED = "disabled";
    public static final String FIELD_PAUSED_AT = "paused_at";
    public static final String FIELD_TRIGGER_CONDITION = "condition";
    public static final String FIELD_TOTAL_ACTIONS = "TotalActions";

    
    
    /* Table names */ 
    public static final String TABLE_AGENTS        = "agents";
    public static final String TABLE_ACTIONS       = "actions";
    public static final String TABLE_TRIGGERS      = "triggers";
    public static final String TABLE_USAGE         = "usage_log";
    public static final String TABLE_STATS         = "stats";

    
    private static final String CREATE_AGENTS_TABLE = 
            "CREATE TABLE " + TABLE_AGENTS + " (" +
            FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            FIELD_GUID + " TEXT, " +
            FIELD_STATIC_CLASS + " TEXT, " +
            FIELD_NAME + " TEXT, " +
            FIELD_DESCRIPTION + " TEXT, " +
            FIELD_ICON + " TEXT, " +
            FIELD_PRIORITY + " INTEGER NOT NULL DEFAULT 0, " +
            FIELD_VERSION + " INTEGER NOT NULL DEFAULT 0, " +
            FIELD_INSTALLED_AT + " INTEGER NOT NULL DEFAULT 0, " +
            FIELD_PREFERENCES + " TEXT, " +
            FIELD_TYPE + " INTEGER, " +
            FIELD_TRIGGERED_AT + " INTEGER NOT NULL DEFAULT 0, " +
            FIELD_TRIGGERED_BY + " INTEGER NOT NULL DEFAULT " + String.valueOf(Constants.TRIGGER_TYPE_UNKNOWN) + ", " + 
            FIELD_LAST_TRIGGERED_AT + " INTEGER NOT NULL DEFAULT 0, " +
            FIELD_LAST_TRIGGERED_BY + " INTEGER NOT NULL DEFAULT " + String.valueOf(Constants.TRIGGER_TYPE_UNKNOWN) + ", " +
            FIELD_PAUSED_AT + " INTEGER NOT NULL DEFAULT 0" +
            ")";
    
    private static final String CREATE_ACTIONS_TABLE = 
            "CREATE TABLE " + TABLE_ACTIONS + " (" +
             FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
             FIELD_GUID + " TEXT, " +
             FIELD_ACTION_NUMBER + " INTEGER NOT NULL DEFAULT 0, " +
             FIELD_ACTION_CLASS + " TEXT, " +
             FIELD_COMMAND + " TEXT, " +
             FIELD_PREVIOUS_STATE + " TEXT, " +
             FIELD_TRIGGER_ON + " INTEGER NOT NULL, " + 
             FIELD_DISABLED + " INTEGER NOT NULL DEFAULT 0, " + 
             "FOREIGN KEY(" + FIELD_GUID + ") REFERENCES " + TABLE_AGENTS + "(" + FIELD_GUID + ")" +
             ")";
    
    private static final String CREATE_TRIGGERS_TABLE = 
            "CREATE TABLE " + TABLE_TRIGGERS + " (" +
             FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
             FIELD_GUID + " TEXT, " +
             FIELD_TYPE + " INTEGER NOT NULL, " +
             FIELD_KEY_1 + " TEXT, " +
             FIELD_KEY_2 + " TEXT, " +
             FIELD_DISABLED + " INTEGER NOT NULL DEFAULT 0, " +
             "FOREIGN KEY(" + FIELD_GUID + ") REFERENCES " + TABLE_AGENTS + "(" + FIELD_GUID + ")" +
             ")";
    
    private static final String CREATE_USAGE_LOG_TABLE = 
            "CREATE TABLE " + TABLE_USAGE + " (" +
            FIELD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            FIELD_GUID + " TEXT, " +
            FIELD_REASON + " TEXT, " +
            FIELD_STATS + " TEXT, " +
            FIELD_TIME_EXECUTED + " TEXT" +
            ")";
    
    private static final String CREATE_STATS_TABLE = 
            "CREATE TABLE " + TABLE_STATS + " (" + 
            FIELD_TOTAL_ACTIONS + " INTEGER, " +
            FIELD_DATE + " TEXT" +
            ")";
    

    private static void seedAgent(SQLiteDatabase db, String guid, int priority, String className) {
    	String sql = "INSERT INTO " + TABLE_AGENTS + "(" + 
    		FIELD_GUID + "," + FIELD_PRIORITY + "," +
    		FIELD_VERSION + "," + FIELD_STATIC_CLASS + ") VALUES " +
    		"('" + guid + "', "+ String.valueOf(priority) + ", 1, '" + className + "')";
    	db.execSQL(sql);    	
    }
    private static void seedAgents(SQLiteDatabase db) {
    	seedAgent(db, BatteryAgent.HARDCODED_GUID, 30000, "BatteryAgent");
    	seedAgent(db, SleepAgent.HARDCODED_GUID, 20000, "SleepAgent");
    	seedAgent(db, ParkingAgent.HARDCODED_GUID, 10500, "ParkingAgent");
    	seedAgent(db, MeetingAgent.HARDCODED_GUID, 10300, "MeetingAgent");
    	seedAgent(db, DriveAgent.HARDCODED_GUID, 10000, "DriveAgent");
    }
    

    
    
    public TaskDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Logger.i("Running " + CREATE_AGENTS_TABLE);
        db.execSQL(CREATE_AGENTS_TABLE);
        seedAgents(db);
        db.execSQL(CREATE_ACTIONS_TABLE);
        db.execSQL(CREATE_TRIGGERS_TABLE);
        db.execSQL(CREATE_USAGE_LOG_TABLE);
        db.execSQL(CREATE_STATS_TABLE);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    	Logger.d("TaskDatabaseHelper onUpgrade: checking upgrades");

    	if (oldVersion < 2) {
    		Cursor cur = TaskDatabaseHelper.getAgentByGuid(db, SleepAgent.HARDCODED_GUID);
    		if (cur.moveToFirst() && (cur.getLong(cur.getColumnIndex(FIELD_INSTALLED_AT)) > 0)) {
    			TaskDatabaseHelper.createAction(db, SleepAgent.HARDCODED_GUID, "BatteryAgentAction", Constants.TRIGGER_TYPE_TIME);
    			Logger.d("TaskDatabaseHelper onUpgrade: added BatteryAgentAction to SleepAgent");
    		}
    		cur.close();
    	}

    	if (oldVersion < 3) {
    		// change drive agent trigger from phone call to missed call trigger
    		ContentValues cv = new ContentValues();
    		cv.put(TaskDatabaseHelper.FIELD_TYPE, Constants.TRIGGER_TYPE_MISSED_CALL);
    		db.update(TaskDatabaseHelper.TABLE_TRIGGERS, cv, 
    				FIELD_GUID + " = '" + DriveAgent.HARDCODED_GUID + "' AND " + 
    						FIELD_TYPE + " = " + Constants.TRIGGER_TYPE_PHONE_CALL, null);
    		cv = new ContentValues();
    		cv.put(TaskDatabaseHelper.FIELD_TRIGGER_ON, Constants.TRIGGER_TYPE_MISSED_CALL);
    		db.update(TaskDatabaseHelper.TABLE_ACTIONS, cv, 
    				FIELD_GUID + " = '" + DriveAgent.HARDCODED_GUID + "' AND " + 
    						FIELD_TRIGGER_ON + " = " + Constants.TRIGGER_TYPE_PHONE_CALL, null);

    		Logger.d("TaskDatabaseHelper onUpgrade: replaced phone call triggers in DriveAgent");

    		// add missed call trigger and action to sleep agent
    		Cursor cur = TaskDatabaseHelper.getAgentByGuid(db, SleepAgent.HARDCODED_GUID);
    		if (cur.moveToFirst() && (cur.getLong(cur.getColumnIndex(FIELD_INSTALLED_AT)) > 0)) {
    			TaskDatabaseHelper.createTrigger(db, SleepAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_MISSED_CALL);
    			TaskDatabaseHelper.createAction(db, SleepAgent.HARDCODED_GUID, "VerifyUrgentSmsAction", Constants.TRIGGER_TYPE_MISSED_CALL);
    			Logger.d("TaskDatabaseHelper onUpgrade: added missed call triggers to SleepAgent");
    		}
    		cur.close();
    	}
    	
    	if (oldVersion < 4) {
    		Logger.d("TaskDatabaseHelper onUpgrade: replaced MeetingAgentAction with PhoneSilenceAciton");
    		ContentValues cv = new ContentValues();
    		cv.put(TaskDatabaseHelper.FIELD_ACTION_CLASS, "PhoneSilenceAction");
    		db.update(TaskDatabaseHelper.TABLE_ACTIONS, cv, 
    						FIELD_ACTION_CLASS + " = 'MeetingAgentAction'", null);
    	}

    	if (oldVersion < 5) {
    		Logger.d("TaskDatabaseHelper onUpgrade: adding manual activations");
    		
    		Cursor cur = TaskDatabaseHelper.getAgentByGuid(db, BatteryAgent.HARDCODED_GUID);
    		if (cur.moveToFirst() && (cur.getLong(cur.getColumnIndex(FIELD_INSTALLED_AT)) > 0)) {
    			TaskDatabaseHelper.createAction(db, BatteryAgent.HARDCODED_GUID, "BatteryAgentAction", Constants.TRIGGER_TYPE_MANUAL);
    			Logger.d("TaskDatabaseHelper onUpgrade: added manual actions to BatteryAgent");
    		}
    		cur.close();

    		cur = TaskDatabaseHelper.getAgentByGuid(db, DriveAgent.HARDCODED_GUID);
    		if (cur.moveToFirst() && (cur.getLong(cur.getColumnIndex(FIELD_INSTALLED_AT)) > 0)) {
    			TaskDatabaseHelper.createAction(db, DriveAgent.HARDCODED_GUID, "PhoneSilenceAction", Constants.TRIGGER_TYPE_MANUAL);
    			Logger.d("TaskDatabaseHelper onUpgrade: added manual actions to DriveAgent");
    		}
    		cur.close();
    		
    		cur = TaskDatabaseHelper.getAgentByGuid(db, MeetingAgent.HARDCODED_GUID);
    		if (cur.moveToFirst() && (cur.getLong(cur.getColumnIndex(FIELD_INSTALLED_AT)) > 0)) {
    			TaskDatabaseHelper.createAction(db, MeetingAgent.HARDCODED_GUID, "PhoneSilenceAction", Constants.TRIGGER_TYPE_MANUAL);
    			Logger.d("TaskDatabaseHelper onUpgrade: added manual actions to MeetingAgent");
    		}
    		cur.close();
    		
    		cur = TaskDatabaseHelper.getAgentByGuid(db, SleepAgent.HARDCODED_GUID);
    		if (cur.moveToFirst() && (cur.getLong(cur.getColumnIndex(FIELD_INSTALLED_AT)) > 0)) {
    			TaskDatabaseHelper.createAction(db, SleepAgent.HARDCODED_GUID, "PhoneSilenceAction", Constants.TRIGGER_TYPE_MANUAL);
    			TaskDatabaseHelper.createAction(db, SleepAgent.HARDCODED_GUID, "BatteryAgentAction", Constants.TRIGGER_TYPE_MANUAL);
    			Logger.d("TaskDatabaseHelper onUpgrade: added manual actions to SleepAgent");
    		}
    		cur.close();
    		
    	}
    	
    	if (oldVersion < 6) {
    		Logger.d("Upgrading db to v6");
    		Cursor cur = TaskDatabaseHelper.getAgentByGuid(db, MeetingAgent.HARDCODED_GUID);
    		if (cur.moveToFirst() && (cur.getLong(cur.getColumnIndex(FIELD_INSTALLED_AT)) > 0)) {
    			TaskDatabaseHelper.createAction(db, MeetingAgent.HARDCODED_GUID, "AutorespondSmsAction", Constants.TRIGGER_TYPE_SMS);
    			TaskDatabaseHelper.createAction(db, MeetingAgent.HARDCODED_GUID, "AutorespondPhoneCallAction", Constants.TRIGGER_TYPE_PHONE_CALL);
    			TaskDatabaseHelper.createAction(db, MeetingAgent.HARDCODED_GUID, "VerifyUrgentSmsAction", Constants.TRIGGER_TYPE_MISSED_CALL);

    			TaskDatabaseHelper.createTrigger(db, MeetingAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_MISSED_CALL);
    			TaskDatabaseHelper.createTrigger(db, MeetingAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_SMS);
    			TaskDatabaseHelper.createTrigger(db, MeetingAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_PHONE_CALL);
    			Logger.d("TaskDatabaseHelper onUpgrade: added phone call triggers to MeetingAgent");
    		}
    		cur.close();
    	}
    	
    	if (oldVersion < 7) {
    		Logger.d("Upgrading db to v7");
    		
    		Cursor cur = TaskDatabaseHelper.getAgentByGuid(db, DriveAgent.HARDCODED_GUID);
    		if (cur.moveToFirst() && (cur.getLong(cur.getColumnIndex(FIELD_INSTALLED_AT)) > 0)) {
    			TaskDatabaseHelper.createAction(db, DriveAgent.HARDCODED_GUID, "PhoneSilenceAction", Constants.TRIGGER_TYPE_DRIVING);
    			Logger.d("TaskDatabaseHelper onUpgrade: added driving actions to DriveAgent");
    		}
    		cur.close();
    	}

        String STATUS_AGENT_GUID = "tryagent.status";
        if (oldVersion < 8) {
            Logger.d("Upgrading db to v8");
            seedAgent(db, STATUS_AGENT_GUID, 5000, "StatusAgent");
        }

        if (oldVersion < 9) {
            Logger.d("Upgrading db to v9");
            db.execSQL("DELETE FROM " + TaskDatabaseHelper.TABLE_AGENTS + " WHERE " + TaskDatabaseHelper.FIELD_GUID + " = '" + STATUS_AGENT_GUID + "'");
            db.execSQL("DELETE FROM " + TaskDatabaseHelper.TABLE_TRIGGERS + " WHERE " + TaskDatabaseHelper.FIELD_GUID + " = '" + STATUS_AGENT_GUID + "'");
            db.execSQL("DELETE FROM " + TaskDatabaseHelper.TABLE_ACTIONS + " WHERE " + TaskDatabaseHelper.FIELD_GUID + " = '" + STATUS_AGENT_GUID + "'");
        }
    	
    }

 
    
    /*
     * Helper methods for retrieving specific agent info
     */
    
    public static final String[] FIELDS_TRIGGERS = {
        FIELD_ID,
        FIELD_GUID,
        FIELD_TYPE,
        FIELD_KEY_1,
        FIELD_KEY_2,
        FIELD_DISABLED
    };
    
    public static final String[] FIELDS_ACTIONS = {
      FIELD_ID,
      FIELD_GUID,
      FIELD_ACTION_NUMBER,
      FIELD_ACTION_CLASS,
      FIELD_COMMAND,
      FIELD_PREVIOUS_STATE,
      FIELD_TRIGGER_ON,
      FIELD_DISABLED
    };
    
    public static final String[] FIELDS_AGENTS = {
      FIELD_ID,
      FIELD_GUID,
      FIELD_STATIC_CLASS,
      FIELD_NAME,
      FIELD_DESCRIPTION,
      FIELD_ICON,
      FIELD_PRIORITY,
      FIELD_VERSION,
      FIELD_INSTALLED_AT,
      FIELD_PREFERENCES,
      FIELD_TYPE,
      FIELD_TRIGGERED_AT,
      FIELD_TRIGGERED_BY,
      FIELD_LAST_TRIGGERED_AT,
      FIELD_LAST_TRIGGERED_BY,
      FIELD_PAUSED_AT
    };
    
    public static Cursor getAllAgents(SQLiteDatabase db, int limit) {
    	return db.rawQuery(getAgentsSql(limit, null, null, null), null);
    }
    
    public static Cursor getInstalledAgents(SQLiteDatabase db, int limit) {
    	return db.rawQuery(getAgentsSql(limit, null, 1, null), null);    	
    }
	
	public static Cursor getAgentByGuid(SQLiteDatabase db, String guid) {
		return db.rawQuery(getAgentsSql(1, guid, null, null), null);
	}
	
	protected static String getAgentsSql(int limit, String guid, Integer installedBit, String orderBy) {
		String whereStr = " WHERE 1=1 AND " + TaskDatabaseHelper.FIELD_GUID + " <> 'tryagent.status' ";  //TODO: remove this check
		if (guid != null) {
			whereStr += " AND " + TaskDatabaseHelper.FIELD_GUID + " = '" + guid + "'";
		}
		if (installedBit != null) {
			String compStr = (installedBit.intValue() == 0) ? "=" : ">";
			whereStr += " AND " + TaskDatabaseHelper.FIELD_INSTALLED_AT + " " + compStr + " 0";
		}

		String orderByStr = orderBy;
		if (orderByStr == null) {
			orderByStr = TaskDatabaseHelper.FIELD_PRIORITY + " desc, " + TaskDatabaseHelper.FIELD_ID  + " desc";
		}
		
		String sql = "SELECT " + TaskDatabaseHelper.FIELD_ID + " AS _id"
		+ ", " + TaskDatabaseHelper.FIELD_GUID 
		+ ", " + TaskDatabaseHelper.FIELD_PRIORITY
		+ ", " + TaskDatabaseHelper.FIELD_VERSION
		+ ", " + TaskDatabaseHelper.FIELD_INSTALLED_AT
		+ ", ifnull(" + TaskDatabaseHelper.FIELD_NAME + ",'')"
		+ ", ifnull(" + TaskDatabaseHelper.FIELD_DESCRIPTION + ",'')"
		+ ", ifnull(" + TaskDatabaseHelper.FIELD_ICON + ",'')"
		+ ", ifnull(" + TaskDatabaseHelper.FIELD_STATIC_CLASS + ",'')"
		+ ", ifnull(" + TaskDatabaseHelper.FIELD_PREFERENCES + ",'')"
		+ ", " + TaskDatabaseHelper.FIELD_TRIGGERED_AT
		+ ", " + TaskDatabaseHelper.FIELD_TRIGGERED_BY
		+ ", " + TaskDatabaseHelper.FIELD_LAST_TRIGGERED_AT
		+ ", " + TaskDatabaseHelper.FIELD_LAST_TRIGGERED_BY
		+ ", " + TaskDatabaseHelper.FIELD_PAUSED_AT
		+ " FROM " + TaskDatabaseHelper.TABLE_AGENTS 
		+ whereStr
		+ " ORDER BY " + orderByStr
		+ " LIMIT " + Integer.toString(limit);

		return sql;
	}
    
	// only returns triggers that are not disabled
	public static Cursor getEnabledTriggersOfType(SQLiteDatabase db, int triggerType) {
		return db.query(TABLE_TRIGGERS, FIELDS_TRIGGERS, FIELD_TYPE + "= ? AND " + FIELD_DISABLED + " = 0", new String[] { String.valueOf(triggerType)}, null, null, null);
	}

		
	// only returns actions that are not disabled
    public static Cursor getEnabledActionsForAgentGuid(SQLiteDatabase db, String agentGuid, int triggerType) {
    	return db.query(TABLE_ACTIONS, FIELDS_ACTIONS, FIELD_GUID + " = '" + agentGuid + "' AND " + FIELD_TRIGGER_ON + " = " + String.valueOf(triggerType) + " AND " + FIELD_DISABLED + " = 0", null, null, null, FIELD_ACTION_NUMBER);
    }

    public static Cursor getAllTriggersEnabledActionsForAgentGuid(SQLiteDatabase db, String agentGuid) {
    	return db.query(TABLE_ACTIONS, FIELDS_ACTIONS, FIELD_GUID + " = '" + agentGuid + "' AND " + FIELD_DISABLED + " = 0", null, null, null, FIELD_ACTION_NUMBER);
    }

    
    
    public static void logUsageRecord(Context context, String agentGuid, String reason, String optionalStats) {
    	SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();
    	logUsageRecord(db, agentGuid, reason, optionalStats);
    }
    
    public static void logUsageRecord(SQLiteDatabase db, String agentGuid, String reason, String optionalStats) {
    	ContentValues values = new ContentValues();
        values.put(FIELD_GUID, agentGuid);
        values.put(FIELD_REASON, reason);
        values.put(FIELD_STATS, optionalStats);
        values.put(FIELD_TIME_EXECUTED, System.currentTimeMillis());
        
        db.insert(TABLE_USAGE, null, values);
    }
    
    public static void createTrigger(SQLiteDatabase db, String agentGuid, int triggerType) {
    	createTrigger(db, agentGuid, triggerType, null, null);
    }

    public static void createTrigger(SQLiteDatabase db, String agentGuid, int triggerType, String key1, String key2) {
    	ContentValues triggers = new ContentValues();
    	triggers.put(TaskDatabaseHelper.FIELD_GUID, agentGuid);
    	triggers.put(TaskDatabaseHelper.FIELD_TYPE, triggerType);
    	if (key1 != null) {
    		triggers.put(TaskDatabaseHelper.FIELD_KEY_1, key1);
    	}
    	if (key2 != null) {
    		triggers.put(TaskDatabaseHelper.FIELD_KEY_2, key2);
    	}
    	db.insertOrThrow(TaskDatabaseHelper.TABLE_TRIGGERS, null, triggers);
    }
    
    public static void deleteTriggersOfType(SQLiteDatabase db, String agentGuid, int triggerType) {
    	db.delete(TaskDatabaseHelper.TABLE_TRIGGERS, 
    			TaskDatabaseHelper.FIELD_GUID + " = '" + agentGuid + "' AND " + 
    			TaskDatabaseHelper.FIELD_TYPE + " = " + String.valueOf(triggerType), null);
    }

    // set enableDisable to true to enable; false to disable
    public static void setTriggerEnabledDisabled(SQLiteDatabase db, String agentGuid, int triggerType, boolean enableDisable) {
    	ContentValues cv = new ContentValues();
    	cv.put(TaskDatabaseHelper.FIELD_DISABLED, enableDisable ? 0 : 1);
    	db.update(TaskDatabaseHelper.TABLE_TRIGGERS, cv, TaskDatabaseHelper.FIELD_GUID + " = '" + agentGuid + "' AND " + 
    			TaskDatabaseHelper.FIELD_TYPE + " = '" + String.valueOf(triggerType) + "'", null);
    }

    public static void updateTriggerField(SQLiteDatabase db, String agentGuid, int triggerType, String fieldName, String val) {
    	updateTableField(TaskDatabaseHelper.TABLE_TRIGGERS, db, agentGuid, triggerType, fieldName, val);
    }

    public static void createAction(SQLiteDatabase db, String agentGuid, String actionClass, int triggerType) {    	
    	ContentValues actions = new ContentValues();
    	actions.put(TaskDatabaseHelper.FIELD_GUID, agentGuid);
    	actions.put(TaskDatabaseHelper.FIELD_ACTION_CLASS, actionClass);
    	actions.put(TaskDatabaseHelper.FIELD_TRIGGER_ON, triggerType);
    	db.insertOrThrow(TaskDatabaseHelper.TABLE_ACTIONS, null, actions);
    }

    // set enableDisable to true to enable; false to disable
    // will do this for all trigger types
    public static void setActionEnabledDisabled(SQLiteDatabase db, String agentGuid, String actionClass, boolean enableDisable) {
    	ContentValues cv = new ContentValues();
    	cv.put(TaskDatabaseHelper.FIELD_DISABLED, enableDisable ? 0 : 1);
    	db.update(TaskDatabaseHelper.TABLE_ACTIONS, cv, TaskDatabaseHelper.FIELD_GUID + " = '" + agentGuid + "' AND " + 
    			TaskDatabaseHelper.FIELD_ACTION_CLASS + " = '" + actionClass + "'", null);
    }

    // set enableDisable to true to enable; false to disable
    // will only do this for trigger type triggerType
    public static void setActionEnabledDisabled(SQLiteDatabase db, String agentGuid, String actionClass, int triggerType, boolean enableDisable) {
    	ContentValues cv = new ContentValues();
    	cv.put(TaskDatabaseHelper.FIELD_DISABLED, enableDisable ? 0 : 1);
    	db.update(TaskDatabaseHelper.TABLE_ACTIONS, cv, TaskDatabaseHelper.FIELD_GUID + " = '" + agentGuid + "' AND " + 
    			TaskDatabaseHelper.FIELD_TRIGGER_ON + " = '" + String.valueOf(triggerType) + "' AND" +
    			TaskDatabaseHelper.FIELD_ACTION_CLASS + " = '" + actionClass + "'", null);
    }

    public static void updateActionField(SQLiteDatabase db, String agentGuid, int triggerType, String fieldName, String val) {
    	updateTableField(TaskDatabaseHelper.TABLE_ACTIONS, db, agentGuid, triggerType, fieldName, val);
    }


    protected static void updateTableField(String table, SQLiteDatabase db, String agentGuid, int triggerType, String fieldName, String val) {
    	ContentValues cv = new ContentValues();
    	if (val == null) {
    		cv.putNull(fieldName);
    	} else {
    		cv.put(fieldName, val);
    	}
    	String triggerField = (table.equals(TaskDatabaseHelper.TABLE_TRIGGERS) ? TaskDatabaseHelper.FIELD_TYPE : TaskDatabaseHelper.FIELD_TRIGGER_ON);
    	db.update(table, cv, TaskDatabaseHelper.FIELD_GUID + " = '" + agentGuid + "' AND " + 
    			triggerField + " = '" + String.valueOf(triggerType) + "'", null);	
    }
    

    
    public static JSONObject generateDatabaseBackup(SQLiteDatabase db) {
        JSONObject backup = new JSONObject();        
        
        String[] backupTables = new String[] {TABLE_AGENTS, TABLE_ACTIONS, TABLE_TRIGGERS};
        
        for (String table: backupTables) {
            JSONArray current = new JSONArray();
            
            Cursor c = db.query(table, null, null, null, null, null, null);
            if (c.moveToFirst()) {
                do {
                    JSONObject row = new JSONObject();
                    for (int j=0; j<c.getColumnCount(); j++) {
                        try {
                        	if (table.equals(TABLE_AGENTS) && c.getColumnName(j).equals(FIELD_PREFERENCES)) {
                        		// dereference preferences and dump them
                        		String prefStr = c.getString(j);
                        		if ((prefStr == null) || (prefStr.trim().equals(""))) {continue;}
                        		@SuppressWarnings("unchecked")
								HashMap<String, String> prefs = 
                        				(HashMap<String, String>) Utils.deserializeFromBase64String(prefStr);
                        		row.put(c.getColumnName(j), prefsToJSON(prefs));
                        	} else {
                        		row.put(c.getColumnName(j), c.getString(j));
                        	}
                        } catch (JSONException e) {
                            Logger.e("Exception adding data to backup " + e, e);
                        }
                    }
                    current.put(row);
                } while (c.moveToNext());
            } else {
            	Logger.d("No rows for table: " + table);
            }
            c.close();

            try {
                backup.put(table, current);
            } catch (JSONException e) {
                Logger.e("Exception adding table to backup " + e, e);
            }
        }
        return backup;
    }
    
    // warning: will modify prefs as needed
    private static JSONObject prefsToJSON(HashMap<String, String> prefs) {    	
    	// make sure contacts isn't too long
		String contacts = prefs.remove(AgentPreferences.SMS_AUTORESPOND_CONTACTS);
		if (contacts == null) {contacts = "null";}
		if (contacts.length() > 20) { contacts = contacts.substring(0, 20) + "...";}
		prefs.put(AgentPreferences.SMS_AUTORESPOND_CONTACTS, contacts);
		
        JSONObject backup = new JSONObject();        
        for (String key: prefs.keySet()) {
        	try {
				backup.put(key, prefs.get(key));
			} catch (JSONException e) {
				Logger.d("Could not backup pref: " + key);
			}
        }
        return backup;
    }
    
	public static void syncAgentPrefsToDb(Context context, String agentGuid, String rawPreferences) {
		SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(TaskDatabaseHelper.FIELD_PREFERENCES, rawPreferences);
		db.update(TaskDatabaseHelper.TABLE_AGENTS, cv, TaskDatabaseHelper.FIELD_GUID + " = '" + agentGuid + "'", null);
	}
    
}
