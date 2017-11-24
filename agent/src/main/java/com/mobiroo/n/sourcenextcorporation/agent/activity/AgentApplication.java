package com.mobiroo.n.sourcenextcorporation.agent.activity;


import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

import io.fabric.sdk.android.Fabric;

public class AgentApplication extends Application implements Application.ActivityLifecycleCallbacks{

    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        Context context = getApplicationContext();

        int curVersion = Utils.getPackageVersionInt(context);
        int lastVersion = PrefsHelper.getPrefInt(context, Constants.PREF_LAST_APP_VERSION, -1);
        if (curVersion != lastVersion) {
            if (lastVersion != -1) {
                Logger.d("AgentApplication upgrade: checking receivers");
                Utils.checkReceivers(context);
            }
            PrefsHelper.setPrefInt(context, Constants.PREF_LAST_APP_VERSION, curVersion);
        }

        PrefsHelper.setPrefBool(context, Constants.PREF_GRANDFATHER, true);
        Usage.initialize(context);

    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        Usage.startSession(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Usage.endSession(activity);
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }
}
