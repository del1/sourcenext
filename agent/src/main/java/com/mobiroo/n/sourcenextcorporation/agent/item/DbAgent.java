package com.mobiroo.n.sourcenextcorporation.agent.item;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.action.BaseAction;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.NotificationFactory;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.action.SettingsButler;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentBluetoothSetting;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentPermission;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentUIElement;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentNotificationInterface;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.AgentStatusChangedReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPrefDumper;
import com.mobiroo.n.sourcenextcorporation.agent.util.BluetoothWrapper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage.Events;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage.Properties;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.widget.AgentToggleWidgetProvider;

import java.util.ArrayList;
import java.util.HashMap;


public abstract class DbAgent implements Agent {

	public static final long DEFAULT_MAX_PAUSED_TIME = 23 * AlarmManager.INTERVAL_HOUR;
	public static final long DEFAULT_MIN_PAUSED_TIME = 0;

	protected int mId;
	protected String mGuid;

	protected String mName;
	protected String mDescription;
	protected String mStaticClass;

	protected int mIconId;
	protected int mColorIconId;
	protected int mWhiteIconId;

	protected int mPriority;
	protected int mVersion;

	protected long mInstalledAt;
	protected long mTriggeredAt;
	protected int mTriggeredBy;

	protected long mLastTriggeredAt;
	protected int mLastTriggeredBy;

	protected long mPausedAt;

	protected String mPreferences;

	protected Context mContext;


	protected void syncFieldsFromCursor(Context context, Cursor cur) {
		mId = cur.getInt(0);
		mGuid = cur.getString(1);

		mPriority = cur.getInt(2);
		mVersion = cur.getInt(3);
		mInstalledAt = cur.getLong(4);

		mName = cur.getString(5);
		if (mName.equals("")) {
			mName = context.getResources().getString(R.string.unknown);
		}

		mDescription = cur.getString(6);
		if (mDescription.equals("")) {
			mDescription = context.getResources().getString(R.string.unknown);
		}

		mIconId = R.drawable.ic_launcher;
		mWhiteIconId = R.drawable.ic_notification_agent;
		mStaticClass = cur.getString(8);
		mPreferences = cur.getString(9);

		mTriggeredAt = cur.getLong(10);
		mTriggeredBy = cur.getInt(11);

		mLastTriggeredAt = cur.getLong(12);
		mLastTriggeredBy = cur.getInt(13);

		mPausedAt = cur.getLong(14);
	}

	public int getId() {
		return mId;
	}
	public String getGuid() {
		return mGuid;
	}
	public String getName() {
		return mName;
	}
	public String getDescription() {
		return mDescription;
	}
	public String getLongDescription() {
		return mDescription;
	}

	public int getIconId() {
		return mIconId;
	}
	public int getPriority() {
		return mPriority;
	}
	public int getVersion() {
		return mVersion;
	}
	public String getStaticClass() {
		return mStaticClass;
	}
	public long getInstalledAt() {
		return mInstalledAt;
	}
	public long getTriggeredAt() {
		return mTriggeredAt;
	}
	public int getTriggeredBy() {
		return mTriggeredBy;
	}
	public long getPausedAt() {
		return mPausedAt;
	}



	public boolean isInstalled() {
		return (mInstalledAt > 0);
	}
	public boolean isActive() {
		return (mTriggeredAt > 0);
	}
	public boolean isStatic() {
		return DbAgent.isStatic(mStaticClass);
	}
    public boolean isStartable() { return true; }

	public boolean isPaused() {
		return (mPausedAt > 0);
	}

	public static boolean isStatic(String staticClassName) {
		return ((staticClassName != null) && (staticClassName.endsWith("Agent")));
	}

	public abstract Class<?> getConfigActivity();
    public abstract AgentPermission[] getTriggerArray();

    @Override
    public final boolean install(Context context, boolean silent, boolean skipCheckReceivers) {
        String oldPrefs = mPreferences;
        if (! DbAgent.installUninstallFromGuid(context, getGuid(), true, silent)) {
            return false;
        }

        syncFieldsFromDb();

        mPreferences = "";
        syncPrefsToDb();

        afterInstall(context, silent, skipCheckReceivers);
        setPreservedPrefs(oldPrefs);
        return true;
    }

    @Override
	public void afterInstall(Context context, boolean silent, boolean skipCheckReceivers) {
		return;
	}

	@Override
	public final boolean uninstall(Context context, boolean silent) {
		if (DbAgent.installUninstallFromGuid(context, getGuid(), false, silent)) {			
			// update this object's state to match db
			syncFieldsFromDb();
			afterUninstall(context, silent);
			return true;
		}
		return false;
	}

    @Override
	public void afterUninstall(Context context, boolean silent) {
		return;
	}

	private void syncFieldsFromDb() {
		Cursor cur = AgentFactory.getAgentCursorByGuid(mContext, getGuid());
		if (cur != null) {
			syncFieldsFromCursor(mContext, cur);
			cur.close();
		} else {
			Logger.e("DbAgent: unexpected null cursor!");
		}
	}


	@Override
	public boolean resetSettingsToDefault(Context context, boolean silent) {
		if (!uninstall(context, silent)) {return false;}
		mPreferences = "";
		syncPrefsToDb();
		return install(context, silent, false);
	}

	public static boolean isActive(Context context, String agentGuid) {
		Agent agent = AgentFactory.getAgentFromGuid(context, agentGuid);
		return (agent == null) ? false : agent.isActive();
	}

	public static void setActive(Context context, String agentGuid, int triggerType) {
		setActive(context, agentGuid, triggerType, false);
	}

	public static void setActive(Context context, String agentGuid, int triggerType, boolean unpause) {
		Agent agent = AgentFactory.getAgentFromGuid(context, agentGuid);
		if (agent.customActivate(triggerType)) {return;}

		if (!unpause && (triggerType != Constants.TRIGGER_TYPE_MANUAL) && !agent.havePreconditionsBeenMet()) {
			Logger.i("Not activating " + agent.getName() + 
					" because of preconditions or status (active = " + String.valueOf(agent.isActive()) + ")");
			return;
		}

		if (setActiveInactive(context, agentGuid, Constants.STATUS_ACTIVE, triggerType, unpause)) {
            Logger.d("DBAgent: successful non custom setActive for: " + agentGuid);

            triggerActions(context, agentGuid, triggerType, null);
            agent = AgentFactory.getAgentFromGuid(context, agentGuid);
            agent.afterActivate(triggerType, unpause);
        }
	}

    public void setActive(int triggerType) {
        DbAgent.setActive(mContext, getGuid(), triggerType);
    }
    public void afterActivate(int triggerType, boolean unpause) {
        return;
    }



	public static void setInactive(Context context, String agentGuid, int triggerType) {
		setInactive(context, agentGuid, triggerType, false);
	}
    public static void setInactive(Context context, String agentGuid, int triggerType, boolean pause) {
        Agent agent = AgentFactory.getAgentFromGuid(context, agentGuid);
        int oldTriggeredBy = agent.getTriggeredBy();
        if (agent.customDeactivate(triggerType)) {return;}

        if (setActiveInactive(context, agentGuid, Constants.STATUS_INACTIVE, triggerType, pause)) {
            Logger.d("DBAgent: successful non custom setInactive for: " + agentGuid);

            // on boot untrigger, make sure right actions are cancelled.
            untriggerActions(context, agentGuid, (triggerType == Constants.TRIGGER_TYPE_BOOT) ? oldTriggeredBy : triggerType, null);
            (new SettingsButler(context)).clearAgent(agentGuid);
            agent.afterDeactivate(triggerType, pause);
        }
    }
    public void setInactive(int triggerType) {
        DbAgent.setInactive(mContext, getGuid(), triggerType);
    }

    @Override
    public void afterDeactivate(int triggerType, boolean unpause) {
        return;
    }


	public static void triggerActions(Context context, String agentGuid, int triggerType, Object extraInfo) {
		triggerUntriggerActions(context, agentGuid, triggerType, extraInfo, true);
	}
	public static void untriggerActions(Context context, String agentGuid, int triggerType, Object extraInfo) {
		triggerUntriggerActions(context, agentGuid, triggerType, extraInfo, false);
	}


	// should not use this unless you know what you're doing with it
	public String getRawPreferences() {
		return (mPreferences == null) ? "" : mPreferences;
	}
	public HashMap<String, String> getPreferencesMap() {
		return getPreferencesMapFromString(mPreferences);
	}
	@SuppressWarnings("unchecked")
	private HashMap<String, String> getPreferencesMapFromString(String prefs) {
		if ((prefs == null) || (prefs.equals(""))) {
			return new HashMap<String, String>();
		}
		return (HashMap<String, String>)Utils.deserializeFromBase64String(prefs);
	}

	public HashMap<String, String> updatePreference(String prefName, String prefVal) {
		HashMap<String, String> prefs = getPreferencesMap();

		String curVal = prefs.get(prefName);
		if (((prefVal == null) && (curVal != null)) ||
				(prefVal != null) && (! prefVal.equals(curVal))) {
			prefs.put(AgentPreferences.AGENT_SETTINGS_CHANGED, String.valueOf(true));
		}

		prefs.put(prefName, prefVal);
		mPreferences = Utils.serializeToBase64String(prefs);
		syncPrefsToDb();
		AgentPrefDumper.dumpAgentPrefs(mContext);

		if (isPaused() && !prefName.equals(AgentPreferences.DEVICE_STILL_FOR)) {
			Logger.d("Clearing pause on settings change of pref " + prefName + " for " + getGuid());
			DbAgent.setInactive(mContext, getGuid(), getTriggeredBy());
		}

		handleGenericPrefUpdate(prefName, prefVal);
		return prefs;
	}

	protected HashMap<String, String> deletePreferenceForUpgrade(HashMap<String, String> prefs, String prefName) {
		prefs.remove(prefName);
		mPreferences = Utils.serializeToBase64String(prefs);
		syncPrefsToDb();
		return prefs;
	}

	private void syncPrefsToDb() {
		TaskDatabaseHelper.syncAgentPrefsToDb(mContext, getGuid(), mPreferences);
	}


	public SharedPreferences getSession() {
		return mContext.getSharedPreferences(AgentPreferences.AGENT_SESSION_PREFIX + mGuid, Context.MODE_PRIVATE);
	}
	public void clearSession() {
		SharedPreferences sp = mContext.getSharedPreferences(AgentPreferences.AGENT_SESSION_PREFIX + mGuid, Context.MODE_PRIVATE);
		sp.edit().clear().commit();
	}



	private void handleGenericPrefUpdate(String prefName, String prefVal) {
		if (prefName.equals(AgentPreferences.SILENCE_PHONE)) {
			SQLiteDatabase db = TaskDatabaseHelper.getInstance(mContext).getReadableDatabase();
			TaskDatabaseHelper.setActionEnabledDisabled(db, getGuid(), "PhoneSilenceAction", prefVal.equals("true"));

            boolean enabled = Boolean.parseBoolean(prefVal);
            if (enabled) {
                NotificationManager notificationManager =
                        (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && !notificationManager.isNotificationPolicyAccessGranted()) {

                    Intent intent = new Intent(android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);

                    mContext.startActivity(intent);
                }
            }
			return;
		}

        if (prefName.equals(AgentPreferences.START_ONLY_WHEN_CHARGING)) {
            Utils.checkReceivers(mContext);
            return;
        }


		if (prefName.equals(AgentPreferences.BLUETOOTH_NAME_TRIGGER)) {
			// TODO: activate or deactivate agent, if already connected or disconnected to Bluetooth on settings change?

			SQLiteDatabase db = TaskDatabaseHelper.getInstance(mContext).getReadableDatabase();
			ArrayList<BluetoothWrapper> networks = AgentBluetoothSetting.parseNetworkMacNames(prefVal);

			TaskDatabaseHelper.deleteTriggersOfType(db, getGuid(), Constants.TRIGGER_TYPE_BLUETOOTH);
			for (BluetoothWrapper btw: networks) {
				TaskDatabaseHelper.createTrigger(db, getGuid(), Constants.TRIGGER_TYPE_BLUETOOTH, btw.mac, null);
			}
			Utils.checkReceivers(mContext);
            if (this instanceof ActivityDetectorInterface) {
                ((ActivityDetectorInterface) this).resetActivityDetection();
            }
			return;
		}

        if (prefName.equals(AgentPreferences.USE_ACTIVITY_DETECTION)) {
            Utils.checkReceivers(mContext);
            if (this instanceof ActivityDetectorInterface) {
                ((ActivityDetectorInterface) this).resetActivityDetection();
            }
        }

		if (TextUtils.equals(prefName, AgentPreferences.NOTIFICATIONS_READ_ALOUD_VOICE_RESPONSE)) {
			boolean enabled = Boolean.parseBoolean(prefVal);
			if (enabled) {
                if (!PrefsHelper.getPrefBool(mContext, Constants.PREF_HAS_SEEN_NOTIFICATION_ACCESS, false)) {
                    ContentResolver contentResolver = mContext.getContentResolver();
                    String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
                    String packageName = mContext.getPackageName();

                    // check to see if the enabledNotificationListeners String contains our package name
                    if (enabledNotificationListeners == null || !enabledNotificationListeners.contains(packageName))
                    {
                        // in this situation we know that the user has not granted the app the Notification access permission
                        mContext.startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    }

                }
			}
		}
	}

	private static boolean installUninstallFromGuid(Context context, String guid, boolean install, boolean silent) {
		Agent agent = AgentFactory.getAgentFromGuid(context, guid);
		if (agent == null) { return false;}

		Logger.d("installUninstallFromGuid: " + guid + ", install = " + install + ", silent = " + silent);

		if (install && agent.isInstalled()) {
			Logger.d("Not installing agent because it's already installed: " + agent.getName());
			return false;
		}
		if (!install && !agent.isInstalled()) {
			Logger.d("Not uninstalling agent because it's not currently installed: " + agent.getName());
			return false;
		}

		// deactivate agent before uninstall, if agent is active or paused
		if (!install && (agent.isActive() || agent.isPaused())) {
			DbAgent.setInactive(context, agent.getGuid(), agent.getTriggeredBy());
		}

		SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();

		long installTime = install ? System.currentTimeMillis() : 0;

		ContentValues cv = new ContentValues();
		cv.put(TaskDatabaseHelper.FIELD_INSTALLED_AT, installTime);

		// clear out all values for sure in db
		if (! install) {
			cv.put(TaskDatabaseHelper.FIELD_PAUSED_AT, 0);
			cv.put(TaskDatabaseHelper.FIELD_TRIGGERED_AT, 0);
			cv.put(TaskDatabaseHelper.FIELD_TRIGGERED_BY, Constants.TRIGGER_TYPE_UNKNOWN);
			cv.put(TaskDatabaseHelper.FIELD_LAST_TRIGGERED_AT, 0);
			cv.put(TaskDatabaseHelper.FIELD_LAST_TRIGGERED_BY, Constants.TRIGGER_TYPE_UNKNOWN);
		}


		if (! PrefsHelper.getPrefBool(context, Constants.PREF_PRESERVE_SETTINGS_AGENT_UNINSTALL, false)) {
			cv.putNull(TaskDatabaseHelper.FIELD_PREFERENCES);   // delete existing preferences on install/uninstall
		}

		db.update(TaskDatabaseHelper.TABLE_AGENTS, cv, TaskDatabaseHelper.FIELD_GUID + " = '" + guid + "'", null);

		// remove actions and triggers
		if (! install) {
			Logger.d("installUninstallFromGuid: deleting actions and triggers");
			db.delete(TaskDatabaseHelper.TABLE_ACTIONS, TaskDatabaseHelper.FIELD_GUID + " = '" + guid + "'", null);
			db.delete(TaskDatabaseHelper.TABLE_TRIGGERS, TaskDatabaseHelper.FIELD_GUID + " = '" + guid + "'", null);
			Utils.checkReceivers(context);
		}

		if(silent) return true;

		String message = (install ? agent.getEnabledLogMessage() : agent.getDisabledLogMessage() );
		TaskDatabaseHelper.logUsageRecord(context, guid, message, null);

		if(install) {
			AgentToggleWidgetProvider.updateAgentWidgets(context, agent.getGuid());
			context.sendBroadcast(new Intent(AgentStatusChangedReceiver.ACTION_INSTALLED));
		} else {
			AgentToggleWidgetProvider.updateAgentWidgets(context, agent.getGuid());
			context.sendBroadcast(new Intent(AgentStatusChangedReceiver.ACTION_UNINSTALLED));
		}

		return true;
	}

	// returns true if change made; false if already in that state or agent not found
	private static boolean setActiveInactive(Context context, String agentGuid, String status, int triggerType, boolean pauseAction) {
		Agent agent = AgentFactory.getAgentFromGuid(context, agentGuid);
		if (agent == null) { return false;}

		// activate checks
		if (status.equals(Constants.STATUS_ACTIVE)) {
			if (agent.isActive()) {
				Logger.i("No need to activate agent " + agent.getName() + " because it's already activated.");
				return false;
			}
			if (agent.isPaused() && !pauseAction) {
				if ((System.currentTimeMillis() - agent.getPausedAt()) > agent.getMaxPausedTime()) {
					Logger.i("Agent " + agent.getName() + " is over max paused time.  Clearing pause.");
					DbAgent.setInactive(context, agentGuid, agent.getTriggeredBy());
				} else {
					Logger.i("Not activating agent " + agent.getName() + " because it's been paused.");
					return false;
				}
			}
			if (pauseAction && !agent.isPaused()) {
				Logger.i("Not unpausing agent " + agent.getName() + " because it's not currently paused.");
				return false;
			}
		} 

		// auto unpause check
		// non-pause deactivate of an agent when it's paused = auto unpause
		// just clear out paused_at and return false so deactivate actions don't trigger
		if (status.equals(Constants.STATUS_INACTIVE) && !pauseAction && agent.isPaused()) {
			if ((System.currentTimeMillis() - agent.getPausedAt()) < agent.getMinPausedTime()) {
				Logger.i("Agent " + agent.getName() + " is under min paused time.  Not clearing pause.");
				return false;
			}

			Logger.i("Autodeactivating a paused agent: " + agent.getName() + " clearing out paused flag so it'll activate next time");

			SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();
			ContentValues cv = new ContentValues();
			cv.put(TaskDatabaseHelper.FIELD_PAUSED_AT, 0);
			db.update(TaskDatabaseHelper.TABLE_AGENTS, cv, TaskDatabaseHelper.FIELD_GUID + " = '" + agentGuid + "'", null);

            // the agent has changed from paused to stop -- a new inactive broadcast must be sent.
            context.sendBroadcast(new Intent(AgentStatusChangedReceiver.ACTION_INACTIVE));

			// state change from inactive(paused) -> inactive(enabled) 
			AgentToggleWidgetProvider.updateAgentWidgets(context, agent.getGuid());

            // dismiss any outstanding notification
            NotificationFactory.dismissMain(context, agent.getGuid());

            return false;
		}

		// deactivate checks
		// this will also prevent pausing an already deactivated or paused agent
		if (status.equals(Constants.STATUS_INACTIVE)) {
			if (!agent.isActive()) {
				Logger.i("No need to deactivate agent " + agent.getName() + " because it's already deactivated.");
				return false;
			}

			if ((agent.getTriggeredBy() != triggerType) && (triggerType != Constants.TRIGGER_TYPE_BOOT)) {
				Logger.i("Not deactivating agent because untrigger doesn't match trigger: " + 
						String.valueOf(triggerType) + " vs " + String.valueOf(agent.getTriggeredBy()));
				return false;
			}
		}

		SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();

        long ranForMillis = System.currentTimeMillis() - agent.getTriggeredAt();

		long newActivateTime = (status.equals(Constants.STATUS_ACTIVE)) ? System.currentTimeMillis() : 0;
		int newTriggerType = (status.equals(Constants.STATUS_ACTIVE)) ? triggerType : Constants.TRIGGER_TYPE_UNKNOWN;

		ContentValues cv = new ContentValues();
		// put virtual values
		cv.put(TaskDatabaseHelper.FIELD_TRIGGERED_AT, newActivateTime);
		cv.put(TaskDatabaseHelper.FIELD_TRIGGERED_BY, newTriggerType);

		// put actual values
		cv.put(TaskDatabaseHelper.FIELD_LAST_TRIGGERED_AT, System.currentTimeMillis());
		cv.put(TaskDatabaseHelper.FIELD_LAST_TRIGGERED_BY, triggerType);

        boolean manuallyActive = (agent.getTriggeredBy() == Constants.TRIGGER_TYPE_MANUAL);
        boolean doPauseUnpause = (pauseAction && agent.needsBackendPause() && !manuallyActive);
        boolean isUserStartStop = (pauseAction || (triggerType == Constants.TRIGGER_TYPE_MANUAL));

        // pauseAction -- STATUS_ACTIVE = unpause; STATUS_INACTIVE = pause
		if (pauseAction) {
			long pauseTime = (!doPauseUnpause || status.equals(Constants.STATUS_ACTIVE)) ? 0 : System.currentTimeMillis();
			cv.put(TaskDatabaseHelper.FIELD_PAUSED_AT, pauseTime);
		}

		db.update(TaskDatabaseHelper.TABLE_AGENTS, cv, TaskDatabaseHelper.FIELD_GUID + " = '" + agentGuid + "'", null);		


		if(status.equals(Constants.STATUS_ACTIVE)) {
			AgentToggleWidgetProvider.updateAgentWidgets(context, agent.getGuid());

			String message = (isUserStartStop ? agent.getManualStopLogMessage() : agent.getStartedLogMessage(triggerType, doPauseUnpause));
			TaskDatabaseHelper.logUsageRecord(context, agentGuid, message, null);

			Usage.logEvent(context,
                    doPauseUnpause ? Events.AGENT_UNPAUSED : Events.AGENT_STARTED, true,
                    new Usage.EventProperty(Properties.AGENT_NAME, agentGuid),
                    new Usage.EventProperty(Properties.TRIGGER_NAME, Utils.getTriggerName(context, triggerType)));
			Usage.logGoogleAnalyticsAgentStarted(context, agentGuid);

            if (agent instanceof AgentNotificationInterface) {
                ((AgentNotificationInterface) agent).notifyStartAgent(context, triggerType, doPauseUnpause);
            }

			context.sendBroadcast(new Intent(AgentStatusChangedReceiver.ACTION_ACTIVE).putExtra(AgentStatusChangedReceiver.EXTRA_GUID, agent.getGuid()));
		} else {
			agent.clearSession();
			AgentToggleWidgetProvider.updateAgentWidgets(context, agent.getGuid());


			String message = (isUserStartStop ? agent.getManualStartLogMessage() : agent.getFinishedLogMessage(triggerType, ranForMillis));
			message = String.format(message, agent.getName());
			TaskDatabaseHelper.logUsageRecord(context, agentGuid, message, null);

			context.sendBroadcast(new Intent(AgentStatusChangedReceiver.ACTION_INACTIVE).putExtra(AgentStatusChangedReceiver.EXTRA_GUID, agent.getGuid()));

            if (agent instanceof AgentNotificationInterface) {
                if (doPauseUnpause) {
                    ((AgentNotificationInterface) agent).notifyPauseAgent(context, triggerType, ranForMillis);
                } else if (triggerType != Constants.TRIGGER_TYPE_MANUAL) {
                    ((AgentNotificationInterface) agent).notifyStopAgent(context, triggerType, ranForMillis);
                } else {
                    NotificationFactory.dismissMain(context, agent.getGuid());
                }
            }

			if (pauseAction) {
				Usage.logEvent(context,
                        Events.AGENT_PAUSED, true,
                        new Usage.EventProperty(Properties.AGENT_NAME, agentGuid));
			}
		}

		return true;
	}



    private static void triggerUntriggerActions(Context context, String agentGuid, int triggerType, Object extraInfo, boolean trigger) {
        SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();

        Cursor cursor = TaskDatabaseHelper.getEnabledActionsForAgentGuid(db, agentGuid, triggerType);

        Logger.i("Actions found to " + (trigger ? "trigger(" : "untrigger(") + triggerType +
                ") for agentGuid " + agentGuid + " : " + String.valueOf(cursor.getCount()));

        if (cursor.moveToFirst()) {
            do {
                if (trigger) {
                    BaseAction.getActionFromCursor(context, cursor).trigger(triggerType, extraInfo);
                } else {
                    BaseAction.getActionFromCursor(context, cursor).untrigger(triggerType,extraInfo);
                }
            } while (cursor.moveToNext());
        } else {
            Logger.i("DbAgent; no actions found to triggerUntrigger.");
        }

        return;
    }


	protected void defaultPrefs(HashMap<String, String>prefs, String prefsName, String value) {
		if (prefs.containsKey(prefsName)) {return;}
		prefs.put(prefsName, value);
	}

	protected DbAgent() {
	}

	@Override
	public void pause(Context context) {
		Logger.i("Agent", "pausing " + getGuid());
		DbAgent.setInactive(context, getGuid(), this.mTriggeredBy, true);
	}	

	@Override
	public void unPause(Context context) {
		DbAgent.setActive(context, getGuid(), this.mLastTriggeredBy, true);
	}

	@Override
	public int getWhiteIconId() {
		return mWhiteIconId;
	}




	@Override
	public int getColorIconId() {
		return getIconId();
	}

	@Override
	public boolean havePreconditionsBeenMet() {
		return true;
	}


	/** Status bar messages **/

	@Override
	public String getEnabledStatusMessage() {
		return null;
	}

	@Override
	public String getStartedStatusMessage() {
		return mContext.getResources().getString(R.string.agent_status_bar_started);
	}

    @Override
    public String getPausedStatusMessage() {
        return null;
    }

	/** Log messages **/

	@Override
	public String getStartedLogMessage(int triggerType, boolean unpause) {
		String message = mContext.getResources().getString(R.string.agent_activated);
		message = String.format(message, getName());
		return message;
	}

	@Override
	public String getEnabledLogMessage() {
		String message = mContext.getResources().getString(R.string.agent_installed);
		message = String.format(message, getName());
		return message;
	}

	@Override
	public String getDisabledLogMessage() {
		String message = mContext.getResources().getString(R.string.agent_uninstalled);
		message = String.format(message, getName());
		return message;
	}

	@Override
	public String getFinishedLogMessage(int triggerType, long ranForMillis) {
		String message = mContext.getResources().getString(R.string.agent_deactivated);
		message = String.format(message, getName());
		return message;
	}

	@Override
	public final String getManualStartLogMessage() {
		String message = mContext.getResources().getString(R.string.agent_paused);
		message = String.format(message, getName());
		return message;
	}

	@Override
	public final String getManualStopLogMessage() {
		String message = mContext.getResources().getString(R.string.agent_unpaused);
		message = String.format(message, getName());
		return message;
	}


	@Override
	public boolean settingsHaveChanged() {
		HashMap<String, String> prefs = getPreferencesMap();

		return Boolean.parseBoolean(prefs.get(AgentPreferences.AGENT_SETTINGS_CHANGED));
	}

	@Override
	public AgentUIElement[] getSettings(AgentConfigurationProvider acp) {
		return new AgentUIElement[0];
	}

	@Override
	public int getWidgetOutlineIconId() {
		return getIconId();
	}

	@Override
	public int getWidgetFillIconId() {
		return getIconId();
	}

	// returns true if handles activate by itself
	// returns false if we should go through normal setActive flow
	@Override
	public boolean customActivate(int triggerType) {
		return false;
	}

	// returns true if handles deactivate by itself
	// returns false if we should go through normal setInactive flow
	@Override
	public boolean customDeactivate(int triggerType) {
		return false;
	}




	@Override
	public boolean needsBackendPause() {
		return true;
	}

    @Override
    public boolean needsUIPause() { return false;}

	private void setPreservedPrefs(String oldPrefs) {
		if ((oldPrefs == null) || (oldPrefs.equals(""))) {return;}

		if (! PrefsHelper.getPrefBool(mContext, Constants.PREF_PRESERVE_SETTINGS_AGENT_UNINSTALL, false)) {
			return;
		}

		HashMap<String, String> oldPrefsMap = getPreferencesMapFromString(oldPrefs);
		HashMap<String, String> curPrefsMap = getPreferencesMap();
		for (String k:oldPrefsMap.keySet()) {
			String oldPrefsVal = oldPrefsMap.get(k);
			String curPrefsVal = curPrefsMap.get(k);
			if ((curPrefsVal == null) || (curPrefsVal.equals(oldPrefsVal))) {
				continue;
			}
			Logger.d("PreserveSettings: setting " + k + " to " + oldPrefsVal);
			updatePreference(k, oldPrefsVal);
		}

	}



	@Override
	public long getMaxPausedTime() {
		return DEFAULT_MAX_PAUSED_TIME;
	}
	@Override
	public long getMinPausedTime() {
		return DEFAULT_MIN_PAUSED_TIME;
	}

}
