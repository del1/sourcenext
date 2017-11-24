package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.SmsMessageWrapper;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;

public class SmsBroadcastReceiver extends BroadcastReceiver {
	private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private static final String WAP_PUSH_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";

	public static final String MMS_BODY_PLACEHOLDER = "MMS";


	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			if (! ((intent == null) || 
					intent.getAction().equals(SMS_RECEIVED) ||  
					intent.getAction().equals(WAP_PUSH_RECEIVED))) {
				return;
			}

			Logger.d(intent.getAction().equals(SMS_RECEIVED) ? "SMS received" : "MMS received");

			Bundle bundle = intent.getExtras();
			if (bundle == null) {return;}

			SQLiteDatabase db = TaskDatabaseHelper.getInstance(context).getReadableDatabase();
			Cursor c = TaskDatabaseHelper.getEnabledTriggersOfType(db, Constants.TRIGGER_TYPE_SMS);    	
			if (! c.moveToFirst()) {c.close(); return;}


			SmsMessageWrapper[] messages = null;

			if (intent.getAction().equals(SMS_RECEIVED)) {
                if (intent.getBooleanExtra("fakesmsmode", false)) {
                    messages = new SmsMessageWrapper[1];
                    messages[0] = new SmsMessageWrapper(intent.getStringExtra("number"), intent.getStringExtra("message"));
                } else {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    messages = new SmsMessageWrapper[pdus.length];
                    for (int i = 0; i < pdus.length; i++) {
                        SmsMessage s = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        messages[i] = new SmsMessageWrapper(s.getOriginatingAddress(), s.getMessageBody());
                    }
                }
			} else {

				try {
					String incomingData = new String(bundle.getByteArray("data"));
					int typeIndex = incomingData.indexOf("/TYPE");				
					String number = incomingData.substring(0, typeIndex);
					number = number.substring(number.lastIndexOf("+"), number.length());
					Logger.d("MMS number: " + number);

					messages = new SmsMessageWrapper[1];
					messages[0] = new SmsMessageWrapper(number, MMS_BODY_PLACEHOLDER);
				} catch (Exception e) {
					messages = null;
					Logger.e("SmsBroadcastReceiver: Could not parse MMS message");
					e.printStackTrace();
				}
			}

			if ((messages == null) || (messages.length < 1)) {return;}

			do {
				int triggerId = c.getInt(c.getColumnIndex(TaskDatabaseHelper.FIELD_ID));
				String agentGuid = c.getString(c.getColumnIndex(TaskDatabaseHelper.FIELD_GUID));
				if (DbAgent.isActive(context, agentGuid)) {
					Logger.i("SMS Receive Trigger: fired for trigger id: " + String.valueOf(triggerId));
					for (int i=0; i < messages.length; i++) {
						DbAgent.triggerActions(context, agentGuid, Constants.TRIGGER_TYPE_SMS, messages[i]);
					}
				}
			} while (c.moveToNext());
		} catch (Exception e) {
			Logger.e("SMSBroadcastReceiver Exception: " + e.toString());
			e.printStackTrace();
		}
	}

}
