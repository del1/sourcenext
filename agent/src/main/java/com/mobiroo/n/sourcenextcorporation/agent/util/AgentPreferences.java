package com.mobiroo.n.sourcenextcorporation.agent.util;

public class AgentPreferences {		
	public static final String STRING_SPLIT="###";
	public static final String LIST_SPLIT="~~~";

	// agent has changed
	public static final String AGENT_OPENED = "agentOpened";
	public static final String AGENT_SETTINGS_CHANGED = "agentSettingsChanged";
	public static final String AGENT_NOTIFICATION_SEEN = "agentNotificationSeen_";
	public static final String AGENT_STARTED_FROM_CONFIG = "agentStartedFromConfig";
	
	// generic action preferences
	public static final String SILENCE_PHONE = "agentPrefSilencePhone";
	public static final String ACTIVATE_ONLY_ON_INACTIVITY = "agentActivateOnlyOnInactivity";
	public static final String MIN_INACTIVE_TIME = "agentMinInactiveTime";
	public static final String DEVICE_STILL_FOR       = "agentDeviceStillFor";
	public static final String DAYS_OF_WEEK = "agentDaysOfWeek";
	public static final String START_ONLY_WHEN_CHARGING = "agentStartOnlyWhenCharging";
	public static final String START_ONLY_WHEN_SCREEN_OFF = "agentStartOnlyWhenScreenOff";

	// issues
	public static final String ISSUES_DISMISSED = "agentIssuesDismissed";

	// ab tests
	public static final String AB_TEST_INSTALL = "agentAbTestInstall";
	
	// autorespond modes
	// used by AutorespondPhoneCallAction and AutorespondSmsAction
	// wake wakes up the user using alarms
	// respond just responds with a text message
	public static final String AUTORESPOND_MODE_WAKE = "agentrespondModeWake";
	public static final String AUTORESPOND_MODE_RESPOND = "autorespondModeRespond";
	
	/*
	SMS settings (used by AutorespondSmsAction and AutorespondPhoneCallAction)
	SMS_AUTORESPOND: set to 'true' to allow SMS handling and auto-response actions; 'false' to disable
	SMS_AUTORESPOND_MODE: set to either AUTORESPOND_MODE_WAKE or AUTO_RESPOND_MODE_RESPOND
	SMS_AUTORESPOND_VERIFY_URGENT: set to 'true' to enable verify urgent mode.  When true, people in
	                               whitelist must first verify urgency to get through.  This is done
	                               through an SMS that asks them to say urgent.
	SMS_AUTORESPOND_MESSAGE: the message to auto-respond with.  Used in mode AUTORESPOND_MODE_RESPOND
	                         as well as whenever SMS_AUTORESPOND_VERIFY_URGENT is true.
	SMS_AUTORESPOND_ONCE: set to 'true' to only auto-respond once per contact per session.  This should
	                      almost always always be true.
	SMS_AUTORESPOND_CONTACTS: string of contact IDs that are whitelisted for waking and autorespond.  
	                          Separated by ###. 'everyone' and 'strangers' are special.
	SMS_READ_ALOUD: used by ReadTextAction; set to true to read aloud incoming text messages                        
	*/
	public static final String SMS_AUTORESPOND = "agentPrefSMSAutorespond";
	public static final String SMS_AUTORESPOND_MODE = "agentPrefSMSAutorespondMode";
	public static final String SMS_AUTORESPOND_VERIFY_URGENT = "agentPrefSMSAutoRespondToUrgentSMS";
	public static final String SMS_AUTORESPOND_MESSAGE = "agentPrefSMSAutoRespondMessage";
	public static final String SMS_AUTORESPOND_ONCE = "agentPrefSMSRespondOnce";
	public static final String SMS_AUTORESPOND_CONTACTS = "agentPrefSMSAuthRespondContacts";
	public static final String SMS_AUTORESPOND_CONTACT_NOONE = "";
	public static final String SMS_AUTORESPOND_CONTACT_EVERYONE = "everyone";
	public static final String SMS_AUTORESPOND_CONTACT_STRANGERS = "strangers";
	public static final String SMS_READ_ALOUD = "agentPrefSmsReadAloud";
	public static final String SMS_READ_ALOUD_VOICE_RESPONSE = "agentPrefSmsReadAloudVoiceResponse";
    public static final String SMS_READ_USING_SPEAKERPHONE = "agentPrefSmsUseSpeakerPhone";
    public static final String SMS_RESPOND_WITH_HEADSET = "agentPrefSmsResponseWithHeadset";
	public static final String NOTIFICATIONS_READ_ALOUD_VOICE_RESPONSE = "agentPrefReadOtherMessagesAloudResponse";

	// phone call settings (used by AutorespondPhoneCallAction, a subclass of AutorespondSmsAction)
	// shares some preferences with SMS trigger
	// PHONE_CALL_AUTORESPOND: set to 'true' to allow phone call handling and auto-responses
	// PHONE_CALL_AUTORESPOND_MODE: set to either AUTORESPOND_MODE_WAKE or AUTORESPOND_MODE_RESPOND
	// PHONE_CALL_ALLOW_ON_REPEAT_CALL: set to allow anyone that calls twice in 5 minutes through
	// also re-uses: SMS_AUTORESPOND_VERIFY_URGENT, SMS_AUTORESPOND_MESSAGE, SMS_AUTORESPOND_ONCE, 
	// SMS_AUTORESPOND_CONTACTS
	public static final String PHONE_CALL_AUTORESPOND = "agentPrefPhoneCallAutorespond";
	public static final String PHONE_CALL_AUTORESPOND_MODE = "agentPrefPhoneCallAutorespondMode";
	public static final String PHONE_CALL_ALLOW_ON_REPEAT_CALL = "agentPrefPhoneCallAllowOnRepeatCall";
	
	
	// generic trigger preferences	
	public static final String BATTERY_PERCENT_TRIGGER = "agentPrefBatteryPerecentTrigger";
	public static final String BLUETOOTH_NAME_TRIGGER = "agentPrefBTNameTrigger";	
	public static final String TIME_START_TRIGGER = "agentPrefTimeStartTrigger";
	public static final String TIME_END_TRIGGER = "agentPrefTimeEndTrigger";
	

	public static final String TIME_RANGE_TRIGGER = "agentPrefTimeStartEndArray";

	// agent-specific actions/triggers
	public static final String BATTERY_DISABLE_WHEN_CHARGING = "agentPrefBatteryDisableWhenCharging";
	public static final String BATTERY_BT = "agentPrefBatteryBt";
	public static final String BATTERY_SYNC = "agentPrefBatterySync";
	public static final String BATTERY_DISPLAY = "agentPrefBatteryDisplay";
	public static final String BATTERY_BRIGHTNESS_LEVEL = "agentPrefBatteryBrightnessLevel";
	public static final String BATTERY_WIFI = "agentPrefBatteryWifi";
	public static final String BATTERY_MOBILE_DATA = "agentPrefBatteryMobileData";

	public static final String USE_ACTIVITY_DETECTION = "agentUseActivityDetection";
	
	public static final String MEETING_ACCOUNTS = "agentMeetingAccounts_";  // used with + account.name
	public static final String MEETING_ACCEPTED_ONLY = "agentMeetingAcceptedOnly";
	public static final String MEETING_BUSY_ONLY = "agentMeetingBusyOnly";
	public static final String MEETING_IGNORE_ALL_DAY = "agentMeetingIgnoreAllDay";
	public static final String MEETING_START_EARLY_M = "agentMeetingStartEarly";

	public static final String SOUND_SILENCE_DEVICE = "agentSilenceDevice";
	
	// other constants
	public static final String AGENT_SESSION_PREFIX = "AGENT_SESSION_FOR_GUID_";
	public static final String OTHER_PREFS_FILE = "otherPrefs";
	public static final String AGENT_MAX_ALARM_REQUEST_CODE = "maxAlarmRequestCode";

    public static final String WIFI_HOME_NAME = "agentPrefWifiHome";
}
