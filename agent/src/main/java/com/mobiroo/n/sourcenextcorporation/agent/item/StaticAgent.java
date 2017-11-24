package com.mobiroo.n.sourcenextcorporation.agent.item;

import android.content.Context;

import com.mobiroo.n.sourcenextcorporation.agent.activity.AgentConfigurationActivity;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.elements.AgentPermission;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentDelayNotification;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentFirstStartingNotification;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentNotification;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.AgentStartingNotification;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.NotificationFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.NotificationUtils;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;


public abstract class StaticAgent extends DbAgent {
	
	protected StaticAgent() {
		super();
	}
	
	public StaticAgent(DbAgent dba) {
		setDataFromDbAgent(dba);
	}
	

	void setDataFromDbAgent(DbAgent dba) {
		mContext = dba.mContext;
		
		mId = dba.mId;
		mGuid = dba.mGuid;
		mStaticClass=dba.mStaticClass;
		mIconId = dba.mIconId;
		
		mPriority = dba.mPriority;
		mVersion = dba.mVersion;
		mInstalledAt = dba.mInstalledAt;
		mPreferences = dba.mPreferences;
		mTriggeredAt = dba.mTriggeredAt;
		mTriggeredBy = dba.mTriggeredBy;
		mLastTriggeredAt = dba.mLastTriggeredAt;
		mLastTriggeredBy = dba.mLastTriggeredBy;
		mPausedAt = dba.mPausedAt;
	}
	
	public Class<?> getConfigActivity() {
		return AgentConfigurationActivity.class;
	}

	
	public abstract int getNameId();
	public abstract int getDescriptionId();
	public abstract int getLongDescriptionId();

	public abstract AgentPermission[] getTriggerArray();

	
	@Override
	public final String getName() {
		return mContext.getResources().getString(getNameId());
	}
	
	@Override
	public final String getDescription() {
		return mContext.getResources().getString(getDescriptionId());
	}
	
	@Override
	public final String getLongDescription() {
		return mContext.getResources().getString(getLongDescriptionId());
	}


    /** Notification common code **/
    /** will be used by agents that implement AgentNotificationInterface **/


    public final void notifyStartAgent(Context context, int triggerType, boolean unpause) {
        AgentNotification notif;

        if (unpause
                || (triggerType == Constants.TRIGGER_TYPE_MANUAL)
                || NotificationUtils.AgentNotificationSeen(getGuid(), context)) {
            notif = new AgentStartingNotification(context, this, triggerType, unpause);
        } else {
            NotificationUtils.SetAgentNotificationSeen(getGuid(), context, true);
            notif = new AgentFirstStartingNotification(context, this, triggerType, unpause);
        }

        NotificationFactory.notify(context, notif);

        if (!unpause &&
                PrefsHelper.getPrefBool(context, Constants.PREF_SOUND_ON_AGENT_START, false) &&
                shouldPlayStartNotification()) {
            Utils.playNotificationSound(context);
        }
    }

    public final void notifyStopAgent(Context context, int triggerType, long ranForMillis) {
        NotificationFactory.dismissMain(context, this.getGuid());
    }

    public final void notifyPauseAgent(Context context, int triggerType, long ranForMillis) {
        NotificationFactory.dismissMain(context, this.getGuid());
    }


    /** Delayable Notification common code **/
    /** will be used by agents that implement DelayableNotificationInterface **/


    public final void notifyDelayAgent(Context context, int triggerType) {
        AgentDelayNotification notif = new AgentDelayNotification(context, this, triggerType);
        NotificationFactory.notify(context, notif);
    }

    public void delay(Context context, int triggerType) {
        pause(context);
        NotificationFactory.dismissMain(context, getGuid());
        notifyDelayAgent(context, triggerType);
    }

    public void skip(Context context, int triggerType) {
        pause(context);
        NotificationFactory.dismissMain(context, getGuid());

        // skip: do nothing by default
        // will still show as paused in main UI
        // but notification will be gone
    }

    protected boolean shouldPlayStartNotification() {
        return true;
    }

}
