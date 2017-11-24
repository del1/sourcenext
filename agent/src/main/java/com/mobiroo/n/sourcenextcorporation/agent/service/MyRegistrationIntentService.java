package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;
import com.pushmaker.applibs.AbstractPushMakerRegistrationService;

/**
 * FCM RegistrationおよびPushMakerとの通信を行うIntentServiceの具象クラス
 */
public class MyRegistrationIntentService extends AbstractPushMakerRegistrationService {

    /**
     * RegisterおよびPushMaker送信のIntentServiceを開始
     * @param context
     */
    public static void startIntentService(Context context) {
        Log.d(Constants.TAG, "pushMakerRegisterStarted");
        Intent intent = new Intent(context, MyRegistrationIntentService.class);
        // 必要ならば端末IDを設定
        intent.putExtra(AbstractPushMakerRegistrationService.EXTRA_TERMINAL_ID, "TERMINAL_ID");

        context.startService(intent);
    }

    /**
     * GCM登録解除のIntentServiceを開始
     * @param context
     */
    public static void startUnregisterIntentService(Context context) {
        Intent intent = new Intent(context, MyRegistrationIntentService.class);
        intent.putExtra(AbstractPushMakerRegistrationService.EXTRA_DO_UNREGISTER, true);

        context.startService(intent);
    }

    /**
     * 登録成功時のハンドラ
     */
    @Override
    public void pushMakerRegisterFinished() {
        Log.d(Constants.TAG, "pushMakerRegisterFinished");
        PrefsHelper.setPrefBool(getApplicationContext(),Constants.PREF_FCM_REGISTERED,true);
    }

    /**
     * 登録スキップ時のハンドラ
     */
    @Override
    public void pushMakerRegisterSkipped() {
        Log.d(Constants.TAG, "pushMakerRegisterSkipped");
    }

    /**
     * 登録失敗時のハンドラ
     * @param e
     */
    @Override
    public void pushMakerRegisterFailed(Exception e) {
        e.printStackTrace();
        Log.d(Constants.TAG, "pushMakerRegisterFailed: " + e.toString());
    }

    /**
     * 登録解除時のハンドラ
     */
    @Override
    public void pushMakerUnregisterFinished() {
        Log.d(Constants.TAG, "pushMakerUnregisterFinished");
    }

    /**
     * 登録解除失敗時のハンドラ
     * @param e
     */
    @Override
    public void pushMakerUnregisterFailed(Exception e) {
        Log.d(Constants.TAG, "pushMakerUnregisterFailed: " + e.toString());
    }
}
