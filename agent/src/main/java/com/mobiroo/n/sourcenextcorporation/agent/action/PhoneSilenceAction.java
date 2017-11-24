package com.mobiroo.n.sourcenextcorporation.agent.action;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;

import java.util.HashMap;

public class PhoneSilenceAction extends BaseAction {

    private static MediaPlayer sMediaPlayer;

    // NOTE: do not include AudioManager.STREAM_ALARM or AudioManager.STREAM_MUSIC
    // TODO: make it an option to disable STREAM_DTMF, STREAM_VOICE_CALL, STREAM_ALARM and STREAM_MUSIC as well
    protected static final int[] mStreams = {AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_RING, AudioManager.STREAM_SYSTEM};

    private static final String TRIGGER_REQUEST_ID = "0";


    @SuppressLint("UseSparseArrays")
    @Override
    protected void performActions(int operation, int triggerType, Object extraInfo) {
        Logger.i("perfoming PhoneSilenceAction operation " + String.valueOf(operation));

        if (operation == OPERATION_TRIGGER) {
            PhoneSilenceAction.minimizeVolumes(mContext, mAgentGuid, TRIGGER_REQUEST_ID);
        } else {
            PhoneSilenceAction.resetVolumes(mContext, mAgentGuid, TRIGGER_REQUEST_ID);
        }
    }


    @SuppressLint("UseSparseArrays")
    public static void maximizeVolumes(Context context, String agentGuid, String requestId) {
        SettingsButler sb = new SettingsButler(context);
        sb.setVolumes(agentGuid, requestId, sb.getAlertVolumes(context, mStreams, agentGuid, requestId), AudioManager.RINGER_MODE_NORMAL);
    }

    @SuppressLint("UseSparseArrays")
    private static void minimizeVolumes(Context context, String agentGuid, String requestId) {
        HashMap<Integer, Integer> streamVolumes = new HashMap<Integer, Integer>(5);
        for (int stream : mStreams) {
            streamVolumes.put(Integer.valueOf(stream), Integer.valueOf(0));
        }
        SettingsButler sb = new SettingsButler(context);

        Agent agent = AgentFactory.getAgentFromGuid(context, agentGuid);
        HashMap<String, String> agentPrefs = agent.getPreferencesMap();

        sb.setVolumes(agentGuid, requestId, streamVolumes, Utils.convertBooleanToRingerType(Boolean.parseBoolean(agentPrefs.get(AgentPreferences.SOUND_SILENCE_DEVICE))));
    }

    @SuppressLint("UseSparseArrays")
    public static void resetVolumes(Context context, String agentGuid, String requestId) {
        // do not lower existing volumes for regular PhoneSilenceAction triggered
        // this will prevent lowering volumes on reset for volumes
        // that user has manually changed while an agent was active
        boolean doNotLowerVolumes = TRIGGER_REQUEST_ID.equals(requestId);

        SettingsButler sb = new SettingsButler(context);
        sb.resetVolumes(agentGuid, requestId, doNotLowerVolumes);
    }

    public static void startRinging(Context context) {
        Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        sMediaPlayer = MediaPlayer.create(context, ringtone);
        sMediaPlayer.start();
    }

    public static void stopRinging() {
        if(sMediaPlayer != null && sMediaPlayer.isPlaying()) {
            sMediaPlayer.stop();
        }
    }

}
