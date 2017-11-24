package com.mobiroo.n.sourcenextcorporation.tagstand.util;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.action.ReadTextAction;

public class SpeakTextService extends Service {

   private TextSpeaker speaker;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        registerReceiver(mPhoneStateReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));

        if (intent != null) {

            String text = intent.getStringExtra(TextSpeaker.EXTRA_MESSAGE);
            String sender = intent.getStringExtra(TextSpeaker.EXTRA_SENDER);

            boolean useBTIfAvailable = intent.getBooleanExtra(TextSpeaker.EXTRA_USE_BLUETOOTH_IF_AVAILABLE, true);
            
            speaker = new TextSpeaker(this, false, useBTIfAvailable);
            if ((sender != null) && (!sender.isEmpty()) && !ReadTextAction.DO_NOT_READ_SENDER.equals(sender)) {
                speaker.addText(sender);
            } else if (!ReadTextAction.DO_NOT_READ_SENDER.equals(sender)) {
                speaker.addText(getString(AppSpecific.R_INCOMING_SMS));
            }
            
            if ((text != null) && (!text.isEmpty())) {
                speaker.addText(text);
            }
            
            speaker.speak();

            Usage.logEvent(this, Usage.Events.SMS_READ_WITH_VOICE, true);
        }
        
        return START_NOT_STICKY;
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { this.unregisterReceiver(mPhoneStateReceiver); }
        catch (Exception ignored) {}
    }

    private void stopService() {
        if (speaker != null) {
            speaker.shutDownNow();
        }
        this.stopSelf();
    }

    private BroadcastReceiver mPhoneStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                stopService();
            }
        }
    };
}
