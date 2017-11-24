package com.mobiroo.n.sourcenextcorporation.agent.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.DbAgent;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.TaskDatabaseHelper;

public class PhoneCallReceiver extends BroadcastReceiver {

	private static String PHONE_CALL_RECEIVED = "android.intent.action.PHONE_STATE";
	private static AgentPhoneStateListener sPhoneStateListener;

	protected class AgentPhoneStateListener extends PhoneStateListener {
		protected Context mContext;
		protected int mPriorState = -1;
		protected String mPhoneNumber;
		
		public void setContext(Context context) {
			mContext = context;
		}

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			Logger.d("onCallStateChanged: " + String.valueOf(state) + " : " + String.valueOf(mPriorState) + " : "
					+ incomingNumber);

			boolean missedCall = ((mPriorState == TelephonyManager.CALL_STATE_RINGING) && (state == TelephonyManager.CALL_STATE_IDLE));
			mPriorState = state;

			if ((state != TelephonyManager.CALL_STATE_RINGING) && (state != TelephonyManager.CALL_STATE_IDLE)) {
				return;
			}

			// in API 19, call state idle passed empty string for ""
			// store RINGING number in mPhoneNumber for duration for call
			if ((incomingNumber != null) && (!incomingNumber.trim().equals(""))) {
				mPhoneNumber = incomingNumber;
				Logger.d("onCallStateChanged: using phone number: " + mPhoneNumber);
			}

			SQLiteDatabase db = TaskDatabaseHelper.getInstance(mContext).getReadableDatabase();

			Cursor c = TaskDatabaseHelper.getEnabledTriggersOfType(db, Constants.TRIGGER_TYPE_PHONE_CALL);
			if (!c.moveToFirst()) {
				Logger.d("onCallStateChanged: no phone call ringing triggers found");
			}				
			else {
				do {
					int triggerId = c.getInt(c
							.getColumnIndex(TaskDatabaseHelper.FIELD_ID));
					String agentGuid = c.getString(c
							.getColumnIndex(TaskDatabaseHelper.FIELD_GUID));

					if (DbAgent.isActive(mContext, agentGuid)) {
						if (state == TelephonyManager.CALL_STATE_RINGING) {
							Logger.i("onCallStateChanged: fired phone call trigger id: "
									+ String.valueOf(triggerId));
							DbAgent.triggerActions(mContext, agentGuid,
									Constants.TRIGGER_TYPE_PHONE_CALL,
									mPhoneNumber);
						} else {
							Logger.i("onCallStateChanged: unfired phone call trigger id: "
									+ String.valueOf(triggerId));
							DbAgent.untriggerActions(mContext, agentGuid,
									Constants.TRIGGER_TYPE_PHONE_CALL,
									mPhoneNumber);
						}
					}
				} while (c.moveToNext());
			}
			c.close();

			if (!missedCall) {return;}

			// fire missed call triggers
			Cursor c2 = TaskDatabaseHelper.getEnabledTriggersOfType(db,
					Constants.TRIGGER_TYPE_MISSED_CALL);
			if (!c2.moveToFirst()) {
				Logger.d("onCallStateChanged: no missed call triggers found");
			} else {
				do {
					int triggerId = c2.getInt(c
							.getColumnIndex(TaskDatabaseHelper.FIELD_ID));
					String agentGuid = c2.getString(c
							.getColumnIndex(TaskDatabaseHelper.FIELD_GUID));

					if (DbAgent.isActive(mContext, agentGuid)) {
						Logger.i("onCallStateChanged: fired missed call trigger id: "
								+ String.valueOf(triggerId));
						DbAgent.triggerActions(mContext, agentGuid,
								Constants.TRIGGER_TYPE_MISSED_CALL,
								mPhoneNumber);
					}
				} while (c2.moveToNext());
			}
			c2.close();

		}

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.d("PhoneCallReceiver received intent:" + intent.getAction());
		if (!intent.getAction().equals(PHONE_CALL_RECEIVED)) {
			return;
		}

		if (sPhoneStateListener == null) {
			sPhoneStateListener = new AgentPhoneStateListener();
			sPhoneStateListener.setContext(context);
			TelephonyManager telephony = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			telephony.listen(sPhoneStateListener,
					PhoneStateListener.LISTEN_CALL_STATE);
		} else {
			sPhoneStateListener.setContext(context);
		}
	}

}
