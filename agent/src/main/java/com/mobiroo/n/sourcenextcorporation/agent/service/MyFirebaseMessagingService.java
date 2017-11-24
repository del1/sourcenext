package com.mobiroo.n.sourcenextcorporation.agent.service;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.NotificationUtil;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;

import java.util.Map;

/**
 * FCM通知受信クラス
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    /**
     * メッセージ受信時の処理
     * @param remoteMessage
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Map<String, String> data = remoteMessage.getData();
        if (data == null) return;

        if (PrefsHelper.getPrefBool(this,Constants.PREF_PUSH_NOTIFICATION,false)) {
            String msg = data.get("msg");
            String snd = data.get("snd");
            String url = data.get("url");

            if (msg != null) {
                NotificationUtil.showNotification(this, msg, snd, url);
            }
        }
    }
}
