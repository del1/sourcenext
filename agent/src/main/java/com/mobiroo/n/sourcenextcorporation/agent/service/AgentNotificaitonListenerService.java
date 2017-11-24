package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.TextSpeaker;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.DriveAgent;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by krohnjw on 8/13/2015.
 */
@SuppressLint("OverrideAbstract")
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AgentNotificaitonListenerService extends NotificationListenerService {

    private List<String> mPackages;

    private List<String> getPackages() {
        return new ArrayList<String>() {
            {
                add("com.google.android.talk"); // Hangouts
                add("com.whatsapp"); // Whatsapp
                add("com.facebook.orca"); // Facebook Messenger
            }
        };
    }
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Logger.d("Not reading message due to unsupported platform version");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String packageName = sbn.getPackageName();
            if (!getPackages().contains(packageName)) {
                Logger.d("Did not find a matching package, returning");
                return;
            }

            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString("android.title");
//            String text = extras.getCharSequence("android.text").toString();

            // Changed type of text to CharSequence from String, so that it will not throw NPE
            // even if 'android.text' is null.
            // Should the message be allowed to be read aloud if the text is null?
            CharSequence text = extras.getCharSequence("android.text");

            DriveAgent a = (DriveAgent) AgentFactory.getAgentFromGuid(this, DriveAgent.HARDCODED_GUID);
            if (!a.isActive()) {
                Logger.d("Drive Agent is not active, returning");
                return;
            }

            boolean doVoiceResponse = Boolean.parseBoolean(a.getPreferencesMap().get(AgentPreferences.NOTIFICATIONS_READ_ALOUD_VOICE_RESPONSE));
            if (doVoiceResponse) {
                // TODO: Get use BT when ready from prefs
                Agent drive = AgentFactory.getAgentFromGuid(this, DriveAgent.HARDCODED_GUID);
                boolean useSpeaker = Boolean.parseBoolean(drive.getPreferencesMap().get(AgentPreferences.SMS_READ_USING_SPEAKERPHONE));
                TextSpeaker ts = new TextSpeaker(this, true, !useSpeaker);
                ts.addText("Message received.");
                ts.addText(title + ".");
                ts.addText(text + ".");
                ts.speak();
            }
        }


    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // http://cdn.shopify.com/s/files/1/0175/8784/products/Number6DontCare400w_large.jpg?v=1377739464

    }
}
