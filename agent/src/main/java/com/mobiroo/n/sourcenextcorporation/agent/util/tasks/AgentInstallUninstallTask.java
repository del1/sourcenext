package com.mobiroo.n.sourcenextcorporation.agent.util.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;

/**
 * Created by omarseyal on 4/5/14.
 */
public class AgentInstallUninstallTask extends AsyncTask<Void, Void, Void> {
    protected ProgressDialog mDialog;
    protected Context mContext;
    protected boolean mInstall;
    protected String mAgentGuid;
    protected AgentTaskCollection mTaskCollection;

    public AgentInstallUninstallTask(Context context, ProgressDialog dialog, String agentGuid, AgentTaskCollection taskCollection, boolean install) {
        mContext = context;
        mDialog = dialog;
        mAgentGuid = agentGuid;
        mInstall = install;
        mTaskCollection = taskCollection;
    }


    @Override
    protected void onPreExecute() {
        if (mTaskCollection != null)
            mTaskCollection.addTask(this);
    }

    @Override
    protected Void doInBackground(Void... nothing) {
        try {
            Agent agent = AgentFactory.getAgentFromGuid(mContext, mAgentGuid);

            if (agent == null) { return null; }

            if (mInstall) {
                agent.install(mContext, false, false);
            } else {
                agent.uninstall(mContext, false);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (this.isCancelled())
            return;

        try {
            if (mDialog != null)
                mDialog.dismiss();
        } catch (Exception e) {}

        if(mTaskCollection != null)
            mTaskCollection.completeTask(this);
    }
}
