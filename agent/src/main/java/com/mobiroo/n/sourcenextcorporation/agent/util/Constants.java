package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.Manifest;
import android.media.AudioManager;
import android.net.Uri;


public class Constants {
	// globals
    public static final String TAG = "Agent";
    public static final String PREFS_NAME = "AgentPrefs";
    
    public static final String FEEDBACK_EMAIL_ADDRESS = "help@coleridgeapps.com";
    public static final String ABOUT_TEXT = "© 2016 Coleridge Apps.\nMore Info: http://tryagent.com";

    public static final String STRING_SPLIT = "##";
    
    // preference names
    public static final String PREF_DEBUGGING = "prefDebug";
    public static final String PREF_LAST_GROUP = "prefLastReportGroup";
    public static final String PREF_NOTIFICATIONS = "prefNotifications";
    public static final String PREF_INSTALL_MILLIS = "prefInstallMillis";
    public static final String PREF_INSTALL_VERSION = "prefInstallVersion";
    public static final String PREF_USE_ANALYTICS = "prefUseAnalytics";
    public static final String PREF_ZIP_DEBUG_FILE = "prefZipDebugFile";
    public static final String PREF_PRESERVE_SETTINGS_AGENT_UNINSTALL = "prefPreserveSettingsAgentUninstall";
    public static final String PREF_USE_STATUS_SHARE = "prefUseStatusShare";
    public static final String PREF_SOUND_ON_AGENT_START = "prefSoundOnAgentStart";
    public static final String PREF_PUSH_NOTIFICATION = "prefPushNotification";
    public static final String PREF_LAST_APP_VERSION = "prefLastAppVersion";
    public static final String PREF_GRANDFATHER = "prefGrandfather";
    public static final String PREF_SHORTCUT_CREATED = "prefShortcutCreated";
    public static final String PREF_APP_UUID = "prefAppUUID";
    public static final String PREF_RT_DATE = "prefRTDate";
    public static final String PREF_DISPLAY_PUSH_SETTING = "prefDisplayPushSetting";
    public static final String PREF_DRM_SHOWN_DATE = "prefDRMShownDate";

    public static final String PREF_WELCOME_ACTIVITY_SEEN = "pref_welcome_activity_seen";
    public static final String PREF_LAST_ANALYTICS_PARKING_STARTED = "prefLastMPParkingStarted";

    public static final String PREF_HAS_SEEN_NOTIFICATION_ACCESS = "prefHasSeenNotificationAccess";

    public static final String PREF_FCM_REGISTERED = "prefFcmRegistered";

    // enumerated values for some preferences
    public static final int PREF_VAL_NOTIFICATIONS_ONE = 1;
    public static final int PREF_VAL_NOTIFICATIONS_NONE = 2;

    public static final String PREF_LAST_KNOWN_LOCATION = "prefLastKnownLocation";

    // agent statuses
	public static final String STATUS_ACTIVE = "active";
	public static final String STATUS_INACTIVE = "inactive";
    
    
	// agent activation trigger types
    public static final int TRIGGER_TYPE_UNKNOWN = 0;
    public static final int TRIGGER_ALWAYS_ACTIVE = 8;
    public static final int TRIGGER_TYPE_MANUAL = 9;
    public static final int TRIGGER_TYPE_BATTERY = 10;
    public static final int TRIGGER_TYPE_SMS = 11;
    public static final int TRIGGER_TYPE_TIME = 12;
    public static final int TRIGGER_TYPE_BLUETOOTH = 13;
    public static final int TRIGGER_TYPE_WIFI = 14;
    public static final int TRIGGER_TYPE_NFC = 15;
    public static final int TRIGGER_TYPE_PHONE_CALL = 16;
    public static final int TRIGGER_TYPE_MISSED_CALL = 17;
    public static final int TRIGGER_TYPE_PARKING = 18;
    public static final int TRIGGER_TYPE_DRIVING = 19;
    public static final int TRIGGER_TYPE_BOOT = 20;
    
    
    // google analytics constants
    public static final String USAGE_CATEGORY_APP_ACTION = "app_action";
    public static final String USAGE_APP_OPENED = "opened";
    public static final String USAGE_AGENT_STARTED = "agent_started";
    public static final String USAGE_ANALYTICS_REMOVED = "analytics_removed";
    
    
    // other various constants
    public static final String GMT_DATE_FORMAT = "dd LLL yyyy HH:mm:ss z";
    
    public static final Uri URI_AGENTS_TABLE = Uri.parse("sqlite://com.mobiroo.n.sourcenextcorporation.agent/agents");
    
	public static final int[] ALL_AUDIO_STREAMS = {AudioManager.STREAM_ALARM, AudioManager.STREAM_DTMF, 
		AudioManager.STREAM_MUSIC, AudioManager.STREAM_NOTIFICATION,
		AudioManager.STREAM_RING, AudioManager.STREAM_SYSTEM, AudioManager.STREAM_VOICE_CALL};

    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;


    public static String getpk() {
        return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj/B+rYHxf/budDZOF7o1UirYdSfTY9x1oRUPtsNonf6jAi/61ValvyUZ632jbMdWUSecZGxok1FIUfvXw+CPo9Nevr8X5C7HzOQr1VANVUEDCIddUpzgvPIk6MxB+i8Y+C+jboEg9hI36keLs8ay+DATuyt0lTGm9sHCKwU9Djo3fEvVMBjC4vQasXtZUhcnBsqAnIro0LIwInx6EtCJeP7OqdPr4cR4HS5yAuBGwb8vM4MdLj9MY4gDh9YU/Vly9FW9bn1FjYlSTzdydx/Dm7xE0vEvHTsIr1nn40/TLoABGmghI4IX8ZsK/2lM+fsDTnnnrGQBfrZ23WKdurHnQQIDAQAB";
    }

    public static String SKU_UNLOCK = "sku_upgrade";

    // Permission Request Codes
    public static final int PERMISSIONS_REQUEST_READ_CALENDAR = 1;
    public static final int PERMISSIONS_REQUEST_MEETING = 2;
    public static final int PERMISSIONS_REQUEST_READ_PHONE_STATE = 3;
    public static final int PERMISSIONS_REQUEST_SMS = 4;
    public static final int PERMISSIONS_REQUEST_WRITE_SETTINGS = 5;
    public static final int PERMISSIONS_REQUEST_READ_CONTACTS = 6;
    public static final int PERMISSIONS_REQUEST_LOCATION = 7;
    public static final int PERMISSIONS_REQUEST_VOICE_RESPONSE = 8;
    public static final int PERMISSIONS_REQUEST_CODE = 9;

    // Permission Broadcast Actions
    public static final String ACTION_PERMISSION_GRANTED_MEETING = "tryagent.meeting.permission.granted";
    public static final String ACTION_PERMISSION_READ_PHONE_STATE = "tryagent.permission.read.phone.state";
    public static final String ACTION_PERMISSION_RESULT = "tryagent.permission.result";

    /**
     * Permissions required for Meeting agent
     */
    public static final String[] PERMISSIONS_MEETING = {
            Manifest.permission.READ_CALENDAR, Manifest.permission.GET_ACCOUNTS
    };

    /**
     * Permissions required for Drive agent
     */
    public static final String[] PERMISSIONS_DRIVE = {
            Manifest.permission.READ_CONTACTS
    };

    public static final String PRIVACY_POLICY_URL = "http://tryagent.com/privacy.html";

    public static final String EXTRAS_PERMISSIONS = "permissions";

    public static final String PERMISSION_DO_NOT_DISTURB = "permission_do_not_disturb";
    public static final String PERMISSION_WRITE_SYSTEM_SETTINGS = "permission_write_system_settings";

    public static final int NOTIFICATION_ID = 1000;

    public static final String SENDER_ID = "REPLACE_HERE";
}
