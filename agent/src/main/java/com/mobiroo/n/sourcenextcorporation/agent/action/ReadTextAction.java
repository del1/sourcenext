package com.mobiroo.n.sourcenextcorporation.agent.action;

import android.content.Intent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.SpeakTextService;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.TextSpeaker;
import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.MeetingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.receiver.SmsBroadcastReceiver;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.TelephonyUtils;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.item.SleepAgent;
import com.mobiroo.n.sourcenextcorporation.agent.service.ReadSmsService;
import com.mobiroo.n.sourcenextcorporation.agent.util.SmsMessageWrapper;

import java.util.HashMap;
import java.util.Locale;

public class ReadTextAction extends BaseAction {

    public static final String DO_NOT_READ_SENDER = "DO_NOT_READ_SENDER";

    @Override
    protected void performActions(int operation, int triggerType, Object extraInfo) {
        if (operation == OPERATION_UNTRIGGER) {
            return;
        }

        HashMap<String, String> agentPrefs = mAgent.getPreferencesMap();

        if (!Boolean.parseBoolean(agentPrefs.get(AgentPreferences.SMS_READ_ALOUD))) {
            return;
        }

        //TODO: this is a hack; figure out better way to avoid reading text if MA or SA are on
        if (DbAgent.isActive(mContext, MeetingAgent.HARDCODED_GUID) || (DbAgent.isActive(mContext, SleepAgent.HARDCODED_GUID))) {
            Logger.d("Not reading text aloud because meeting agent or sleep agent are active.");
            return;
        }

        // do not read aloud SMS if phone is ringing or in call
        if (Utils.isOnPhoneCall(mContext)) {
            Logger.d("Phone is not idle; skipping read aloud but playing notification sound instead.");
            Utils.playNotificationSound(mContext);
            return;
        }

        boolean doVoiceResponse = Boolean.parseBoolean(agentPrefs.get(AgentPreferences.SMS_READ_ALOUD_VOICE_RESPONSE));

        SmsMessageWrapper smw = (SmsMessageWrapper) extraInfo;
        String messageBody = smw.message;
        String originatingAddress = smw.number;

        String contactName = TelephonyUtils.contactNameFromPhone(mContext, originatingAddress);
        if ((contactName == null) || (contactName.trim().equals(""))) {
            contactName = TextUtils.join(" ", originatingAddress.split(""));
        }

        boolean useBluetoothIfAvailable = !Boolean.parseBoolean(mAgent.getPreferencesMap().get(AgentPreferences.SMS_READ_USING_SPEAKERPHONE));
        boolean respondWithHeadset = Boolean.parseBoolean(mAgent.getPreferencesMap().get(AgentPreferences.SMS_RESPOND_WITH_HEADSET));
        boolean includeSender = TelephonyUtils.readCurrentSender(mContext, contactName);

        if (includeSender) {
            TelephonyUtils.storeLastSmsSender(mContext, contactName);
        }

        if (doVoiceResponse
                && !SmsBroadcastReceiver.MMS_BODY_PLACEHOLDER.equals(messageBody)
                && isValidTtsLocale()
                && SpeechRecognizer.isRecognitionAvailable(mContext)) {
            Intent intent = new Intent(mContext, ReadSmsService.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(TextSpeaker.EXTRA_SENDER, String.format(mContext.getString(R.string.incoming_sms_sender), contactName));
            intent.putExtra(TextSpeaker.EXTRA_MESSAGE, messageBody);
            intent.putExtra(TextSpeaker.EXTRA_NUMBER, smw.number);
            intent.putExtra(TextSpeaker.EXTRA_USE_BLUETOOTH_IF_AVAILABLE, useBluetoothIfAvailable);
            intent.putExtra(TextSpeaker.EXTRA_RESPOND_WITH_HEADSET, respondWithHeadset);
            Logger.d("Reading SMS with response");
            mContext.startService(intent);
        } else {
            Intent intent = new Intent(mContext, SpeakTextService.class);
            intent.putExtra(TextSpeaker.EXTRA_SENDER, includeSender ? String.format(mContext.getString(R.string.incoming_sms_sender), contactName) : DO_NOT_READ_SENDER);
            intent.putExtra(TextSpeaker.EXTRA_MESSAGE, messageBody);
            intent.putExtra(TextSpeaker.EXTRA_NUMBER, smw.number);
            intent.putExtra(TextSpeaker.EXTRA_USE_BLUETOOTH_IF_AVAILABLE, useBluetoothIfAvailable);
            intent.putExtra(TextSpeaker.EXTRA_RESPOND_WITH_HEADSET, respondWithHeadset);
            Logger.d("Reading SMS with no response");
            mContext.startService(intent);
        }
    }

    public boolean isValidTtsLocale() {
        Locale current = Locale.getDefault();

        return current.equals(Locale.ENGLISH)
                || current.equals(Locale.UK)
                || current.equals(Locale.US);
    }

}
