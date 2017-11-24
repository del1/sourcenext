package com.mobiroo.n.sourcenextcorporation.agent.action;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.text.TextUtils;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.SmsMessageWrapper;
import com.mobiroo.n.sourcenextcorporation.agent.util.TelephonyUtils;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.activity.AlarmActivity;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

public class AutorespondSmsAction extends BaseAction {

    @Override
    protected void performActions(int operation, int triggerType, Object extraInfo) {
        if (operation == OPERATION_UNTRIGGER) {
            return;
        }
        HashMap<String, String> agentPrefs = mAgent.getPreferencesMap();

        boolean respondSms = Boolean.parseBoolean(agentPrefs.get(AgentPreferences.SMS_AUTORESPOND));
        boolean respondPhone = Boolean.parseBoolean(agentPrefs.get(AgentPreferences.PHONE_CALL_AUTORESPOND));

        // check for either sms or phone
        // because urgent sms needs to be handled for either
        if (!(respondSms || respondPhone)) {
            Logger.d("SMS and phone response both off.");
            return;
        }

        SmsMessageWrapper smw = (SmsMessageWrapper) extraInfo;
        String originatingAddress = smw.number;

        if (!isAllowedAutorespondContact(agentPrefs, originatingAddress)) {
            return;
        }

        if (handleVerifyUrgentSms(agentPrefs, smw.message)) {
            return;
        }

        if (!respondSms) {
            Logger.d("SMS response is not on.");
            return;
        }

        String statusMessage = mContext.getString(R.string.status_texted_text);

        if (sendVerifyUrgentSms(agentPrefs, originatingAddress, statusMessage)) {
            return;
        }
        if (isVerifyUrgentMode(agentPrefs)) {
            return;
        }

        // do the action action based on mode

        if (agentPrefs.get(AgentPreferences.SMS_AUTORESPOND_MODE).equals(AgentPreferences.AUTORESPOND_MODE_WAKE)) {
            // use 4000ms delay to avoid double beep, i.e. from regular system notification
            SettingsButler sb = new SettingsButler(mContext);
            HashMap<Integer, Integer> volumes = sb.getAlertVolumes(mContext, new int[]{AudioManager.STREAM_NOTIFICATION}, mAgentGuid, "AutorespondSmsAction");
            int notifVolume = Utils.QUERY_MAX_VOLUME;
            if ((volumes != null) && (volumes.containsKey(AudioManager.STREAM_NOTIFICATION))) {
                notifVolume = volumes.get(AudioManager.STREAM_NOTIFICATION).intValue();
            }
            Utils.playNotificationSound(mContext, 4000, notifVolume);
            return;
        }

        if (agentPrefs.get(AgentPreferences.SMS_AUTORESPOND_MODE).equals(AgentPreferences.AUTORESPOND_MODE_RESPOND)) {
            sendSmsResponse(agentPrefs, originatingAddress, statusMessage);
            return;
        }
    }


    // protected functions below: many are also used by subclasses


    // returns true if allowed to autorespond to this address/phone
    protected boolean isAllowedAutorespondContact(HashMap<String, String> agentPrefs, String originatingAddress) {
        //Check for contacts permission
        if (!Utils.isPermissionGranted(mContext, Manifest.permission.READ_CONTACTS)) {
            // User removed the permission from settings. return.
            Utils.postNotification(mContext, new String[] {
                    Manifest.permission.READ_CONTACTS
            });
            return false;
        }

        // go through allowed contact list
        String[] contactsAllowed = agentPrefs.get(AgentPreferences.SMS_AUTORESPOND_CONTACTS).split(AgentPreferences.STRING_SPLIT);
        ArrayList<TelephonyUtils.SimpleContactWrapper> contacts = TelephonyUtils.contactIdsFromPhone(mContext, originatingAddress);
        Logger.d("isAllowedAutorespondContact: contactId = " + (contacts.size() == 0 ? "null" : TextUtils.join(",", contacts)));

        boolean foundContact = false;
        for (String allowedCid : contactsAllowed) {
            if (contacts.size() == 0) {
                // deal with strangers
                if (allowedCid.equals(AgentPreferences.SMS_AUTORESPOND_CONTACT_STRANGERS)) {
                    foundContact = true;
                    break;
                } else {
                    continue;
                }
            }

            if (allowedCid.equals(AgentPreferences.SMS_AUTORESPOND_CONTACT_EVERYONE)) {
                foundContact = true;
                break;
            }

            for (TelephonyUtils.SimpleContactWrapper contact : contacts) {
                if (TextUtils.equals(allowedCid, contact.contactId)) {
                    foundContact = true;
                    break;
                }
            }
        }
        if (!foundContact) {
            Logger.i("Did not find user in allowed autorespond contact list: " + originatingAddress);
        }
        return foundContact;
    }

    // returns true if SMS was sent
    // returns false if we don't send it, e.g. if we've already responded once in agent session
    protected boolean sendSmsResponse(HashMap<String, String> agentPrefs, String originatingAddress, String statusMessage) {
        String response = agentPrefs.get(AgentPreferences.SMS_AUTORESPOND_MESSAGE);
        if (response == null) {
            Logger.i("Null SMS response text; returning.");
            return false;
        }
        response = response.trim();
        if (response.equals("")) {
            Logger.i("Empty SMS response text; returning.");
            return false;
        }

        boolean respondOnlyOnce = Boolean.parseBoolean(agentPrefs.get(AgentPreferences.SMS_AUTORESPOND_ONCE));
        if (respondOnlyOnce) {
            String key = AgentPreferences.SMS_AUTORESPOND_ONCE + "_RespondedAlready";
            String delim = AgentPreferences.STRING_SPLIT;
            SharedPreferences agentSession = mAgent.getSession();
            HashSet<String> respondedTo = Utils.getHashFromString(agentSession.getString(key, ""), delim);

            // make sure +1 and 1 prefixes are treated the same as no prefix
            String modifiedOriginating = originatingAddress.replace("+", "");
            if ((modifiedOriginating.length() == 11) && (modifiedOriginating.charAt(0) == '1')) {
                modifiedOriginating = modifiedOriginating.substring(1);
            }
            Logger.d("AutorespondSmsAction: modifiedOriginating: " + modifiedOriginating);


            String locale = Locale.getDefault().toString();

            // 8 Chars in Australia or Czech republic. For all others allow 8 chars through
            int shortCodeMaximum = (locale.equals("en_AU") || locale.equals("cs_CZ")) ? 8 : 9;

            if (respondedTo.contains(modifiedOriginating)) {
                Logger.d("Already responded to user; don't reply: " + originatingAddress + " -> " + modifiedOriginating);
                return false;
            } else if (modifiedOriginating.length() < shortCodeMaximum) {
                Logger.d("Originating address is " + shortCodeMaximum + " characters or less: " + originatingAddress + ", not responding as this may be a shortcode");
                return false;
            }

            respondedTo.add(modifiedOriginating);
            agentSession.edit().putString(key, Utils.getStringFromHash(respondedTo, delim)).commit();
        }

        TelephonyUtils.sendSMS(response, originatingAddress, mContext);

        String infoMessage = mContext.getString(R.string.status_texted_sub);
        logEvent(originatingAddress, statusMessage, infoMessage);

        return true;
    }


    // send an SMS saying "reply urgent to wake me up" or similar
    protected boolean sendVerifyUrgentSms(HashMap<String, String> agentPrefs, String originatingAddress, String statusMessage) {
        if (isVerifyUrgentMode(agentPrefs)) {
            if (!sendSmsResponse(agentPrefs, originatingAddress, statusMessage)) {
                return false;
            }
            return true;
        }
        return false;
    }

    // wake up phone if in urgent mode and we get SMS with "urgent" in it
    // returns true if wake up is done
    protected boolean handleVerifyUrgentSms(HashMap<String, String> agentPrefs, String messageBody) {
        if (!isVerifyUrgentMode(agentPrefs)) {
            return false;
        }

        String urgentStringLower = mContext.getString(R.string.urgent).toLowerCase();
        String notStringLower = mContext.getString(R.string.not).toLowerCase();

        if (messageBody != null &&
                messageBody.toLowerCase().contains(urgentStringLower) &&
                !messageBody.toLowerCase().contains(notStringLower)) {
            Logger.i("Received Urgent SMS");

            wakeUserUp(mContext.getString(R.string.wake_msg_urgent));
            return true;
        }
        return false;
    }

    protected boolean isVerifyUrgentMode(HashMap<String, String> agentPrefs) {
        return Boolean.parseBoolean(agentPrefs.get(AgentPreferences.SMS_AUTORESPOND_VERIFY_URGENT));
    }

    protected void wakeUserUp(String alertText) {
        Logger.d("wakeUserUp");
        Intent intent = new Intent(mContext, AlarmActivity.class);
        intent.putExtra(AlarmActivity.EXTRA_ALARM_TEXT, alertText);
        intent.putExtra(AlarmActivity.EXTRA_AGENT_GUID, mAgentGuid);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    // log successful sms using this
    protected void logEvent(String originatingAddress, String statusMessage, String infoMessage) {
        Usage.logEvent(mContext, Usage.Events.SMS_SENT, true, new Usage.EventProperty(Usage.Properties.AGENT_NAME, mAgent.getGuid()));

        String sMsg = String.format(statusMessage, mAgent.getName());

        String originator = TelephonyUtils.contactNameFromPhone(mContext, originatingAddress);
        if ((originator == null) || (originator.equals(""))) {
            originator = "(" + originatingAddress + ")";
        }

        String iMsg = String.format(infoMessage, originator);

        TaskDatabaseHelper.logUsageRecord(mContext, mAgent.getGuid(), sMsg, iMsg);
    }

}
