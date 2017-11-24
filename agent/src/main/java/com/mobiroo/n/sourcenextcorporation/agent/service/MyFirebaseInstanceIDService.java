package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

/**
 * InstanceID Listenerクラス（Googleのリファレンス実装通り）
 */
public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    /**
     * Token更新時のハンドラ
     */
    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        String token = FirebaseInstanceId.getInstance().getToken();
        Log.e(Constants.TAG,"Firebase Instance ==> "+token);
        MyRegistrationIntentService.startIntentService(this);
    }
}
