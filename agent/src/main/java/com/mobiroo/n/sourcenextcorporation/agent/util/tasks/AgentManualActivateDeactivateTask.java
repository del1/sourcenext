package com.mobiroo.n.sourcenextcorporation.agent.util.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

/**
 * Created by omarseyal on 4/7/14.
 */
public class AgentManualActivateDeactivateTask extends
        AsyncTask<Void, Void, Void> {

        protected ProgressDialog mDialog;
        protected boolean mActivate;
        protected boolean mPauseAction;
        protected Context mContext;
        protected String mAgentGuid;
        protected Agent mAgent;

        protected AgentTaskCollection mTaskCollection;

        public AgentManualActivateDeactivateTask(Context context, ProgressDialog dialog, boolean activate, String agentGuid, boolean pauseAction, AgentTaskCollection taskCollection) {
            mDialog = dialog;
            mActivate = activate;
            mContext = context;
            mAgentGuid = agentGuid;
            mPauseAction = pauseAction;

            mTaskCollection = taskCollection;
        }

        @Override
        protected void onPreExecute() {
            mTaskCollection.addTask(this);
        }

        @Override
        protected Void doInBackground(Void... nothing) {
            try {
                mAgent = AgentFactory.getAgentFromGuid(mContext, mAgentGuid);
                if (mAgent == null) {
                    return null;
                }
                if (mActivate) {
                    if (mPauseAction) {
                        mAgent.unPause(mContext);
                    } else {
                        DbAgent.setActive(mContext, mAgentGuid, Constants.TRIGGER_TYPE_MANUAL);
                    }
                } else {
                    if (mPauseAction) {
                        mAgent.pause(mContext);
                    } else {
                        DbAgent.setInactive(mContext, mAgentGuid, mAgent.getTriggeredBy());
                    }
                }
                Thread.sleep(400);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (this.isCancelled())
                return;

            mTaskCollection.completeTask(this);

            try {
                if (mDialog != null) {mDialog.dismiss();}

                if ((mAgent != null) &&  mActivate && !mPauseAction) {
                    String toastMsg = String.format(mContext.getResources().getString(R.string.agent_manual_start_toast), mAgent.getName());
                    Toast.makeText(mContext, toastMsg, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Logger.d("Error: " + e.toString());
            }


        }

}
