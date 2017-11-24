package com.mobiroo.n.sourcenextcorporation.agent.action;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

import junit.framework.Assert;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SettingsButler {
	public static final int ENABLED = 1;
	public static final int DISABLED = 0;

	public static final int UNKNOWN_SETTING = -100;

	public static final String PREFS_FILE = "agentSettingsButler";
	private static final String STATE_DELIM = ":::";

	private static final String BLUETOOTH_PREF = "BT";
	private static final String SYNC_PREF = "SYNC";
	private static final String BRIGHTNESS_PREF = "BRIGHT";
	private static final String VOLUMES_PREF = "VOL";
	private static final String WIFI_PREF = "WIFI";
	private static final String MOBILE_DATA_PREF = "MDATA";
	
	private static final String[] ALL_PREFS = {BLUETOOTH_PREF, BRIGHTNESS_PREF, SYNC_PREF, VOLUMES_PREF, WIFI_PREF, MOBILE_DATA_PREF};

	private Context mContext;
	private SharedPreferences mSharedPrefs;

	@SuppressWarnings("unused")
	private SettingsButler() {
		Assert.fail("Should not use empty SettingsButler constructor");
	}

	public SettingsButler(Context context) {
		mContext = context;
		mSharedPrefs = mContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
	}

	public void clearAll() {
		mSharedPrefs.edit().clear().commit();
	}

	// should not be necessary but better to be safe
	// will log errors if doing anything, so we can fix it for real
	public void clearAgent(String agentGuid) {
		SharedPreferences.Editor editor = mSharedPrefs.edit();

		boolean needsCommit = false;
		for (String prefName: ALL_PREFS) {
			SettingState ss = loadSettingState(prefName);
			if (removeAgentTransitions(ss, prefName, agentGuid)) {
				editor.putString(prefName, ss.serializeToPref());
				needsCommit = true;
			}
		}

		if (needsCommit) {
			editor.commit();
		} else {
			Logger.d("SettingsButler clearAgent: nothing done");
		}
	}


	// bluetoothState should be one of SettingsButtler.{ENABLED,DISABLED}
	public void setBluetoothState(String agentGuid, String requestId, int bluetoothState) {
		SettingState ss = loadSettingState(BLUETOOTH_PREF);

		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		String curState = String.valueOf((adapter.isEnabled()) ? ENABLED : DISABLED);
		String newState = String.valueOf(bluetoothState);

		Logger.d("SettingsButler setBluetoothState, curState = " + curState + ", newState = " + newState);
		if (!curState.equals(newState)) {
			if (bluetoothState == ENABLED) {
				adapter.enable();
			} else {
				adapter.disable();
			}			
		}

		addTransitionAndSave(ss, BLUETOOTH_PREF, agentGuid, requestId, curState, newState);
	}
	public void resetBlutoothState(String agentGuid, String requestId) {
		SettingState ss = loadSettingState(BLUETOOTH_PREF);

		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		String curState = String.valueOf((adapter.isEnabled()) ? ENABLED : DISABLED);
		String resetToState = removeTransitionAndSave(ss, BLUETOOTH_PREF, agentGuid, requestId, curState);

		Logger.d("SettingsButler resetBluetoothState, curState = " + curState + ", resetToState = " + resetToState);

		if (!curState.equals(resetToState)) {
			if (Integer.parseInt(resetToState) == ENABLED) {
				adapter.enable();
			} else {
				adapter.disable();
			}			
		}
	}


	// wifiState should be one of SettingsButtler.{ENABLED,DISABLED}
	public void setWifiState(String agentGuid, String requestId, int wifiState) {
		SettingState ss = loadSettingState(WIFI_PREF);

		WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

		String curState = String.valueOf(wm.isWifiEnabled() ? ENABLED : DISABLED);
		String newState = String.valueOf(wifiState);

		Logger.d("SettingsButler setWifiState, curState = " + curState + ", newState = " + newState);
		if (!curState.equals(newState)) {
			wm.setWifiEnabled(wifiState == ENABLED);
		}

		addTransitionAndSave(ss, WIFI_PREF, agentGuid, requestId, curState, newState);
	}
	public void resetWifiState(String agentGuid, String requestId) {
		SettingState ss = loadSettingState(WIFI_PREF);

		WifiManager wm = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

		String curState = String.valueOf((wm.isWifiEnabled()) ? ENABLED : DISABLED);
		String resetToState = removeTransitionAndSave(ss, WIFI_PREF, agentGuid, requestId, curState);

		Logger.d("SettingsButler resetWifiState, curState = " + curState + ", resetToState = " + resetToState);

		if (!curState.equals(resetToState)) {
			wm.setWifiEnabled(Integer.parseInt(resetToState) == ENABLED);
		}
	}

	
	// mobileDataState should be one of SettingsButtler.{ENABLED,DISABLED}
	public void setMobileDataState(String agentGuid, String requestId, int mobileDataState) {
		SettingState ss = loadSettingState(MOBILE_DATA_PREF);

		String curState = String.valueOf(isMobileDataEnabled() ? ENABLED : DISABLED);
		String newState = String.valueOf(mobileDataState);

		Logger.d("SettingsButler setMobiledataState, curState = " + curState + ", newState = " + newState);
		if (!curState.equals(newState)) {
			enableDisableMobileData(mobileDataState == ENABLED);
		}

		addTransitionAndSave(ss, MOBILE_DATA_PREF, agentGuid, requestId, curState, newState);
	}
	public void resetMobileDataState(String agentGuid, String requestId) {
		SettingState ss = loadSettingState(MOBILE_DATA_PREF);

		String curState = String.valueOf((isMobileDataEnabled()) ? ENABLED : DISABLED);
		String resetToState = removeTransitionAndSave(ss, MOBILE_DATA_PREF, agentGuid, requestId, curState);

		Logger.d("SettingsButler resetMobileDataState, curState = " + curState + ", resetToState = " + resetToState);

		if (!curState.equals(resetToState)) {
			enableDisableMobileData(Integer.parseInt(resetToState) == ENABLED);
		}
	}
	
	
	// syncState should be one of SettingsButler.{ENABLED,DISABLED}
	public void setSyncState(String agentGuid, String requestId, int syncState) {
		SettingState ss = loadSettingState(SYNC_PREF);

		String curState = String.valueOf((ContentResolver.getMasterSyncAutomatically()) ? ENABLED : DISABLED);
		String newState = String.valueOf(syncState);

		Logger.d("SettingsButler setSyncState, curState = " + curState + ", newState = " + newState);


		if (!curState.equals(newState)) {
			ContentResolver.setMasterSyncAutomatically(syncState == ENABLED);
		}

		addTransitionAndSave(ss, SYNC_PREF, agentGuid, requestId, curState, newState);
	}
	public void resetSyncState(String agentGuid, String requestId) {
		SettingState ss = loadSettingState(SYNC_PREF);

		String curState = String.valueOf((ContentResolver.getMasterSyncAutomatically()) ? ENABLED : DISABLED);
		String resetToState = removeTransitionAndSave(ss, SYNC_PREF, agentGuid, requestId, curState);

		Logger.d("SettingsButler resetSyncState, curState = " + curState + ", resetToState = " + resetToState);

		if (!curState.equals(resetToState)) {
			ContentResolver.setMasterSyncAutomatically(Integer.parseInt(resetToState) == ENABLED);
		}
	}


	// brightnessPercent should be percent between 0 and 100
	public void setBrightness(String agentGuid, String requestId, int brightnessPercent) {
		SettingState ss = loadSettingState(BRIGHTNESS_PREF);

		long curBrightPercent = Math.round(getSetting(Settings.System.SCREEN_BRIGHTNESS) * 100.0 / 255.0);
		int curBrightMode = getSetting(Settings.System.SCREEN_BRIGHTNESS_MODE);

		String curState = String.valueOf(curBrightMode) + STATE_DELIM + String.valueOf(curBrightPercent);
		String newState = String.valueOf(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) + STATE_DELIM + String.valueOf(brightnessPercent);

		Logger.d("SettingsButler setBrightness, curState = " + curState + ", newState = " + newState);

		if (!curState.equals(newState)) {
			setBrightness(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL, brightnessPercent);
		}

		addTransitionAndSave(ss, BRIGHTNESS_PREF, agentGuid, requestId, curState, newState);
	}
	public void resetBrightness(String agentGuid, String requestId) {
		SettingState ss = loadSettingState(BRIGHTNESS_PREF);

		long curBrightPercent = Math.round(getSetting(Settings.System.SCREEN_BRIGHTNESS) * 100.0 / 255.0);
		int curBrightMode = getSetting(Settings.System.SCREEN_BRIGHTNESS_MODE);

		String curState = String.valueOf(curBrightMode) + STATE_DELIM + String.valueOf(curBrightPercent);
		String resetToState = removeTransitionAndSave(ss, BRIGHTNESS_PREF, agentGuid, requestId, curState);

		Logger.d("SettingsButler resetBrightness, curState = " + curState + ", resetToState = " + resetToState);

		if (!curState.equals(resetToState)) {
			String[] resetToStateSplit = resetToState.split(STATE_DELIM);
			setBrightness(Integer.parseInt(resetToStateSplit[0]), Integer.parseInt(resetToStateSplit[1]));
		}
	}


	// streamVolumes should be hash of AudioManager stream type --> volume level
	// ringerMode should be one of AudioManager.RINGER_MODE_{SILENT,VIBRATE,NORMAL}
	@SuppressLint("NewApi")
    public void setVolumes(String agentGuid, String requestId, HashMap <Integer, Integer> streamVolumes, int ringerMode) {
		if (!Utils.canChangeDoNotDisturb(mContext)) {
			Utils.postNotification(mContext, new String[] {
					Constants.PERMISSION_DO_NOT_DISTURB
			});
			return;
		}

		SettingState ss = loadSettingState(VOLUMES_PREF);


		AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

		HashMap <Integer, Integer> curVolumes = getCurrentVolumesHash(streamVolumes.keySet());
		int curRingerMode = am.getRingerMode();

		String curState = integerHashSimpleSerialize(curVolumes) + STATE_DELIM + String.valueOf(curRingerMode);
		String newState = integerHashSimpleSerialize(streamVolumes) + STATE_DELIM + String.valueOf(ringerMode);


		Logger.d("SettingsButler setVolumes, curState = " + curState + ", newState = " + newState);
        sendShushIntent();


        for (Integer stream: streamVolumes.keySet()) {
			if (!curVolumes.containsKey(stream)) {continue;}
			if (streamVolumes.get(stream).equals(curVolumes.get(stream))) {continue;}
			am.setStreamVolume(stream.intValue(), streamVolumes.get(stream).intValue(), 0);
		}
		am.setRingerMode(ringerMode);

		addTransitionAndSave(ss, VOLUMES_PREF, agentGuid, requestId, curState, newState);

        int newRingerMode = am.getRingerMode();
        HashMap <Integer, Integer> newVolumes = getAllCurrentVolumesHash();
        Logger.d("SettingsButler afterSetVolumes, newVolumes = " + newVolumes.toString() + ", newRingerMode=" + newRingerMode);
	}

    public void resetVolumes(String agentGuid, String requestId) {
        resetVolumes(agentGuid, requestId, false);
    }
	public void resetVolumes(String agentGuid, String requestId, boolean doNotLowerVolumes) {
        if (!Utils.canChangeDoNotDisturb(mContext)) {
			Utils.postNotification(mContext, new String[] {
					Constants.PERMISSION_DO_NOT_DISTURB
			});
            return;
        }

        SettingState ss = loadSettingState(VOLUMES_PREF);

		AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        String resetToState = removeTransitionAndSave(ss, VOLUMES_PREF, agentGuid, requestId, null);
		if (resetToState == null) {return;}
		
		String[] splits = resetToState.split(STATE_DELIM);

		HashMap <Integer, Integer> resetVolumes = integerHashSimpleDeserialize(splits[0]);
		HashMap <Integer, Integer> curVolumes = getAllCurrentVolumesHash();
		

		Logger.d("SettingsButler resetVolumes, curState = " + curVolumes.toString() + ", resetState = " + resetVolumes.toString());
        sendShushIntent();


        for (Integer stream: resetVolumes.keySet()) {
			if (!curVolumes.containsKey(stream)) {continue;}

            int rv = resetVolumes.get(stream).intValue();
            int cv = curVolumes.get(stream).intValue();

            if (rv == cv) { continue; }
            if ((doNotLowerVolumes) && (rv < cv)) {
                Logger.d("SettingsButler resetVolumes, not lowering stream volume lower than current: " + stream.toString() + " rv=" + rv + " vs cv =" + cv);
                continue;
            }

			am.setStreamVolume(stream.intValue(), rv, 0);
		}

        int curRingerMode = am.getRingerMode();
        int resetRingerMode = Integer.parseInt(splits[1]);
		Logger.d("SettingsButler resetVolumes, curRingerMode = " + curRingerMode + ", resetRingerMode = " + resetRingerMode);
		if ((curRingerMode != resetRingerMode) ||
                ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) && (resetRingerMode == AudioManager.RINGER_MODE_NORMAL))) {
            if ((! doNotLowerVolumes) || (resetRingerMode >= curRingerMode)) {
    			am.setRingerMode(resetRingerMode);
            } else {
                Logger.d("SettingsButler resetVolumes, not resetting ringer mode because it would lower sound levels");
            }
		}

        int newRingerMode = am.getRingerMode();
        HashMap <Integer, Integer> newVolumes = getAllCurrentVolumesHash();
        Logger.d("SettingsButler afterResetVolumes, newVolumes = " + newVolumes.toString() + ", newRingerMode=" + newRingerMode);
    }
    public HashMap<Integer, Integer> getResetVolumes() {
        SettingState ss = loadSettingState(VOLUMES_PREF);

        String resetToState = ss.mOriginalState;
        if (resetToState == null) {return null;}

        String[] splits = resetToState.split(STATE_DELIM);

        return integerHashSimpleDeserialize(splits[0]);
    }
    public HashMap<Integer, Integer> getAlertVolumes(Context context, int[] streams, String agentGuid, String requestId) {
        HashMap<Integer, Integer> volumes = new HashMap<Integer, Integer>(streams.length);
        HashMap <Integer, Integer> resetVolumes = getResetVolumes();
        boolean useMax = resetVolumes == null;

        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        for (int stream: streams) {
            int max = Integer.valueOf(am.getStreamMaxVolume(stream));
            int volume = (useMax || !resetVolumes.containsKey(stream)) ? max : resetVolumes.get(stream).intValue();
            if (volume < Math.floor((max/2))) {
                volume = (int) Math.floor((max/2));
            }
            Logger.d(String.format("SettingsButler: Returning stream %s volume as %s", stream, volume));
            volumes.put(Integer.valueOf(stream), volume);
        }

        return volumes;
    }

	// ----------------------------------------------------------------------------------	
	// PRIVATE/PROTECTED methods below
	// ----------------------------------------------------------------------------------	


	// state management classes and helpers

	protected class SettingState {
		protected String mSettingPrefName;
		protected String mOriginalState;
		protected ArrayList<SettingTransition> mSettingTransitions;

		protected void reset() {
			mOriginalState = null;
			if (mSettingTransitions != null) {
				mSettingTransitions.clear();
			}
		}

		protected String serializeToPref() {
			return this.toJson().toString();
		}
		protected void deserializeFromPref(String val) {
			try {
				fromJson(new JSONObject(val));
			} catch (JSONException e) {
				e.printStackTrace();
				Logger.e("SettingsButler deserializeFromPref: " + e.getMessage());
				reset();
			}
		}

		protected JSONObject toJson() {
			JSONObject json = new JSONObject();
			try {
				json.put("mSettingPrefName", mSettingPrefName);
				json.put("mOriginalState", mOriginalState);

				JSONArray jsa =new JSONArray();
				for (SettingTransition st: mSettingTransitions) {
					jsa.put(st.toJson());
				}
				json.put("mSettingTransitions", jsa);
			} catch (JSONException e) {
				Logger.e("SettingsButler SettingState.toJson(): " + e.getMessage());
				e.printStackTrace();
			}
			return json;
		}

		protected void fromJson(JSONObject j) {
			mSettingPrefName = j.optString("mSettingPrefName", null);
			mOriginalState = j.optString("mOriginalState", null);
			mSettingTransitions = new ArrayList<SettingTransition>();

			JSONArray transitions = j.optJSONArray("mSettingTransitions");
			if (transitions != null) {
				for (int i=0; i<transitions.length(); i++) {
					JSONObject jso;
					try {
						jso = transitions.getJSONObject(i);
						SettingTransition st = new SettingTransition();
						st.fromJson(jso);
						mSettingTransitions.add(st);
					} catch (JSONException e) {
						Logger.e("SettingsButler SettingState fromJson: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}

		@Override
		public String toString() {
			return toJson().toString(); 
		}

	}
	protected class SettingTransition {
		protected String mAgentGuid;
		protected String mRequestId;
		protected String mOldSettingVal;
		protected String mNewSettingVal;

		protected SettingTransition() {
		}

		protected SettingTransition(String agentGuid, String requestId, String oldSettingVal, String newSettingVal) {
			mAgentGuid = agentGuid;
			mRequestId = requestId;
			mOldSettingVal = oldSettingVal;
			mNewSettingVal = newSettingVal;
		}

		
		
		// generated by Eclipse
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result
					+ ((mAgentGuid == null) ? 0 : mAgentGuid.hashCode());
			result = prime * result
					+ ((mRequestId == null) ? 0 : mRequestId.hashCode());
			return result;
		}

		// generated by Eclipse
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SettingTransition other = (SettingTransition) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (mAgentGuid == null) {
				if (other.mAgentGuid != null)
					return false;
			} else if (!mAgentGuid.equals(other.mAgentGuid))
				return false;
			if (mRequestId == null) {
				if (other.mRequestId != null)
					return false;
			} else if (!mRequestId.equals(other.mRequestId))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return toJson().toString(); 
		}

		protected JSONObject toJson() {
			JSONObject json = new JSONObject();
			try {
				json.put("mAgentGuid", mAgentGuid);
				json.put("mRequestId", mRequestId);
				json.put("mOldSettingVal", mOldSettingVal);
				json.put("mNewSettingVal", mNewSettingVal);
			} catch (JSONException e) {
				Logger.e("SettingsButler SettingTransition toJson: " + e.getMessage());
				e.printStackTrace();
			}
			return json;
		}

		protected void fromJson(JSONObject j) {
			mAgentGuid = j.optString("mAgentGuid", null);
			mRequestId = j.optString("mRequestId", null);
			mOldSettingVal = j.optString("mOldSettingVal", null);
			mNewSettingVal = j.optString("mNewSettingVal", null);
		}

		private SettingsButler getOuterType() {
			return SettingsButler.this;
		}
	}

	private SettingState loadSettingState(String prefName) {
		SettingState ss = new SettingState();
		
		String serializedSettingState = mSharedPrefs.getString(prefName, "");
		if (serializedSettingState.equals("")) {
			ss.mSettingPrefName = prefName;
			ss.mSettingTransitions = new ArrayList<SettingTransition>();
			return ss;
		}
		ss.deserializeFromPref(serializedSettingState);
		return ss;
	}

	private void addTransitionAndSave(SettingState ss, String prefName, String agentGuid, String requestId, String curState, String newState) {
		ss.mSettingPrefName = prefName;
		if (ss.mOriginalState == null) {ss.mOriginalState = curState;}

		SettingTransition st = new SettingTransition(agentGuid, requestId, curState, newState);
		ss.mSettingTransitions.add(st);

		Logger.d("SettingsButler after addTransitionAndSave: ss=" + ss.toString());
		mSharedPrefs.edit().putString(prefName, ss.serializeToPref()).commit();
	}

	private String removeTransitionAndSave(SettingState ss, String prefName, String agentGuid, String requestId, String curState) {
		String resetToState = curState;
		Logger.d("SettingsButler before removeTransitionAndSave: ss=" + ss.toString());

		if (ss.mSettingTransitions.isEmpty()) {
			resetToState = (ss.mOriginalState == null) ? curState : ss.mOriginalState;
		} else {
			SettingTransition st = new SettingTransition(agentGuid, requestId, null, null);
			int lastIndex = ss.mSettingTransitions.lastIndexOf(st);
			if (lastIndex != -1) {
				if (lastIndex == (ss.mSettingTransitions.size() - 1)) {
					st = ss.mSettingTransitions.remove(lastIndex);
					resetToState = (ss.mSettingTransitions.size() == 0 ? ss.mOriginalState : st.mOldSettingVal);
				} else {
					ss.mSettingTransitions.remove(lastIndex);
				}
			} else {
				resetToState = curState;
			}
		}

        if (ss.mSettingTransitions.isEmpty()) {
            ss.reset();
        }

		Logger.d("SettingsButler after removeTransitionAndSave: ss=" + ss.toString());

		mSharedPrefs.edit().putString(prefName, ss.serializeToPref()).commit();

		return resetToState;
	}

	private boolean removeAgentTransitions(SettingState ss, String prefName, String agentGuid) {
		if ((ss.mSettingTransitions == null) || (ss.mSettingTransitions.size() < 1)) {return false;}

		ArrayList<String> removedRequestIds = new ArrayList<String>(3);

		boolean removed = false;
		for (SettingTransition st: ss.mSettingTransitions) {
			if (st.mAgentGuid == null) {Logger.d("WARNING: null st.agentGuid"); continue;}
			if (st.mAgentGuid.equals(agentGuid)) {
				removedRequestIds.add(st.mRequestId);
				st.mRequestId = "0";
				removed = true;
			}
		}

		if (! removed) { return false;}

		removed = false;
		SettingTransition st = new SettingTransition(agentGuid, "0", null, null);
		while (ss.mSettingTransitions.remove(st)) {
			removed = true;
		}

		if (removed) {
			Logger.e("SettingsButler clearAgentAndSave Stragglers for " + agentGuid + " " + TextUtils.join(", ", removedRequestIds));
			if (ss.mSettingTransitions.size() == 0) {
				ss.reset();
			}
		} else {
			Logger.e("SettingsButler clearAgentAndSave logic error -- removed should be true here.");
		}

		Logger.d("SettingsButler clearAgent: ss=" + ss.toString());

		return removed;
	}



	// settings helpers

	private int getSetting(String name) {
		try {
			return Settings.System.getInt(mContext.getContentResolver(), name);
		} catch (Exception e) {
			Logger.e("Exception getting setting " + name + ": " + e);
			return UNKNOWN_SETTING;
		}
	}
	
	

	
	private void enableDisableMobileData(boolean enabled) {
		try {
			ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
			Method method = cm.getClass().getMethod("setMobileDataEnabled", boolean.class);
			method.invoke(cm, enabled);
			Logger.d("SettingsButler: enableDisableMobileData finished.  enabled=" + enabled);
		} catch (NoSuchMethodException e) {
			Logger.e("SettingsButler: Exception when modifying mobile data, noSuchMethod", e);
		} catch (Exception e) {
			Logger.e("SettingsButler: Exception when modifying mobile data." + e);
		}
	}
	private boolean isMobileDataEnabled() {
        try {
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            Method method = cm.getClass().getMethod("getMobileDataEnabled");
            return ((Boolean) method.invoke(cm)) ? true : false;
        } catch (Exception e) {
            Logger.e("SettingsButler: Exception when querying mobile data status", e);
            return true;
        }
	}

	

	@SuppressLint("NewApi")
    private void setBrightness(int auto, int brightnessPercent) {
        if (Utils.isMarshmallowOrUp() && !Settings.System.canWrite(mContext)) {
			Utils.postNotification(mContext, new String[] {
					Constants.PERMISSION_WRITE_SYSTEM_SETTINGS
			});
			return;
        }

        Logger.d("BatteryAgentAction.setBrightness: " + String.valueOf(auto) + " : " + String.valueOf(brightnessPercent));
		/* Update settings from within agent, don't start activity */
		int iBrightnessLevel = Utils.calculateIntegerBrightnessFromPercent(brightnessPercent);
		Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, iBrightnessLevel);

		int iBMode = auto;

		if (iBMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
			Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
			// don't set current window brightness if setting auto=true; let android do it
		} else {
			Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
			Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, iBrightnessLevel);
		}

	}

	@SuppressLint("UseSparseArrays")
    private HashMap<Integer, Integer> getCurrentVolumesHash(Set<Integer> streams) {
		AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
		HashMap <Integer, Integer> streamVolumes = new HashMap<Integer,Integer>(7);
		for (Integer stream : streams) {
			streamVolumes.put(stream, Integer.valueOf(am.getStreamVolume(stream.intValue())));
		}
		return streamVolumes;
	}
	private HashMap<Integer, Integer> getAllCurrentVolumesHash() {
		Set<Integer> streams = new HashSet<Integer>(7);
		for (int stream: Constants.ALL_AUDIO_STREAMS) {
			streams.add(Integer.valueOf(stream));
		}
		return getCurrentVolumesHash(streams);
	}
	private String integerHashSimpleSerialize(HashMap <Integer, Integer> streamVolumes) {
		StringBuilder sb = new StringBuilder();
		for (Integer stream: streamVolumes.keySet()) {
			sb.append(String.valueOf(stream));
			sb.append("=");
			sb.append(String.valueOf(streamVolumes.get(stream)));
			sb.append(",");
		}
		return sb.toString();
	}
	@SuppressLint("UseSparseArrays")
	private HashMap<Integer,Integer> integerHashSimpleDeserialize(String jsonStr) {		
		HashMap<Integer, Integer> vols = new HashMap<Integer, Integer>();
		for (String s: jsonStr.split(",")) {
			if ((s==null) || (s.trim().equals(""))) {continue;}
			String[] splits = s.split("=");
			vols.put(Integer.valueOf(splits[0]), Integer.valueOf(splits[1]));
		}
		return vols;
	}

    private void sendShushIntent() {
        try {
            Intent intent = new Intent("com.androidintents.PRE_RINGER_MODE_CHANGE");
            intent.putExtra("com.androidintents.EXTRA_SENDER", "com.mobiroo.n.sourcenextcorporation.agent");
            mContext.sendBroadcast(intent);
        } catch (Exception e) {

        }
    }

}
