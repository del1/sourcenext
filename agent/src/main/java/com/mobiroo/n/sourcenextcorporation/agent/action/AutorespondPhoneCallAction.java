package com.mobiroo.n.sourcenextcorporation.agent.action;

import android.content.SharedPreferences;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.SleepAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import java.util.HashMap;


public class AutorespondPhoneCallAction extends AutorespondSmsAction {
	
	private static final String PHONE_CALL_SESSION_LOG = "APCAPhoneCallSessionLog";
	private static final String MAXIMIZED_VOLUMES = "APCAMaximizedVols";

	@Override
	protected void performActions(int operation, int triggerType, Object extraInfo) {
		if (extraInfo == null) {return;}
		String originatingPhone = ((String) extraInfo);
		
		HashMap<String, String> agentPrefs = mAgent.getPreferencesMap();

		boolean respondPhoneCall = Boolean.parseBoolean(agentPrefs.get(AgentPreferences.PHONE_CALL_AUTORESPOND));
		if (! respondPhoneCall) {return;}

		if ((operation == OPERATION_TRIGGER) && (handleRepeatCall(agentPrefs, originatingPhone))) {return;}
		
		// use a missed call trigger for urgent mode
		if (isVerifyUrgentMode(agentPrefs)) {return;}

		if (operation == OPERATION_UNTRIGGER) {
			Logger.d("AutorespondPhoneCallAction untrigger");
			undoPhoneCallAction(agentPrefs);
			return;
		}
		

		Logger.d("AutorespondPhoneCallAction: trigger");

		if (! isAllowedAutorespondContact(agentPrefs, originatingPhone)) {return;}
				
		// do the action action based on mode
		doPhoneCallAction(agentPrefs, originatingPhone);		
	}

	
	@SuppressWarnings("unchecked")
	private boolean handleRepeatCall(HashMap<String, String> prefs, String originatingPhone) {
		if ((originatingPhone == null) || (originatingPhone.trim().length() < 7)) {return false;}

		boolean allowRepeatCallMode = Boolean.parseBoolean(prefs.get(AgentPreferences.PHONE_CALL_ALLOW_ON_REPEAT_CALL));
		if (! allowRepeatCallMode) {return false;}

		String adjOriginatingPhone = originatingPhone.replace("+", "");

		SharedPreferences agentSession = mAgent.getSession();
		String prefVal = agentSession.getString(PHONE_CALL_SESSION_LOG, "");
		HashMap<String, String> callLog;
		
		if (prefVal.trim().isEmpty()) {
			callLog = new HashMap<String, String>();
		} else {
			callLog = (HashMap<String, String>) Utils.deserializeFromBase64String(prefVal);
		}

		long nowMillis = System.currentTimeMillis();
		
		long prevCallMillis = 0;
		try {
			prevCallMillis = Long.parseLong(callLog.get(adjOriginatingPhone));
		} catch (NumberFormatException e) {
			prevCallMillis = 0;
		}
		
		callLog.put(adjOriginatingPhone, String.valueOf(nowMillis));
		agentSession.edit().putString(PHONE_CALL_SESSION_LOG, Utils.serializeToBase64String(callLog)).commit();
		
		if (prevCallMillis < (nowMillis - SleepAgent.DEFAULT_REPEAT_CALL_PERIOD)) {
			// no call in time period
			return false;
		}

		// found call in time period, so do wake up, etc.
		doPhoneCallAction(prefs, originatingPhone);
		return true;
	}

	
	private void doPhoneCallAction(HashMap<String, String> prefs, String originatingPhone) {
		String statusMessage = mContext.getString(R.string.status_texted_phone);		

		if (prefs.get(AgentPreferences.PHONE_CALL_AUTORESPOND_MODE).equals(AgentPreferences.AUTORESPOND_MODE_WAKE)) {
			Logger.d("AutorespondPhoneCallAction: maximizing volumes");
			PhoneSilenceAction.maximizeVolumes(mContext, mAgentGuid, "AutorespondPhoneCallAction");
			PhoneSilenceAction.startRinging(mContext);
			mAgent.getSession().edit().putBoolean(MAXIMIZED_VOLUMES, true).commit();
			return;
		}

		if (prefs.get(AgentPreferences.PHONE_CALL_AUTORESPOND_MODE).equals(AgentPreferences.AUTORESPOND_MODE_RESPOND)) {
			sendSmsResponse(prefs, originatingPhone, statusMessage);
			return;
		}
	}
	
	private void undoPhoneCallAction(HashMap<String, String> prefs) {
		if (prefs.get(AgentPreferences.PHONE_CALL_AUTORESPOND_MODE).equals(AgentPreferences.AUTORESPOND_MODE_WAKE)) {
			SharedPreferences agentSession = mAgent.getSession();
			if (agentSession.getBoolean(MAXIMIZED_VOLUMES, false)) {
				PhoneSilenceAction.resetVolumes(mContext, mAgentGuid, "AutorespondPhoneCallAction");
				PhoneSilenceAction.stopRinging();
				Logger.d("AutorespondPhoneCallAction: reset volumes to previous levels");
			}
			agentSession.edit().remove(MAXIMIZED_VOLUMES).commit();
			return;
		}
		Logger.d("AutorespondPhoneCallAction: not in wake mode for untrigger");
	}
}
