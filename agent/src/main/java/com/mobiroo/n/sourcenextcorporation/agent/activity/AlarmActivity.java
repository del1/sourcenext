package com.mobiroo.n.sourcenextcorporation.agent.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.action.PhoneSilenceAction;
import com.mobiroo.n.sourcenextcorporation.agent.R;

public class AlarmActivity extends Activity {

    public static final String EXTRA_ALARM_TEXT = "alarmText";
    public static final String EXTRA_AGENT_GUID = MainActivity.EXTRA_AGENT_GUID;

    public static final int MAX_PLAY_MILLIS = 20000;

    public static final String REQUEST_CODE = "AlarmActivity";

    Vibrator mVibrator;
    TextView mAlarmTextView;
    Button mDismissButton;
    MediaPlayer mMediaPlayer;

    String mAgentGuid;
    boolean mAlarmRunning;
    long mAlarmStartedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        Intent intent = getIntent();
        String alarmText = intent.getStringExtra(EXTRA_ALARM_TEXT);
        mAgentGuid = intent.getStringExtra(EXTRA_AGENT_GUID);

        mAlarmTextView = (TextView) findViewById(R.id.alarmtext);
        mAlarmTextView.setText(alarmText);

        mDismissButton =  (Button) findViewById(R.id.dismissbutton);
        mDismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlarmActivity.this.stopAlarm();
                AlarmActivity.this.finish();
            }
        });

        mAlarmRunning = false;
        startAlarm();
    }


    @Override
    public void onBackPressed() {
        Logger.d("AlarmActivity.onBackPressed");
        this.stopAlarm();
        super.onBackPressed();
    }

    @Override
    public void onUserLeaveHint() {
        Logger.d("AlarmActivity.onUserLeaveHint");

        // handles cases like gosms pro that thrown an additional dialog
        if (mAlarmStartedAt > (System.currentTimeMillis() - 1500)) {
            Logger.d("AlarmActivity.onUserLeaveHint within 1.5 seconds; not stopping alarm.");
        } else {
            this.stopAlarm();
        }

        super.onUserLeaveHint();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.d("AlarmActivity.onDestroy");
        this.stopAlarm();
    }

    protected void stopAlarm() {
        Logger.d("AlarmActivity.stopAlarm");
        if (mAlarmRunning == false) {
            Logger.d("AlarmActivity.stopAlarm: alarmRunning = false; no need to stop.");
            return;
        }
        mAlarmStartedAt = 0;

        mVibrator.cancel();
        mMediaPlayer.stop();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mAlarmRunning = false;
        PhoneSilenceAction.resetVolumes(this, mAgentGuid, REQUEST_CODE);
    }

    protected void startAlarm() {
        Logger.d("AlarmActivity.startAlarm");
        mAlarmRunning = true;
        mAlarmStartedAt = System.currentTimeMillis();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PhoneSilenceAction.maximizeVolumes(this, mAgentGuid, REQUEST_CODE);


        // Get instance of Vibrator from current Context
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // This example will cause the phone to vibrate "SOS" in Morse Code
        int dot = 200;      // Length of a Morse Code "dot" in milliseconds
        int dash = 500;     // Length of a Morse Code "dash" in milliseconds
        int short_gap = 200;    // Length of Gap Between dots/dashes
        int medium_gap = 500;   // Length of Gap Between Letters
        int long_gap = 1000;    // Length of Gap Between Words
        long[] pattern = {
                0,  // Start immediately
                dot, short_gap, dot, short_gap, dot,    // s
                medium_gap,
                dash, short_gap, dash, short_gap, dash, // o
                medium_gap,
                dot, short_gap, dot, short_gap, dot,    // s
                long_gap,
                dot, short_gap, dot, short_gap, dot,    // s
                medium_gap,
                dash, short_gap, dash, short_gap, dash, // o
                medium_gap,
                dot, short_gap, dot, short_gap, dot,    // s
                long_gap,
                dot, short_gap, dot, short_gap, dot,    // s
                medium_gap,
                dash, short_gap, dash, short_gap, dash, // o
                medium_gap,
                dot, short_gap, dot, short_gap, dot,    // s
                long_gap
        };

        mVibrator.vibrate(pattern, -1);

        Uri alert =  RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setDataSource(this, alert);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
            mMediaPlayer.setLooping(true);
            mMediaPlayer.prepare();
            mMediaPlayer.start();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Logger.d("AlarmActivity: ending alarm after delay");
                    try {
                        AlarmActivity.this.stopAlarm();
                    } catch (Exception e) {
                        Logger.e("AlarmActivity: exception in delayed handler:" + e.toString());
                    }
                }
            }, MAX_PLAY_MILLIS);

        } catch (Exception e) {
            Logger.e(e.getMessage());
        }

    }

}
