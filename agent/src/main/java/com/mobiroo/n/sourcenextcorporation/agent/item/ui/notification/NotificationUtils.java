package com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification;

import android.content.Context;

import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

public class NotificationUtils {

	// assume it has been seen
	public static boolean AgentNotificationSeen(String agentGuid,
			Context context) {
		return PrefsHelper.getPrefBool(context,
				AgentNotificationSeenPreference(agentGuid), true);
	}

	public static void SetAgentNotificationSeen(String agentGuid,
			Context context, boolean seen) {
		PrefsHelper.setPrefBool(context, AgentNotificationSeenPreference(agentGuid), seen);
	}

	public static String AgentNotificationSeenPreference(String agentGuid) {
		return AgentPreferences.AGENT_NOTIFICATION_SEEN + agentGuid;
	}

}
