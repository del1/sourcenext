package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.app.Activity;

import java.util.UUID;

/**
 * Created by Pritam on 28/06/17.
 */

public class AppUUID {

    private AppFileUtil mAppFileUtil;
    private Activity mActivity;

    private final String APP_UUID_FILE = "app_uuid" ;

    public AppUUID(Activity activity){
        this.mActivity = activity;
        mAppFileUtil = new AppFileUtil(APP_UUID_FILE);
    }

    public String getUUID(){
        String uuid = null;

        uuid = PrefsHelper.getPrefString(mActivity, Constants.PREF_APP_UUID, null);

        if(ValidatorUtility.isBlank(uuid)){
           uuid = mAppFileUtil.readFile();
        }

        if(ValidatorUtility.isBlank(uuid)){
            uuid = UUID.randomUUID().toString();

            PrefsHelper.setPrefString(mActivity, Constants.PREF_APP_UUID, uuid);
            mAppFileUtil.writeFile(uuid);
        }

        return uuid;
    }
}
