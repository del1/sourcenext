package com.mobiroo.n.sourcenextcorporation.agent.fragment;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.AppSpecific;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.TextSpeaker;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.TextSpeaker.SpeakerListener;
import com.mobiroo.n.sourcenextcorporation.agent.util.TelephonyUtils;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SmsReplyFragment extends Fragment implements RecognitionListener {

    private int ACTION = 0;
    private final int ACTION_START_LISTENING = 1;
    private final int ACTION_STOP_LISTENING = 2;
    private final int ACTION_REQUEST_MESSAGE_BODY = 4;
    private final int ACTION_CONFIRM_MESSAGE = 5;
    private final int ACTION_CANCEL = 7;
    private final int ACTION_REPEAT = 8;
    private final int ACTION_REQUEST_REPEAT_MESSAGE = 9;
    private final int ACTION_QUIT = 10;
    private final int ACTION_CANT_UNDERSTAND = 11;
    private final int ACTION_REQUEST_REPEAT_CONFIRMATION = 12;
    private final int ACTION_NO_ACTION = -1;

    private int LISTENING_REQUEST = 0;
    private final int REQUEST_USER_WANTS_TO_REPLY = 1;
    private final int REQUEST_MESSAGE_FROM_USER = 2;
    private final int REQUEST_CONFIRM_MESSAGE = 3;

    private int mNumRetries = 0;
    private int mErrorRetries = 0;
    private final int MAX_RETRIES = 1;

    private SpeechRecognizer mRecognizer;
    private Bundle mArgs;
    private TextSpeaker mSpeaker;
    private boolean mIsListening;
    private boolean mIsSpeaking;


    private long mListeningStart;
    private final long MIN_LISTENING_TIME_MS = 300;
    private int mListeningRaceHandlerCount = 0;

    private Handler mHandler;

    private String mMessageBody;
    private String mAddress;

    private TextView mResponse;
    private TextView mInstructions;
    private TextView mHeading;
    private ImageView mMicrophone;

    private AudioManager mManager;

    private String MESSAGE_CANCEL = "Say CANCEL to quit";

    private int mStream;

    private boolean mUseHeadsetToRespond = false;

    private BluetoothHeadset mHeadsetProfile;
    private BluetoothDevice mHeadsetDevice;

    final ArrayList<String> mSendMatches = new ArrayList<String>() {
        {
            add("text");
            add("send text");
            add("send texts");
            add("send a text");
        }
    };

    final ArrayList<String> mYesMatches = new ArrayList<String>() {
        {
            add("yes");
            add("ya");
            add("yeah");
        }
    };

    final ArrayList<String> mNoMatches = new ArrayList<String>() {
        {
            add("no");
            add("nah");
            add("know");
        }
    };

    final ArrayList<String> mCancelMatches = new ArrayList<String>() {
        {
            add("cancel");
            add("cancel cancel");
        }
    };

    private class MyListener implements SpeakerListener {

        @Override
        public void speakingCompleted(int status) {
            logd("Speaking complete: " + status);

            if (mIsSpeaking) {
                mIsSpeaking = false;
                if (!mIsListening) {
                    logd("Not listening, dispatching action " + ACTION);
                    mHandler.sendMessage(mHandler.obtainMessage(ACTION));
                } else {
                    logd("Currently listening, not acting");
                }
            }
        }
    }

    private int getStream() {
        mStream = AudioManager.STREAM_MUSIC;

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            if (adapter.getProfileConnectionState(BluetoothProfile.A2DP) == BluetoothProfile.STATE_CONNECTED) {
                Logger.d("TTS TextSpeaker: Bluetooth A2dp device connected");
            } else {
                if (adapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED) {
                    Logger.d("TTS TextSpeaker: Bluetooth headset connected");
                    mStream = AudioManager.STREAM_VOICE_CALL;
                }
            }
        }

        return mStream;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sms_reply, null);
        ((TextView) view.findViewById(R.id.dialog_title)).setText(R.string.sms_reply_title);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mResponse = (TextView) getView().findViewById(R.id.response_text);
        mHeading = (TextView) getView().findViewById(R.id.heading);
        mInstructions = (TextView) getView().findViewById(R.id.instruction);
        mMicrophone = (ImageView) getView().findViewById(R.id.indicator);

        mManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

        String message = "";
        String from = "";

        if (mArgs != null) {
            message = mArgs.getString(TextSpeaker.EXTRA_MESSAGE);
            from = mArgs.getString(TextSpeaker.EXTRA_SENDER);
            mAddress = mArgs.getString(TextSpeaker.EXTRA_NUMBER);
        }

        showHeading("Incoming SMS");
        showResponse(message);
        showInstructions(MESSAGE_CANCEL);

        getStream();
        requestAudioFocus();

        if ((message != null) && !message.isEmpty()) {

            if ((from == null) || from.isEmpty()) {
                from = getString(AppSpecific.R_INCOMING_SMS);
            }

            // Read the incoming message
            mNumRetries = 0;
            mErrorRetries = 0;
            LISTENING_REQUEST = REQUEST_USER_WANTS_TO_REPLY;
            ACTION = ACTION_START_LISTENING;
            speakText(from, message, "To reply say SEND TEXT");
            Usage.logEvent(getActivity(), Usage.Events.SMS_READ_WITH_VOICE, true);
        } else {
            logd("Calling finish listening");
            finishListening();
        }

    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mNumRetries = 0;
        mArgs = getArguments();

        boolean useBTIfAvailable = mArgs == null || mArgs.getBoolean(TextSpeaker.EXTRA_USE_BLUETOOTH_IF_AVAILABLE, true);

        //mUseHeadsetToRespond = mArgs.getBoolean(TextSpeaker.EXTRA_RESPOND_WITH_HEADSET, false);
        logd("Using headset? " + mUseHeadsetToRespond);
        mSpeaker = new TextSpeaker(getActivity(), false, useBTIfAvailable);

        mSpeaker.requestAudioFocus(false);
        mSpeaker.setListener(new MyListener());
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg == null) {
                    logd("Null msg in handle msg");
                    return;
                }
                logd("Handling message " + msg.what);

                switch (msg.what) {
                    case ACTION_START_LISTENING:
                        startListeningWrapper();
                        break;
                    case ACTION_STOP_LISTENING:
                        stopListening();
                        break;
                    case ACTION_REQUEST_MESSAGE_BODY:
                        promptUserForMessage();
                        break;
                    case ACTION_REQUEST_REPEAT_MESSAGE:
                        promptRepeatMessage();
                        break;
                    case ACTION_CONFIRM_MESSAGE:
                        promptUserConfirmMessage();
                        break;
                    case ACTION_REPEAT:
                        promptUserRepeatResponse();
                        break;
                    case ACTION_CANT_UNDERSTAND:
                        speakCantUnderstand();
                        break;
                    case ACTION_REQUEST_REPEAT_CONFIRMATION:
                        promptUserRepeatConfirmation();
                        break;
                    case ACTION_CANCEL:
                    case ACTION_QUIT:
                        logd("Calling finish listening");
                        finishListening();
                        break;

                }

            }
        };

    }

    @TargetApi(19)
    private void requestAudioFocus() {
        logd("Requesting audio focus on stream " + mStream);
        int request = mManager.requestAudioFocus(null, mStream, (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                ? AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                : AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

        logd("Request audio focus result: " + request);
    }

    private SpeechRecognizer getRecognizer() {
        return getRecognizer(false);
    }

    private SpeechRecognizer getRecognizer(boolean createNew) {
        if (createNew && mRecognizer != null) {
            mRecognizer.destroy();
            mRecognizer = null;
        }

        // getActivity might return null if this fragment is no longer attached to an activity
        // or the attached is finishing.
        if (mRecognizer == null && getActivity() != null) {
            mRecognizer = SpeechRecognizer.createSpeechRecognizer(getActivity());
            mRecognizer.setRecognitionListener(this);
        }

        return mRecognizer;
    }

    private void startListeningWrapper() {
        mListeningStart = Calendar.getInstance().getTimeInMillis();

        if (mUseHeadsetToRespond && false) {
            startBluetoothListening();
        } else {
            logd("Start Listening called in wrapper");
            startListening();
        }
    }

    private void startListening() {
        mIsListening = true;
        logd("Start listening called");
        SpeechRecognizer speechRecognizer = getRecognizer(true);
        if (speechRecognizer != null) speechRecognizer.startListening(buildRecognizerIntent());
    }

    private void stopListening() {
        mIsListening = false;
        getRecognizer().cancel();
    }

    private void promptUserForMessage() {
        Logger.d("Prompting user for message!");

        mErrorRetries = 0; // Reset speech retries
        String message = "What would you like to say";
        showHeading(message);
        showResponse("");
        showInstructions(MESSAGE_CANCEL);
        LISTENING_REQUEST = REQUEST_MESSAGE_FROM_USER;
        ACTION = ACTION_START_LISTENING;
        speakText(message);
    }

    private void promptUserRepeatResponse() {
        String message = "I'm sorry, I didn't catch that.  Try one more time.";
        showHeading(message);
        showResponse("");
        showInstructions(MESSAGE_CANCEL);
        speakText(message);
    }

    private void promptUserConfirmMessage() {
        mNumRetries = 0; // Reset retries here
        showHeading("I think you said");
        showResponse(mMessageBody);
        showInstructions("Say YES to send or NO to re-record");
        LISTENING_REQUEST = REQUEST_CONFIRM_MESSAGE;
        ACTION = ACTION_START_LISTENING;
        speakText("I think you said, ", mMessageBody, "Is this correct?");
    }

    private void promptRepeatMessage() {
        String message = "Sorry about that, try one more time.";
        showHeading(message);
        showResponse("");
        showInstructions(MESSAGE_CANCEL);
        ACTION = ACTION_START_LISTENING;
        LISTENING_REQUEST = REQUEST_MESSAGE_FROM_USER;
        speakText(message);
    }

    private void speakSendingMessage() {
        String message = "Great, sending your message for you.";
        showHeading(message);
        showResponse("");
        showInstructions("");
        speakText(message);
    }


    private void promptUserRepeatConfirmation() {
        String message = "I'm sorry, I didn't catch that.  Try one more time.";
        showHeading(message);
        showResponse("");
        showInstructions("Say YES to send or NO to re-record");
        LISTENING_REQUEST = REQUEST_CONFIRM_MESSAGE;
        ACTION = ACTION_START_LISTENING;
        speakText(message);
    }

    private void speakCantUnderstand() {
        String message = "Sorry, it seems like I can't understand you right now";
        showHeading(message);
        showResponse("");
        showInstructions("");
        ACTION = ACTION_QUIT;
        speakText(message);
    }

    private void finishListening() {
        ACTION = ACTION_NO_ACTION;
        if (mIsListening) {
            logd("Stopping listening");
            getRecognizer().cancel();
        }

        if (mSpeaker != null) {
            logd("Shutting down speaker");
            mSpeaker.shutDownNow();
        }


        if ((getActivity() != null) && (!getActivity().isFinishing())) {
            logd("Finishing reading activity");
            getActivity().finish();
        }
    }

    private void showHeading(String message) {
        mHeading.setText(message);
    }

    private void showInstructions(String message) {
        mInstructions.setText(message);
    }

    private void showResponse(String message) {
        mResponse.setText(message);
    }

    private void speakText(String... text) {
        if ((text != null) && (text.length > 0)) {
            mIsSpeaking = true;
            mSpeaker.clearText();
            for (String m : text) {
                if (!m.isEmpty()) {
                    mSpeaker.addText(m);
                }
            }
            mSpeaker.speak();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mUseHeadsetToRespond) {
            stopBluetoothListening();
        }
        getRecognizer().cancel();
        getRecognizer().destroy();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        int request = mManager.abandonAudioFocus(null);
        logd("Abandoning audio focus result: " + request);

        if (mSpeaker != null) {
            mSpeaker.clearText();
            mSpeaker.stopSpeaking();
            mSpeaker.shutDownNow();
            mSpeaker.destroy();
        }

    }

    private Intent buildRecognizerIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra("android.speech.extra.DICTATION_MODE", true);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.mobiroo.n.sourcenextcorporation.agent");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        if (mUseHeadsetToRespond) {
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000);
        }
        return intent;
    }

    private void logd(String message) {
        Logger.d("SMS-Reply: " + message);
    }

    @Override
    public void onBeginningOfSpeech() {
        logd("Speech started");
    }

    @Override
    public void onBufferReceived(byte[] arg0) {
        logd("Received buffer");
    }

    @Override
    public void onEndOfSpeech() {
        mMicrophone.setImageResource(R.drawable.img_mic_black);
        mIsListening = false;
        logd("End of speech");
    }

    @Override
    public void onError(int error) {
        mMicrophone.setImageResource(R.drawable.img_mic_black);
        logd("Speech error " + error);
        // Find out if we have been listening long enough to get a real response, if not re-init
        if (Calendar.getInstance().getTimeInMillis() - mListeningStart < MIN_LISTENING_TIME_MS && mListeningRaceHandlerCount < 2) {
            mListeningRaceHandlerCount++;
            logd("Restarting as not enough time has passed");
            mHandler.dispatchMessage(mHandler.obtainMessage(ACTION));
            return;
        }

        mIsListening = false;
        mListeningRaceHandlerCount = 0;

        switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH:
                logd("no matches");
                if (mErrorRetries < MAX_RETRIES) {
                    mHandler.dispatchMessage(mHandler.obtainMessage(ACTION_REPEAT));
                    mErrorRetries++;
                } else {
                    logd("Exceeded max retries, exiting");
                    ACTION = ACTION_CANT_UNDERSTAND;
                    mHandler.dispatchMessage(mHandler.obtainMessage(ACTION));
                }
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                logd("TIMEOUT - exiting");
                mHandler.dispatchMessage(mHandler.obtainMessage(ACTION_QUIT));
                break;

        }
    }

    @Override
    public void onEvent(int type, Bundle args) {
        logd("Got event " + type);
    }

    @Override
    public void onPartialResults(Bundle results) {
        logd("Partial results");
        ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        for (String s : data) {
            logd("TEXT: " + s);
            if (mCancelMatches.contains(s)) {
                mHandler.dispatchMessage(mHandler.obtainMessage(ACTION_CANCEL));
                return;
            }
        }

    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        logd("Ready for speech");
        mMicrophone.setImageResource(R.drawable.img_mic_red);
    }

    @Override
    public void onResults(Bundle results) {
        mMicrophone.setImageResource(R.drawable.img_mic_black);
        mIsListening = false;
        mListeningRaceHandlerCount = 0;
        logd("Got results");
        ArrayList<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        if (data == null) {
            logd("Results object is null.  Cancelling response and returning");
            mHandler.dispatchMessage(mHandler.obtainMessage(ACTION_CANCEL));
            return;
        }

        for (String s : data) {
            logd("TEXT: " + s);
            if (mCancelMatches.contains(s)) {
                mHandler.dispatchMessage(mHandler.obtainMessage(ACTION_CANCEL));
                return;
            }
        }


        switch (LISTENING_REQUEST) {
            case REQUEST_USER_WANTS_TO_REPLY:
                logd("Got init response");

                for (String s : data) {
                    if (mSendMatches.contains(s)) {
                        mErrorRetries = 0; // Reset speech retries
                        logd("Asking user what text they would like to send");
                        ACTION = ACTION_REQUEST_MESSAGE_BODY;
                        mHandler.dispatchMessage(mHandler.obtainMessage(ACTION));
                        return;
                    }
                }

                /* If we haven't returned then we have an
                    invalid response.  Ask the user to repeat unless
                    we have already done so, then exit.
                 */

                logd("Got back invalid entry, asking to repeat");
                if (mNumRetries < MAX_RETRIES) {
                    mNumRetries++;
                    logd("Retrying for valid response: " + mNumRetries);
                    LISTENING_REQUEST = REQUEST_USER_WANTS_TO_REPLY;
                    mHandler.dispatchMessage(mHandler.obtainMessage(ACTION_REPEAT));
                } else {
                    logd("Exceeded max retries, exiting");
                    ACTION = ACTION_CANT_UNDERSTAND;
                    mHandler.dispatchMessage(mHandler.obtainMessage(ACTION));
                }

                break;
            case REQUEST_MESSAGE_FROM_USER:

                logd("Got message results");
                if (data.get(0).length() > 0) {
                    mErrorRetries = 0; // Reset speech retries
                    mMessageBody = data.get(0);
                    ACTION = ACTION_CONFIRM_MESSAGE;
                    mHandler.dispatchMessage(mHandler.obtainMessage(ACTION));
                }
                break;
            case REQUEST_CONFIRM_MESSAGE:

                logd("Got send results");
                for (String s : data) {
                    if (mYesMatches.contains(s)) {
                        logd("Sending message");
                        ACTION = ACTION_QUIT;
                        TelephonyUtils.sendSMS(mMessageBody, mAddress, getActivity());
                        speakSendingMessage();
                        Usage.logEvent(getActivity(), Usage.Events.SMS_SENT_WITH_VOICE_RESPONSE, true);
                        return;
                    } else if (mNoMatches.contains(s)) {
                        logd("Asking user to repeat message");
                        ACTION = ACTION_REQUEST_REPEAT_MESSAGE;
                        mHandler.dispatchMessage(mHandler.obtainMessage(ACTION));
                        return;
                    }
                }

                /* If we haven't returned we received some text
                    input but it was not a match to the yes or no group
                 */

                if (mNumRetries < MAX_RETRIES) {
                    logd("Retrying for valid response");
                    mNumRetries++;
                    ACTION = ACTION_REQUEST_REPEAT_CONFIRMATION;
                    mHandler.dispatchMessage(mHandler.obtainMessage(ACTION));
                } else {
                    logd("Exceeded max retries, exiting");
                    ACTION = ACTION_CANT_UNDERSTAND;
                    mHandler.dispatchMessage(mHandler.obtainMessage(ACTION));
                }

                break;
        }

    }

    @Override
    public void onRmsChanged(float arg0) {
        // logd("RMS changed " + arg0);
    }

    private BroadcastReceiver mScoUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int mode = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);
            logd("SCO mode is " + mode);
            if (mode == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
                logd("Starting listening");
                startListening();
            }
        }
    };

    private void startBluetoothListening() {
        // TODO: Add a check here to verify we have a headset device connected.
        if (mManager.isBluetoothScoAvailableOffCall() && !mManager.isBluetoothScoOn()) {
            logd("Starting Bluetooth SCO");
            getActivity().registerReceiver(mScoUpdatedReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED));
            mManager.startBluetoothSco();
            mManager.setBluetoothScoOn(true);
        }
    }

    private void stopBluetoothListening() {
        if (mManager.isBluetoothScoOn()) {
            mManager.stopBluetoothSco();
            mManager.setBluetoothScoOn(false);
            getActivity().unregisterReceiver(mScoUpdatedReceiver);
        }
    }

    private void startBluetoothListeningOld() {
        if (mHeadsetDevice != null) {
            mHeadsetProfile.startVoiceRecognition(mHeadsetDevice);
            startListening();
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.getProfileProxy(getActivity(),
                new BluetoothProfile.ServiceListener() {

                    @Override
                    public void onServiceConnected(int profile, BluetoothProfile proxy) {
                        mHeadsetProfile = (BluetoothHeadset) proxy;

                        List<BluetoothDevice> devices = mHeadsetProfile.getConnectedDevices();
                        logd("Found " + devices.size() + " devices");
                        for (BluetoothDevice d : devices) {
                            logd("Found : " + d.getName());
                        }

                        if (devices.size() > 0) {
                            mHeadsetDevice = devices.get(0);
                            try {
                                boolean start = mHeadsetProfile.startVoiceRecognition(mHeadsetDevice);
                            } catch (Exception e) {
                                Logger.e("Exception : " + e, e);
                            }
                        }

                        startListening();
                    }

                    @Override
                    public void onServiceDisconnected(int profile) {
                        logd("Stopping voice recognition");
                        mHeadsetProfile.stopVoiceRecognition(mHeadsetDevice);
                    }
                },
                BluetoothProfile.HEADSET);
    }
}
