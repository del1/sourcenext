package com.mobiroo.n.sourcenextcorporation.agent.action;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

import java.util.HashMap;

public class VerifyUrgentSmsAction extends AutorespondSmsAction {

	@Override
	protected void performActions(int operation, int triggerType, Object extraInfo) {
		HashMap<String, String> agentPrefs = mAgent.getPreferencesMap();

		if (operation == OPERATION_UNTRIGGER) {return;}		
		if (extraInfo == null) {return;}
		
		if (!isVerifyUrgentMode(agentPrefs)) {
			Logger.d("VerifyUrgentSMSAction: urgent mode not on.");
			return;
		}


		Logger.d("VerifyUrgentSMSAction: trigger");

		String originatingAddress = ((String) extraInfo);
		if (! isAllowedAutorespondContact(agentPrefs, originatingAddress)) {return;}
		String statusMessage = mContext.getString(triggerType == Constants.TRIGGER_TYPE_SMS ? R.string.status_texted_text : R.string.status_texted_phone);
		
		sendVerifyUrgentSms(agentPrefs, originatingAddress, statusMessage);
	}


}
