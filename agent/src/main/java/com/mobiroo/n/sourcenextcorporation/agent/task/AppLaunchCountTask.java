package com.mobiroo.n.sourcenextcorporation.agent.task;

import android.app.Activity;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.util.Log;

import com.mobiroo.n.sourcenextcorporation.agent.BuildConfig;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.constants.AppURLs;
import com.mobiroo.n.sourcenextcorporation.agent.util.AppUUID;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.ServiceHandler;

import java.util.HashMap;

/**
 * Created by Pritam on 28/06/17.
 */

public class AppLaunchCountTask extends AsyncTask<String, String, String> {

    private APP_LAUNCH_TYPE mAct;
    private Activity mActivity;

    public enum APP_LAUNCH_TYPE {
        START("start"),
        PUSH_START("pushstart"),
        PUSH("push");

        private String mType;

        APP_LAUNCH_TYPE(String type) {
            this.mType = type;
        }

        public String url() {
            return mType;
        }
    }

        public AppLaunchCountTask(Activity activity, APP_LAUNCH_TYPE act){
        this.mActivity = activity;
        this.mAct = act;
    }

    @Override
    protected String doInBackground(String... params) {

        ServiceHandler serviceHandler = new ServiceHandler();
        HashMap<String, String> urlParams = new HashMap<String, String>();
        AppUUID appUUID = new AppUUID(mActivity);
        urlParams.put("uid", appUUID.getUUID());
        urlParams.put("act", mAct.url());
        urlParams.put("ver", BuildConfig.VERSION_NAME);
        if(mAct == APP_LAUNCH_TYPE.START) {
            urlParams.put("idfa", mActivity.getString(R.string.idfa));
        } else if(mAct == APP_LAUNCH_TYPE.PUSH){
            urlParams.put("view", (PrefsHelper.getPrefBool(mActivity, Constants.PREF_PUSH_NOTIFICATION, false)? "1" : "0"));
        } else {
            if(DateUtils.isToday(PrefsHelper.getPrefLong(mActivity, Constants.PREF_RT_DATE, 0L))) {
                return null;
            }
        }

        String urlParameters = serviceHandler.createQueryStringForParameters(urlParams);
        serviceHandler.makeServiceCall(AppURLs.LAUNCH_COUNTS,ServiceHandler.GET,urlParameters,null,false);
        Log.e(Constants.TAG,"Response status ==> "+ServiceHandler.STATUS_CODE);
        PrefsHelper.setPrefLong(mActivity, Constants.PREF_RT_DATE, System.currentTimeMillis());
        return null;
    }
}
