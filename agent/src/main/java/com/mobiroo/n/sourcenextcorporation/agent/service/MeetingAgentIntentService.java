package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.MeetingAgent;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

public class MeetingAgentIntentService extends IntentService {

    public static final int ACTION_TRIGGER      = 1;
    public static final int ACTION_UNTRIGGER    = 2;
    public static final int ACTION_SCAN         = 3;
    
    public static final String EXTRA_ACTION = "tryagent.meetingAgent.action";
    public static final String EXTRA_MEETING_ID = "tryagent.meetingAgent.meetingId";
    
    PowerManager    mPowerManager;
    WakeLock        mWakeLock;
    

    private void acquireWakeLock() {
        Logger.d("MeetingAgent: Wake lock acquired");
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NFCTL_timer");
        mWakeLock.acquire();
    }
    
    private void releaseWakeLock() {
        Logger.d("MeetingAgent: Wake lock released");
        mWakeLock.release();
    }
   
    public MeetingAgentIntentService() {
        super("Meeting Agent");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        acquireWakeLock();
        
        if ((intent != null) && (intent.hasExtra(EXTRA_ACTION))) {
        	Logger.d("MAIS: Action is " + intent.getIntExtra(EXTRA_ACTION, ACTION_UNTRIGGER) + 
        			", meetingId is: " + intent.getLongExtra(EXTRA_MEETING_ID, -2));
        	
            switch (intent.getIntExtra(EXTRA_ACTION, ACTION_UNTRIGGER)) {
                case ACTION_TRIGGER:
                    // Only trigger if installed
                    if (AgentFactory.getAgentFromGuid(this, MeetingAgent.HARDCODED_GUID).isInstalled()) {
                        Logger.d("MAIS: Calling setActive");
                	    DbAgent.setActive(this, MeetingAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_TIME);
                    }
                    break;
                case ACTION_UNTRIGGER:
                    // Only untrigger if installed
                    if (AgentFactory.getAgentFromGuid(this, MeetingAgent.HARDCODED_GUID).isInstalled()) {
                        Logger.d("MAIS: Calling setInactive");
                        DbAgent.setInactive(this, MeetingAgent.HARDCODED_GUID, Constants.TRIGGER_TYPE_TIME);
                        MeetingAgent agent = (MeetingAgent) AgentFactory.getAgentFromGuid(this, MeetingAgent.HARDCODED_GUID);
                        agent.updateScheduledAlarms(this);
                    }
                    break;
                case ACTION_SCAN:
                	Logger.d("MAIS: Calling scan.");
                    MeetingAgent agent = (MeetingAgent) AgentFactory.getAgentFromGuid(this, MeetingAgent.HARDCODED_GUID);
                    agent.updateScheduledAlarms(this);
                    break;
            }
        }
        releaseWakeLock();
    }
    
}
