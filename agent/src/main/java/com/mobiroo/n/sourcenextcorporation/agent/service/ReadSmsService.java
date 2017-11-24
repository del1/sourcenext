package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.TelephonyManager;

import com.mobiroo.n.sourcenextcorporation.agent.activity.SmsReplyActivity;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.TextSpeaker;

import java.util.ArrayDeque;
import java.util.Queue;

public class ReadSmsService extends Service {

    private boolean mIsSpeaking = false;

    private final String ACTION_CANCEL_SELF = "ReadSmsService.CancelSelf";

    private Queue<Message> mPendingMessages;
    private boolean mUseBTIfAvailable;
    private boolean mRespondWithHeadset;

    private AlarmManager mManager;

    private IntentFilter mFilter = new IntentFilter() {
        {
            addAction(TextSpeaker.ACTION_READ_SMS_COMPLETE);
        }
    };

    private void logd(String message) {
        Logger.d("SMS-SERVICE: " + message);
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

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            logd("Received return broadcast, setting speaking to false");
            mIsSpeaking = false;
            if (mPendingMessages.peek() != null) {
                logd("Speaking next message");
                speakMessage();
            } else {
                logd("Stopping service");
                stopService();
            }

        }
    };

    private class Message {
        public String body;
        public String number;
        public String who;

        public Message(String number, String who, String body) {
            this.number = number;
            this.body = body;
            this.who = who;
        }

        @Override
        public String toString() {

            return who + "<" + number + "> " + body;
        }

        public boolean isNull() {
            return (who == null) && (number == null) && (body == null);
        }
    }

    private PendingIntent getCancelIntent() {
      return PendingIntent.getService(this,
              91,
              new Intent(this, ReadSmsService.class).setAction("CANCEL"),
              PendingIntent.FLAG_ONE_SHOT);
    }

    private void stopAlarm(PendingIntent p) {
        mManager.cancel(p);
    }

    private void setAlarm(PendingIntent p) {
        mManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5 * 60 * 1000, p);
    }

    private void scheduleAutoCancel() {
        PendingIntent p = getCancelIntent();
        stopAlarm(p);
        setAlarm(p);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        mPendingMessages = new ArrayDeque<Message>();
        mIsSpeaking = false;
        mManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        registerReceiver(mReceiver, mFilter);
        registerReceiver(mPhoneStateReceiver, new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null) {
            logd("Service restarting, intent is null");
            if (mPendingMessages.size() == 0) {
                logd("No pending messages, service stopping self");
                stopAlarm(getCancelIntent());
                this.stopSelf();
            } else {
                logd("One or more pending message, speaking if ready");
                speakIfReady();
            }
        } else {
            String action = intent.getAction();
            if (ACTION_CANCEL_SELF.equals(action)) {
                logd("Auto cancelling");
                stopAlarm(getCancelIntent());
                this.stopSelf();
                return START_NOT_STICKY;
            }

            logd("Received incoming intent with data");
            String body = intent.getStringExtra(TextSpeaker.EXTRA_MESSAGE);
            String number = intent.getStringExtra(TextSpeaker.EXTRA_NUMBER);
            String who = intent.getStringExtra(TextSpeaker.EXTRA_SENDER);

            if ((body == null) && (who == null) && (number == null)) {
                logd("Received all null params, exiting");
                stopService();
            }

            mUseBTIfAvailable = intent.getBooleanExtra(TextSpeaker.EXTRA_USE_BLUETOOTH_IF_AVAILABLE, true);
            mRespondWithHeadset = false; //intent.getBooleanExtra(TextSpeaker.EXTRA_RESPOND_WITH_HEADSET, false);

            Message message = new Message(number, who, body);
            if ((message != null) && (!message.isNull())) {
                logd("Received message " + message.toString());

                if (mPendingMessages == null) {
                    mPendingMessages = new ArrayDeque<Message>();
                }

                mPendingMessages.add(message);

                speakIfReady();
            }

        }
        return START_STICKY;
    }

    private void speakIfReady() {
        if (!mIsSpeaking) {
            logd("Not currently speaking, reading message");
            speakMessage();
        } else {
            logd("Reading in process, queueing for response");
            logd(mPendingMessages.size() + " queued");
            // Schedule an alarm to wake up and cancel service if for some reason the activity / fragment does not broadcast complete
            scheduleAutoCancel();
        }
    }

    private void stopService() {
        this.stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try { this.unregisterReceiver(mReceiver); }
        catch (Exception ignored) {}

        try { this.unregisterReceiver(mPhoneStateReceiver); }
        catch (Exception ignored) {}

    }

    private void speakMessage() {
        // See if we have a pending message
        logd("Checking for a pending message");
        if ((mPendingMessages.peek() != null)) {
            Message pending = mPendingMessages.poll();
            logd("Speaking " + pending.toString());
            mIsSpeaking = true;
            Intent intent = new Intent(this, SmsReplyActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

            intent.putExtra(TextSpeaker.EXTRA_MESSAGE, pending.body);
            intent.putExtra(TextSpeaker.EXTRA_NUMBER, pending.number);
            intent.putExtra(TextSpeaker.EXTRA_SENDER, pending.who);
            intent.putExtra(TextSpeaker.EXTRA_USE_BLUETOOTH_IF_AVAILABLE, mUseBTIfAvailable);
            intent.putExtra(TextSpeaker.EXTRA_RESPOND_WITH_HEADSET, mRespondWithHeadset);
            startActivity(intent);
        }
    }

}
