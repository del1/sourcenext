package com.mobiroo.n.sourcenextcorporation.agent.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

import java.util.ArrayList;


public class PrefsHelper  {
    public static final String PREFS_NAME = Constants.PREFS_NAME;
    public static boolean pref_isDebug = true;


    public static String[] getPrefStringArray(Context context, String prefName, String defaultValue, String delim) {
        String prefValue = defaultValue;
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        prefValue = settings.getString(prefName, defaultValue);
        return prefValue.split(delim);
    }
    
    public static void appendPrefStringArray(Context context, String prefName, String defaultValue, String value, String delim) {
    	String prefValue = getPrefString(context, prefName, defaultValue);
    	if(prefValue.trim().equals("")) {
    		prefValue += value;
    	} else { 
    		prefValue += delim + value;
    	}
    	setPrefString(context, prefName, prefValue);
    }

    
    // deletes ALL occurences of "value" in stringArray
    public static boolean removeFromPrefStringArray(Context context, String prefName, String defaultValue, String value, String delim) {
    	String prefValue = defaultValue;
    	
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        prefValue = settings.getString(prefName, defaultValue);
        String[] prefStringArray = prefValue.split(delim);

        ArrayList<String> prefStringList = new ArrayList<String>();
        boolean stringDeleted = false;
        
        for(String prefString : prefStringArray) {
        	if(!prefString.trim().equals(value.trim())) {
        		prefStringList.add(prefString);
        	} else {
        		stringDeleted = true;
        	}
        }
        
        prefStringArray = prefStringList.toArray(new String[prefStringList.size()]);
        setPrefStringArray(context, prefName, prefStringArray, delim);
        
        return stringDeleted;
    }
    
    public static void setPrefStringArray(Context context, String prefName, String[] values, String delim) {
    	if(values.length == 0) {
        	setPrefString(context, prefName, "");
    	} else {
    		StringBuilder builder = new StringBuilder();
    		for(int i=0; i<values.length; i++) {
    			builder.append(values[i]);
    			if(values.length != (i+1)) {
    				builder.append(delim);
    			}
    		}
    		setPrefString(context, prefName, builder.toString());
    	}
    }
    
    public static String getPrefString(Context context, String prefName, String defaultValue) {
        String prefValue = defaultValue;
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        prefValue = settings.getString(prefName, defaultValue);
        return prefValue;

    }

    public static void setPrefString(Context context, String prefName, String prefValue) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(prefName, prefValue);
        editor.commit();

    }

    public static boolean getPrefBool(Context context, String prefName) {
        boolean prefValue = false;
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        prefValue = settings.getBoolean(prefName, false);
        return prefValue;

    }

    public static boolean getPrefBool(Context context, String prefName, boolean defaultValue) {
        boolean prefValue = false;
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        prefValue = settings.getBoolean(prefName, defaultValue);
        return prefValue;

    }

    public static void setPrefBool(Context context, String prefName, boolean prefValue) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(prefName, prefValue);
        editor.commit();

    }

    public static long getPrefLong(Context context, String prefName) {
        long prefValue = 0;
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        prefValue = settings.getLong(prefName, 0);
        return prefValue;

    }

    public static long getPrefLong(Context context, String prefName, long prefValue) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        prefValue = settings.getLong(prefName, prefValue);
        return prefValue;

    }

    public static void setPrefLong(Context context, String prefName, long prefValue) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(prefName, prefValue);
        editor.commit();

    }

    public static int getPrefInt(Context context, String prefName) {
        return getPrefInt(context, prefName, 0);

    }

    public static int getPrefInt(Context context, String prefName, int defaultValue) {
        int prefValue = defaultValue;
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        prefValue = settings.getInt(prefName, defaultValue);
        return prefValue;

    }

    public static void setPrefInt(Context context, String prefName, int prefValue) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(prefName, prefValue);
        editor.commit();

    }
    
    public static void removePref(Context context, String prefName) {
    	context.getSharedPreferences(PREFS_NAME, 0).edit().remove(prefName).commit();
    }

    public static boolean isAirplaneModeEnabled(Context context) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                return (Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON) == 1);
            } else {
                return (Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON) == 1);
            }
        } catch (Exception e) {
            Logger.e("Exception querying Airplane mode state: " + e, e);
            return false;
        }
    }
}
