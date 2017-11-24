package com.mobiroo.n.sourcenextcorporation.agent.util.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import com.mobiroo.n.sourcenextcorporation.agent.service.LocationRegistrationService;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;

public class CheckReceiversTask extends AsyncTask<Void, Void, Void> {
    protected ProgressDialog mDialog;
    protected Context mContext;
    protected AgentTaskCollection mTaskCollection;

    public CheckReceiversTask(Context context, ProgressDialog dialog, AgentTaskCollection taskCollection) {
        mContext = context;
        mDialog = dialog;
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
            Utils.checkReceivers(mContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (this.isCancelled())
            return;

        /* Must be run on foreground thread */
        mContext.startService(new Intent(mContext, LocationRegistrationService.class));

        try {
            if (mDialog != null)
                mDialog.dismiss();
        } catch (Exception e) {}

        if(mTaskCollection != null)
            mTaskCollection.completeTask(this);
    }
}
