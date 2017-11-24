package com.mobiroo.n.sourcenextcorporation.agent.activity;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.telephony.TelephonyManager;
import android.view.Window;
import android.view.WindowManager;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.TextSpeaker;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.SmsReplyFragment;

public class SmsReplyActivity extends FragmentActivity {
    
    KeyguardManager km;
    @SuppressWarnings("deprecation")
    KeyguardManager.KeyguardLock kl;
    
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        kl = km.newKeyguardLock("Agent");
        kl.disableKeyguard();
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.dialog_activity_container);

        Fragment fragment = new SmsReplyFragment();
        fragment.setArguments(getIntent().getExtras());
        
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(android.R.id.content, fragment);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
    }
    @Override
    public void onPause() {
        super.onPause();
        try { this.unregisterReceiver(receiver); }
        catch (Exception ignored) {}
        sendSpeakingCompleteBroadcast();
    }
    @SuppressWarnings("deprecation")
    @Override
    public void onDestroy() {
        kl.reenableKeyguard();
        super.onDestroy();
    }

    public void sendSpeakingCompleteBroadcast() {
        Logger.d("Broadcasting SMS reading complete");
        sendBroadcast(new Intent(TextSpeaker.ACTION_READ_SMS_COMPLETE));
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                sendSpeakingCompleteBroadcast();
                finish();
            }
        }
    };
}
