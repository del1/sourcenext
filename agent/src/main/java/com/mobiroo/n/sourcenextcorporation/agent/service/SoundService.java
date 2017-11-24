package com.mobiroo.n.sourcenextcorporation.agent.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.service.driving.items.Complex;
import com.mobiroo.n.sourcenextcorporation.agent.service.driving.items.FFT;
import com.mobiroo.n.sourcenextcorporation.agent.service.driving.lists.SoundDataList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by krohnjw on 5/27/2014.
 */
public class SoundService extends Service {

    public static final String EXTRA_DATA = "values";
    public static final String EXTRA_IN_CAR = "in_car";

    public static final String ACTION_STOP = "com.mobiroo.n.sourcenextcorporation.agent.soundservice.stopService";
    public static final String ACTION_DETECTED_VALUE = "com.mobiroo.n.sourcenextcorporation.agent.soundservice.detected";

    private static AudioRecord mAudioInput;
    private static short[] mAudioBuffer;
    private static int mAudioBufferSize;

    final static int SAMPLE_RATE = 8000;
    final static int CHANNELS = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    final static int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    static boolean mReadInput = true;

    static int bufferReadResult = 0;

    public static final String DIR_NAME = "ego_sound";
    public static final String FILE_NAME = "data.txt";

    private static File log;
    private static FileWriter writer;
    private static BufferedWriter out;

    private static SoundDataList mSoundData;

    private static boolean mLogResults = false;

    public static synchronized SoundDataList getDataQueue() {
        if (mSoundData == null) {
            mSoundData = new SoundDataList();
        }
        return mSoundData;
    }

    private synchronized boolean getReadInput() {
        return mReadInput;
    }

    private synchronized void setReadInput(boolean read) {
        mReadInput = read;
    }

    private Runnable mRunnable;
    private Handler mHandler;

    private SoundController mSoundTask;
    private boolean mSamplingSound = false;

    @Override
    public void onCreate() {
        super.onCreate();

        log("Service being created");
        mSoundData = new SoundDataList();
        // Open file here
        File root = Environment.getExternalStorageDirectory();
        File container = new File(root.getPath() + "/" + DIR_NAME + "/");
        container.mkdirs();
        log = new File(container, FILE_NAME);
        try {
            log.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            writer = new FileWriter(log, true);
            out = new BufferedWriter(writer);
        } catch (Exception e) {
            log("Error getting buffered writer: " + e.toString());
            e.printStackTrace();
        }

    }

    private static void write(String message) {
        if (mLogResults) {
            try {
                out.write(message + "\n");
                out.flush();
                writer.flush();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mSamplingSound) {
            if (mSoundTask != null) {
                mSoundTask.cancel(true);
            }
            stopSoundSampling();
        }

        try {
            if (out != null) {
                out.flush();
            }
            if (writer != null) {
                writer.flush();
            }
            if (out != null) {
                out.close();
            }
            if (writer != null) {
                writer.close();
            }
            out = null;
            writer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        mSoundData = null;
        mAudioInput = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("on start command called");
        registerReceiver(stop, new IntentFilter(ACTION_STOP));

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                stopSoundSampling();
            }
        };
        mHandler.postDelayed(mRunnable, 2500);

        mSoundTask = new SoundController();
        mSoundTask.execute(1);

        return START_STICKY;
    }

    private class SoundController extends AsyncTask<Integer, Void, Void> {


        @Override
        public void onPreExecute() {
            log("Sound sampling beginning");
            mSamplingSound = true;
        }

        @Override
        protected Void doInBackground(Integer... ints) {
            startRecording(this);
            return null;
        }

        @Override
        public void onPostExecute(final Void unused) {
            log("Sound sampling finished");
            mSamplingSound = false;
        }

        @Override
        protected void onCancelled() {
            log("Cancelling Sound sampling");
            mSamplingSound = false;
            mAudioInput = null;
        }
    }

    private void startRecording(SoundController task) {

        mAudioBufferSize = 4096; //AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, ENCODING);
        mAudioBuffer = new short[mAudioBufferSize];

        mAudioInput = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, CHANNELS, ENCODING, mAudioBufferSize);
        log("Audio state is " + mAudioInput.getRecordingState() + "," + mAudioInput.getState());
        log("Calling startRecording");

        if ((mAudioInput.getState() == AudioRecord.STATE_INITIALIZED)
                && (mAudioInput.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)) {
            setReadInput(true);
            try {
                mAudioInput.startRecording();
            } catch (Exception e) {
                // Exit as we could not start recording
                log("Exception starting recording: " + e);
                setReadInput(false);
                mAudioInput = null;
                stopService();
                return;
            }
        } else {
            // Exit as we could not get a valid recorder
            log("Recording in progress or device could not be initialized");
            setReadInput(false);
            mAudioInput = null;
            stopService();
            return;
        }

        boolean wroteFreq = false;
        int readCount = 0;
        final int MAX_READ_COUNT = 10;

        while (getReadInput() && !task.isCancelled()) {

            if (mAudioInput == null) {
                setReadInput(false);
                break;
            }

            bufferReadResult = mAudioInput.read(mAudioBuffer, 0, mAudioBufferSize);
            log("Reading audio buffer: result= " + bufferReadResult + " size is " + mAudioBufferSize);

            readCount += 1;

            if (readCount > MAX_READ_COUNT) {
                setReadInput(false);
                stopSoundSampling();
                break;
            }

            Complex[] complexData = new Complex[mAudioBuffer.length];
            for (int i = 0; i < complexData.length; i++) {
                complexData[i] = new Complex(mAudioBuffer[i], 0);
            }

            Complex[] fftResult = FFT.fft(complexData);
            try {
                getDataQueue().add(fftResult);
            } catch (Exception ignored) {
            }

            if (mLogResults) {
                log("Have " + fftResult.length + " + results");
                StringBuilder sb = new StringBuilder();
                StringBuilder f = new StringBuilder();

                for (int i = 0; i < fftResult.length; i += 2) {
                    Complex c = fftResult[i];
                    double magnitude = Math.sqrt(c.re() * c.re() + c.im() * c.im()); // Magnitude is the amplitude
                    magnitude = magnitude / 1000;
                    // Freq = i/2 * SAMPLE_RATE / bytes.lengh/2
                    long freq = (i / 2) * SAMPLE_RATE / (fftResult.length); // Hz
                    //Logger.d("\n" + c.re() + "\t" + c.im() + "\t" + magnitude + "\t" + freq);
                    if (!wroteFreq) {
                        f.append(freq + "\t");
                    }
                    sb.append(magnitude + "\t");
                }
                if (!wroteFreq) {
                    write(f.toString());
                    wroteFreq = true;
                }
                write(sb.toString());
            }
        }

    }

    private BroadcastReceiver stop = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            log("Stop recording broadcast");
            stopSoundSampling();
            stopService();
        }
    };

    private void stopSoundSampling() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }

        if ((mSamplingSound) && (mSoundTask != null)) {
            mSoundTask.cancel(true);
        }

        try {
            setReadInput(false);
            if (mAudioInput != null) {
                mAudioInput.stop();
                mAudioInput.release();
                mAudioInput = null;
            }

            if (mSoundData != null) {
                SoundDataList.Analysis a = mSoundData.isInCar();
                log("Sending broadcast indicating driving " + a.driving);
                sendBroadcast(new Intent(ACTION_DETECTED_VALUE).putExtra(EXTRA_DATA, a.data).putExtra(EXTRA_IN_CAR, a.driving));
                write("is in car? " + a.driving);
                write(a.data);
            } else {
                log("Sending broadcast indicating driving " + false);
                sendBroadcast(new Intent(ACTION_DETECTED_VALUE).putExtra(EXTRA_DATA, new String()).putExtra(EXTRA_IN_CAR, false));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void stopService() {
        try {
            unregisterReceiver(stop);
        } catch (Exception e) {
            log("Error unregistering receiver: " + e.toString());
        }
        stopSelf();
    }

    private static void log(String message) {
        Logger.d("SoundService: " + message);
    }

    private static final long MIN_INTERVAL_TIME = 30 * 1000;
    private static final String PREF_LAST_RUN = "com.mobiroo.n.sourcenextcorporation.agent.soundService.last_run";

/*    public static void requestSoundSampling(Context context) {
        String last = PrefsHelper.getPrefString(context, PREF_LAST_RUN, "0");
        if (last.isEmpty()) {
            last = "0";
        }

        // Only start sampling if we have not in the last 30 seconds
        if ((System.currentTimeMillis() - Long.parseLong(last)) > MIN_INTERVAL_TIME) {
            log("Starting sound sampling service");
            context.startService(new Intent(context, SoundService.class));
        }
    }

    public static void cancelSoundSampling(Context context) {
        context.stopService(new Intent(context, SoundService.class));
    }*/
}