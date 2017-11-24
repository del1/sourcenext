package com.mobiroo.n.sourcenextcorporation.tagstand.util;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.UtteranceProgressListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

@SuppressWarnings("deprecation")
public class TextSpeaker implements OnInitListener {

    public static final String EXTRA_SENDER = "text.speaker.sender";
    public static final String EXTRA_MESSAGE = "text.speaker.message";
    public static final String EXTRA_NUMBER = "text.speaker.number";
    public static final String EXTRA_USE_BLUETOOTH_IF_AVAILABLE = "text.speaker.use_bt_if_available";
    public static final String EXTRA_RESPOND_WITH_HEADSET = "text.speaker.response_with_headset";

    public static final String ACTION_READ_SMS_COMPLETE = "com.mobiroo.n.sourcenextcorporation.agent.read_sms_complete";

    public static final int RESULT_OK = 1;
    public static final int RESULT_ERROR = -1;

    private TextToSpeech mTts;
    private int mStream;
    private AudioManager mManager;

    private Context mContext;
    private ArrayList<String> mPendingText;

    private boolean mSpeakWhenReady;
    private boolean mIsTTSReady;
    private String mMaxUtteranceId;

    private HashMap<String, String> mTtsParams;
    private boolean mDisableBluetoothSco;
    private boolean mScoChanging;

    private SpeakerListener mListener;

    private boolean mRequestAudioFocus = true;

    private boolean mUseBluetoothIfAvailable = true;
    private boolean mDisableSpeakerPhone = false;

    private BroadcastReceiver mScoUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int mode = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);
            logd("TTS: SCO mode is " + mode);
            if (mode == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                mManager.setBluetoothScoOn(true);
                mScoChanging = false;
                speakText();
            }
        }
    };

    public TextSpeaker(Context context, boolean speakWhenReady, boolean useBTIfAvailable) {
        setupTts(context, speakWhenReady, useBTIfAvailable);
    }

    private void logd(String message) {
        Logger.d("TextSpeaker: " + message);
    }

    public void setListener(SpeakerListener listener) {
        mListener = listener;
    }

    private ArrayList<String> getPendingText() {
        if (mPendingText == null) {
            mPendingText = new ArrayList<String>();
        }
        return mPendingText;
    }

    public void addText(String text) {
        getPendingText().add(text);
    }

    public void clearText() {
        getPendingText().clear();
    }

    public void speak() {
        mSpeakWhenReady = true;
        speakText();
    }

    public void stopSpeaking() {
        try {
            mTts.stop();
        } catch (Exception ignored) {
        }
    }

    private void setupTts(Context context, boolean speakWhenReady, boolean useBTIfAvailable) {
        mContext = context;
        mManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        mIsTTSReady = false;
        mSpeakWhenReady = speakWhenReady;
        mScoChanging = false;

        mUseBluetoothIfAvailable = useBTIfAvailable;

        buildTtsParams(); // Set correct audio stream and tt parameters hash based on devices connected
        checkAudioStream(); // Modify audio settings based on current devices connected

        mTts = new TextToSpeech(context, this);
    }
    // --------------------------------------------------------------------
    // private/protected/internal methods below
    // --------------------------------------------------------------------

    private void checkAudioStream() {
        if (mUseBluetoothIfAvailable) {
            if (mStream == AudioManager.STREAM_VOICE_CALL) {
                if (mManager.isBluetoothScoAvailableOffCall()) {
                    if (!mManager.isBluetoothScoOn()) {
                        mContext.registerReceiver(mScoUpdatedReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
                        logd("Starting bluetooth SCO");
                        mScoChanging = true;
                        mManager.startBluetoothSco();
                        mDisableBluetoothSco = true;
                    }
                }
            }
        }
    }

    private void buildTtsParams() {
        mStream = AudioManager.STREAM_MUSIC;

        mTtsParams = new HashMap<String, String>();

        if (mUseBluetoothIfAvailable) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                if (adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED) {
                    logd("Bluetooth A2dp device connected");
                } else {
                    if (adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED) {
                        logd("Bluetooth headset connected");
                        mStream = AudioManager.STREAM_VOICE_CALL;
                    }
                }
            }
        } else {
            mStream = AudioManager.STREAM_VOICE_CALL;
        }

        logd("Using stream " + mStream);
        mTtsParams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(mStream));
    }

    @Override
    public void onInit(int status) {
        Logger.i("TTS: Init Status " + status);
        switch (status) {
            case TextToSpeech.SUCCESS:
                boolean success = setupTtsLanguage(Locale.getDefault());
                if (!success) {
                    logd("TTS: Missing Language or unsupported language using locale.  Trying English");
                    success = setupTtsLanguage(Locale.ENGLISH);
                }

                if (success) {
                    // TTS should have successfully set a language now
                    setupProgressListener();
                    mIsTTSReady = true;
                    speakText();
                } else {
                    logd("TTS: Failed to set a language for use, exiting");
                }
                break;
            default:
                logd("TTS: Init Failed, finishing");
                break;

        }
    }

    @SuppressLint("NewApi")
    private void setupProgressListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onDone(String utteranceId) {
                    logd("onDone: " + utteranceId);
                    if (utteranceId.equals(mMaxUtteranceId)) {
                        checkShutdown();
                        if (mListener != null) {
                            mListener.speakingCompleted(RESULT_OK);
                        }
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    logd("onError: " + utteranceId);
                    if (utteranceId.equals(mMaxUtteranceId)) {
                        checkShutdown();
                        if (mListener != null) {
                            mListener.speakingCompleted(RESULT_ERROR);
                        }
                    }
                }

                @Override
                public void onStart(String utteranceId) {
                    logd("onStart: " + utteranceId);
                    if (utteranceId.equals("0")) {
                        checkVolume();
                    }
                }
            });
        } else {
            mTts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
                @Override
                public void onUtteranceCompleted(String utteranceId) {
                    logd("onUtteranceCompleted: " + utteranceId);
                    if (utteranceId.equals(mMaxUtteranceId)) {
                        checkShutdown();
                        if (mListener != null) {
                            mListener.speakingCompleted(RESULT_OK);
                        }
                    }
                }
            });
        }

    }

    private void checkShutdown() {
        restoreVolume();

        try {
            mContext.unregisterReceiver(mScoUpdatedReceiver);
        } catch (Exception e) {
        }

        if (mDisableBluetoothSco) {
            logd("TTS: Disabling SCO");
            mManager.stopBluetoothSco();
            mManager.setBluetoothScoOn(false);
        }
    }

    public void destroy() {
        if (mTts != null) {
            mTts.shutdown();
        }
    }

    private boolean setupTtsLanguage(Locale locale) {
        logd("TTS: Setting tts locale");
        int result = mTts.setLanguage(locale);
        return (!(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED));
    }

    private void speakText() {
        if (mSpeakWhenReady && mIsTTSReady && !mScoChanging) {
            logd("Asking TTS to speak now; firing off utterance requests.");
            mMaxUtteranceId = String.valueOf(getPendingText().size() - 1);
            if (!mUseBluetoothIfAvailable && !mManager.isSpeakerphoneOn()) {
                mDisableSpeakerPhone = true;
                mManager.setSpeakerphoneOn(true);
            }
            for (int i = 0; i < getPendingText().size(); i++) {
                mTtsParams.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, String.valueOf(i));
                mTts.speak(getPendingText().get(i), TextToSpeech.QUEUE_ADD, mTtsParams);
            }
        }
    }

    private void checkVolume() {
        int curVolume = mManager.getStreamVolume(mStream);

        if (curVolume == 0) {
            logd("checkVolume setting volume of stream " + String.valueOf(mStream) + " to max.");
            mManager.setStreamVolume(mStream, mManager.getStreamMaxVolume(mStream), 0);
        }

        if (mRequestAudioFocus) {
            logd("Requesting audio focus on stream " + mStream);
            int request = mManager.requestAudioFocus(null, mStream, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            logd("audio focus request results: " + request);
        }
    }

    private void restoreVolume() {
        if (mRequestAudioFocus) {
            abandonAudioFocus();
        }

        if (!mUseBluetoothIfAvailable && mDisableSpeakerPhone) {
            mManager.setSpeakerphoneOn(false);
        }
    }

    public void requestAudioFocus(boolean request) {
        mRequestAudioFocus = request;
    }

    public void abandonAudioFocus() {
        if (mManager != null) {
            int request = mManager.abandonAudioFocus(null);
            logd("audio focus abandon results: " + request);
        }
    }

    public void shutDownNow() {
        stopSpeaking();
        checkShutdown();

        if (mListener != null) {
            mListener.speakingCompleted(RESULT_OK);
        }
    }

    public interface SpeakerListener {
        public void speakingCompleted(int status);
    }
}


