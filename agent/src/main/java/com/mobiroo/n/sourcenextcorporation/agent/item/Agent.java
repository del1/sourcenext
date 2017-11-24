package com.mobiroo.n.sourcenextcorporation.agent.item;

import android.content.Context;
import android.content.SharedPreferences;

import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentConfigurationProvider;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentPermission;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentUIElement;

import java.util.HashMap;


// replacing OldAgent with this new interface
// see DbAgent; all agent data will be stored in sqlite
public interface Agent {

	public int getId();
	public String getGuid();

	public String getName();
	public String getDescription();
	public String getLongDescription();

	public int getIconId();
	public int getColorIconId();
	public int getWhiteIconId();

	public int getWidgetOutlineIconId();
	public int getWidgetFillIconId();

    public AgentPermission[] getTriggerArray();


	public String getEnabledStatusMessage();
	public String getStartedStatusMessage();
    public String getPausedStatusMessage();

	public String getEnabledLogMessage();
	public String getDisabledLogMessage();
	public String getStartedLogMessage(int triggerType, boolean unpause);
	public String getFinishedLogMessage(int triggerType, long ranForMillis);
	public String getManualStartLogMessage();
	public String getManualStopLogMessage();

	public int getPriority();
	public int getVersion();
	public boolean isInstalled();
	public boolean isActive();
	public boolean isPaused();
    public boolean isStartable();


    public long getInstalledAt();
	public long getTriggeredAt();
	public int getTriggeredBy();
	public long getPausedAt();
	public long getMaxPausedTime();
	public long getMinPausedTime();

	public Class<?> getConfigActivity();

	// below should be run in background, outside of UI threads
	public boolean install(Context context, boolean silent, boolean skipCheckReceivers);
	public boolean uninstall(Context context, boolean silent);
	public boolean resetSettingsToDefault(Context context, boolean silent);
	public void pause(Context context);
	public void unPause(Context context);
	public HashMap<String, String> getPreferencesMap();
	public HashMap<String, String> updatePreference(String prefName, String prefVal);
	public String getRawPreferences();


	public boolean customActivate(int triggerType);
	public boolean customDeactivate(int triggerType);

	// session data is shared preferences that get erased when agent is no longer activate
	public SharedPreferences getSession();
	public void clearSession();

	public boolean havePreconditionsBeenMet();
	public boolean needsBackendPause();
    public boolean needsUIPause();

    public void afterActivate(int triggerType, boolean unpause);
    public void afterDeactivate(int triggerType, boolean pause);
    public void afterInstall(Context context, boolean silent, boolean skipCheckReceivers);
    public void afterUninstall(Context context, boolean silent);


    public boolean settingsHaveChanged();
	public AgentUIElement[] getSettings(AgentConfigurationProvider acp);
}
