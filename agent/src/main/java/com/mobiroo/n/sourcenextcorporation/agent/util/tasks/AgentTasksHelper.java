package com.mobiroo.n.sourcenextcorporation.agent.util.tasks;

import android.app.ProgressDialog;
import android.content.Context;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.R;

/**
 * Created by omarseyal on 4/7/14.
 */
public class AgentTasksHelper {
    public static void manualActivateDeactivate(Context context, String agentGuid, boolean activate, boolean pauseAction, AgentTaskCollection taskCollection) {
        Logger.d("AgentTasksHelper manualActivateDeactivate: guid=" + agentGuid + ", activate=" + activate + ", pauseAction=" + pauseAction);

        ProgressDialog dialog = ProgressDialog.show(context, "", context.getString(R.string.working), true);
        dialog.setIndeterminateDrawable(context.getResources().getDrawable(R.drawable.agent_animation_agent));
        new AgentManualActivateDeactivateTask(context, dialog, activate, agentGuid, pauseAction, taskCollection).execute();
    }

    public static void installUninstall(Context context, String agentGuid, boolean install, AgentTaskCollection taskCollection) {
        Logger.d("AgentTasksHelper installUninstall: guid=" + agentGuid + ", install=" + install);

        ProgressDialog dialog = ProgressDialog.show(context, "", context.getString(R.string.working), true);
        dialog.setIndeterminateDrawable(context.getResources().getDrawable(R.drawable.agent_animation_agent));
        new AgentInstallUninstallTask(context, dialog, agentGuid, taskCollection, install).execute();
    }

    public static void checkReceiversAsync(Context context, AgentTaskCollection taskCollection) {
        Logger.d("AgentTaskHeloer checkReceiversAsync");
        new CheckReceiversTask(context, null, taskCollection).execute();
    }

    public static void showStartingNotification(Context context, String agentGuid, int triggerType, AgentTaskCollection taskCollection) {
        Logger.d("AgentTasksHelper showStartingNotification; guid =" + agentGuid);
        new ShowStartingNotificationTask(context, agentGuid, triggerType, taskCollection).execute();
    }


}
