package com.mobiroo.n.sourcenextcorporation.agent.util;


import android.content.Context;
import android.provider.Settings.Secure;

import com.amplitude.api.Amplitude;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

import org.json.JSONObject;

public class Usage {
    
    public static boolean canLogData(Context context) {
        return PrefsHelper.getPrefBool(context, Constants.PREF_USE_ANALYTICS, true);
    }
	
    public enum Events {

      OPENED_APP("Opened Agent app"),
      OPENED_AGENT("Opened an agent"),
      INSTALL_AGENT_WITH_ON_OFF_BUTTON("Enabled an agent with on off button"),
      INSTALL_AGENT_WITH_STATUS_BAR("Enabled an agent with status bar"),
      UNINSTALL_AGENT_WITH_ON_OFF_BUTTON("Disabled an agent with on off button"),
      UNINSTALL_AGENT_WITH_INFO_BUTTON("Disabled an agent from info screen button"),
      UNINSTALL_AGENT_WITH_FIRST_STARTING_NOTIFICATION("Disabled an agent from first start notif"),
      CHANGE_AGENT_SETTINGS("Changed settings for an agent"),
      AGENT_STARTED("An agent started"),
      AGENT_PAUSED("Paused an agent"),
      AGENT_UNPAUSED("Unpaused an agent"),
      SMS_SENT("SMS Sent"),
      SMS_READ_WITH_VOICE("SMS read out loud"),
      SMS_SENT_WITH_VOICE_RESPONSE("SMS sent with voice response"),
      TEST_EVERYTHING_INSTALLED("A/B Test: All agents installed"),
      TEST_SOME_INSTALLED("A/B Test: Only battery and sleep agents installed"),
      SATISFACTION_LOVE("Satisfaction test: User loves app"),
      SATISFACTION_LIKE("Satisfaction test: User likes app"),
      SATISFACTION_DISLIKE("Satisfaction test: User does not like app"),
      WELCOME_SKIPPED("Welcome skipped"),
      WELCOME_INSTALL("Welcome ended with enabling agents"),
      ANALYTICS_REMOVED("Analytics Removed"),
      MORE_INFO_CLICKED("More info clicked"),
      MORE_SETTINGS_CLICKED("More settings clicked"),
      UNDEFINED("Undefined"),
      ONBOARDING_LETS_GET_STARTED("Onboarding: Let's get started"),
      ONBOARDING_SKIP("Onboarding: Skip"),
      ONBOARDING_DRIVING_SKIP("Onboarding: Driving Skip"),
      ONBOARDING_DRIVING_LEARN_MORE("Onboarding: Driving Learn More"),
      ONBOARDING_MEETING_LEARN_MORE("Onboarding: Meeting Learn More"),
      ONBOARDING_CHOOSE("Onboarding: Choose"),
      ONBOARDING_FINISH("Onboarding: Finish"),
      ONBOARDING_CALENDAR_SELECTED("Onboarding: Calendar selected");
      
      private String mValue;
      
      Events(String value) {
          mValue = value;
      }
      
      public String getValue() {
          return mValue;
      }
      
      @Override
      public String toString() {
          return this.getValue();
      }
      
      public static Events getEnum(String value) {
          if (value != null) {
              for (Events v: values()) {
                  if (value.equals(v.getValue())) {
                      return v;
                  }
              }
              return Events.UNDEFINED;
          } else {
              throw new IllegalArgumentException();
          }
      }
    }
    
    public enum Properties {
        
        AGENT_NAME("Agent Name"),
        SETTING_NAME("Setting Name"),
        SETTING_VALUE("Setting Value"),
        TRIGGER_NAME("Trigger Name"),
        DAYS_INSTALLED("Days Installed"),
        UNDEFINED("Undefined");
        
        private String mValue;
        
        Properties(String value) {
            mValue = value;
        }
        
        public String getValue() {
            return mValue;
        }
        
        @Override
        public String toString() {
            return this.getValue();
        }
        
        public static Properties getEnum(String value) {
            if (value != null) {
                for (Properties v: values()) {
                    if (value.equals(v.getValue())) {
                        return v;
                    }
                }
                return Properties.UNDEFINED;
            } else {
                throw new IllegalArgumentException();
            }
        }
      }
    
    
      public static class EventProperty {
          private Properties mProperty;
          private String mValue;
          private String mPrefix;
          
          public EventProperty(String prefix, Properties property, String value) {
              mPrefix = prefix;
              mValue = value;
              mProperty = property;
          }
          public EventProperty(Properties property, String value) {
              mPrefix = null;
              mValue = value;
              mProperty = property;
          }
          
          public String getValue() {
              return mValue;
          }
          
          public String getPropertyName() {
              return (mPrefix == null) ? mProperty.getValue() : mPrefix + "_" + mProperty.getValue();
          }
          
          public Properties getProperty() {
              return mProperty;
          }
      }


    private static String AMPLITUDE_API_PROD_KEY = "39678411bf68667e64d741e304747763";
    public static final String  TOKEN_MIXPANEL = "a2c8706cfb44cfb8101fa8ec7281fff6";  // unused


    public static void startSession(Context context) {
        if (!canLogData(context)) {return;}
        Amplitude.startSession();
    }
    public static void endSession(Context context) {
        if (!canLogData(context)) { return;}
        Amplitude.endSession();
    }

    public static void initialize(Context context) {
        if (!canLogData(context)) { return;}
        Amplitude.initialize(context, AMPLITUDE_API_PROD_KEY);
    }


    public static void logEvent(Context context, Events event, boolean flush, EventProperty... data) {
    	if (!canLogData(context)) { return;}
    	
        JSONObject obj = new JSONObject();

        for (EventProperty item: data) {
            try { 
                obj.put(item.getPropertyName(), item.getValue());
            } catch (Exception e) { }
        }

        Amplitude.logEvent(event.getValue(), obj);
    }
    
 

    private static void logGoogleAnalyticsDataPoint(Context context, String category, String action, String label, long value) {
        try {                
            Tracker v3Tracker = EasyTracker.getInstance(context);
            v3Tracker.send(MapBuilder.createEvent(category, action, label, value).build());
        } catch (Exception e) {
        	Logger.d("Analytics error in background: " + e.toString());
        }
    }
    
    public static void logGoogleAnalyticsAppOpened(Context context) {
    	if (!canLogData(context)) { return;}
    	logGoogleAnalyticsDataPoint(context, Constants.USAGE_CATEGORY_APP_ACTION, Constants.USAGE_APP_OPENED, "", 0);
    }
    
    public static void logGoogleAnalyticsAgentStarted(Context context, String agentGuid) {
    	if (!canLogData(context)) { return;}
    	logGoogleAnalyticsDataPoint(context, Constants.USAGE_CATEGORY_APP_ACTION, Constants.USAGE_AGENT_STARTED, agentGuid, 0);
    }
    
    
    public static void logAnalyticsRemoved(Context context) {
        String id = "";
        try { id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID); }
        catch (Exception e) {}
        if (!id.isEmpty()) {
            Logger.i("Analytics removed");
            logGoogleAnalyticsDataPoint(context, Constants.USAGE_CATEGORY_APP_ACTION, Constants.USAGE_ANALYTICS_REMOVED, id, 0);
            Usage.logEvent(context, Events.ANALYTICS_REMOVED, true);
        }
    }
    
}

