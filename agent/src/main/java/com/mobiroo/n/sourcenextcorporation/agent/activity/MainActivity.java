package com.mobiroo.n.sourcenextcorporation.agent.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.mobiroo.drm.MobirooDrm;
import com.mobiroo.n.sourcenextcorporation.agent.BuildConfig;
import com.mobiroo.n.sourcenextcorporation.agent.R;
import com.mobiroo.n.sourcenextcorporation.agent.billing.IabClient;
import com.mobiroo.n.sourcenextcorporation.agent.billing.IabResult;
import com.mobiroo.n.sourcenextcorporation.agent.billing.Purchase;
import com.mobiroo.n.sourcenextcorporation.agent.constants.AppURLs;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentHelpFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentWidgetFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AgentsFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.AppPreferencesFragment;
import com.mobiroo.n.sourcenextcorporation.agent.fragment.FeedFragment;
import com.mobiroo.n.sourcenextcorporation.agent.item.Agent;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.BatteryAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.MeetingAgent;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.NotificationFactory;
import com.mobiroo.n.sourcenextcorporation.agent.item.ui.notification.NotificationUtils;
import com.mobiroo.n.sourcenextcorporation.agent.service.ExpireTrialIntentService;
import com.mobiroo.n.sourcenextcorporation.agent.service.MyRegistrationIntentService;
import com.mobiroo.n.sourcenextcorporation.agent.task.AppLaunchCountTask;
import com.mobiroo.n.sourcenextcorporation.agent.util.ActivityRecognitionHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPrefDumper;
import com.mobiroo.n.sourcenextcorporation.agent.util.AgentPreferences;
import com.mobiroo.n.sourcenextcorporation.agent.util.AlertDialogUtility;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;
import com.mobiroo.n.sourcenextcorporation.agent.util.DebugFileUtils;
import com.mobiroo.n.sourcenextcorporation.agent.util.PrefsHelper;
import com.mobiroo.n.sourcenextcorporation.agent.util.Usage;
import com.mobiroo.n.sourcenextcorporation.agent.util.Utils;
import com.mobiroo.n.sourcenextcorporation.agent.util.tasks.AgentTaskCollection;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends FragmentActivity {
    public static final int REQUEST_AGENT_CONFIG = 10;
    public static final int REQUEST_WELCOME_ACTIVITY = 20;

    public static final int RESULT_AGENT_INSTALL = 100;
    public static final int RESULT_AGENT_UNINSTALL = 101;

    public static final String EXTRA_LAUNCH_CONFIG = "launch_config";
    public static final String EXTRA_AGENT_GUID = "agentGuid";
    public static final String EXTRA_CLEAR_NOTIFICATIONS = "clearNotifications";
    public static final String EXTRA_FROM_NOTIF = "fromNotif";
    public static final String EXTRA_NOTIF_ID = "notifId";
    public static final String EXTRA_NOTIF_TAG = "notifTag";
    public static final String EXTRA_FROM_WIDGET = "fromWidget";
    public static final String EXTRA_TRIGGER_TYPE = "triggerType";


    public static final String LAST_DRAWER_LOCATION = "lastDrawerLocation";
    public static final String LAST_SANITY_CHECK = "lastSanityCheck";
    public static final String WELCOME_AGENTS_INSTALLED_AT = "welcomeAgentsInstalledAt";
    public static final String APP_INSTALLED = "appInstalled";

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerLeftToggle;
    private ListView mDrawerLeftList;

    private int mCurrentPosition = 0;
    private String mCurrentTag;
    protected int mFeedbackLocation;

    protected AgentTaskCollection mTaskCollection;

    protected IabClient mIabClient;

    public void invalidateDrawer() {
        createDrawerList();
        mDrawerLeftList.setAdapter(new DrawerListAdapter());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mDrawerLayout.openDrawer(mDrawerLeftList);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTaskCollection.cancelTasks();
        if (mIabClient != null) {
            mIabClient.dispose();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTaskCollection = new AgentTaskCollection();

        setContentView(R.layout.activity_main);

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(MainActivity.this);

        String basePakage = BuildConfig.APPLICATION_ID;

        Usage.logGoogleAnalyticsAppOpened(MainActivity.this);
        Usage.logEvent(this, Usage.Events.OPENED_APP, true);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        if (PrefsHelper.getPrefLong(this, Constants.PREF_INSTALL_MILLIS, -1) == -1) {
            PrefsHelper.setPrefLong(this, Constants.PREF_INSTALL_MILLIS, System.currentTimeMillis());
            PrefsHelper.setPrefString(this, Constants.PREF_INSTALL_VERSION, Utils.getPackageVersion(this));
        }

        mDrawerLeftList = (ListView) findViewById(R.id.left_drawer);

        createDrawerList();
        mDrawerLeftList.setAdapter(new DrawerListAdapter());

        // Set the list's click listener
        mDrawerLeftList.setOnItemClickListener(new DrawerItemClickListener());

        mDrawerLeftToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.status_battery_on,
                R.string.battery_agent_bt_off) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu();
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu();
            }
        };

        selectItem(prefs.getInt(LAST_DRAWER_LOCATION, 0));
        if (!prefs.contains(LAST_DRAWER_LOCATION))
            mDrawerLayout.openDrawer(mDrawerLeftList);

        mDrawerLayout.addDrawerListener(mDrawerLeftToggle);

        InstallWelcomeTask installWelcomeTask = null;

        if (!prefs.contains(Constants.PREF_WELCOME_ACTIVITY_SEEN)) {
            Editor editor = prefs.edit();
            editor.putInt(Constants.PREF_WELCOME_ACTIVITY_SEEN, 0);
            editor.putLong(LAST_SANITY_CHECK, System.currentTimeMillis());
            editor.commit();

            if (!AgentPrefDumper.haveSavedAgentPrefs(this)) {
                // Run on-boarding as there are no user prefs to restore
                startActivityForResult(new Intent(this, OnboardingActivity.class), REQUEST_WELCOME_ACTIVITY);
            } else {
                // Just restore prefs
                installWelcomeTask = new InstallWelcomeTask(null);
                installWelcomeTask.execute();
            }
        } else {
            if (BuildConfig.DEBUG || (prefs.getLong(LAST_SANITY_CHECK, 0) < (System.currentTimeMillis() - AlarmManager.INTERVAL_DAY))) {
                prefs.edit().putLong(LAST_SANITY_CHECK, System.currentTimeMillis()).commit();
                new SanityCheckerTask().execute();
            }
            //commented by rohan
            /*if (!prefs.contains(SatisfactionActivity.PREF_SATISFACTION_ACTIVITY_SEEN)) {
                try {

                    if (!prefs.contains(SatisfactionActivity.PREF_SATISFACTION_ACTIVITY_WAIT)) {
                        int day_range = 14;
                        int wait = (int) (Math.random() * (double) day_range);

                        Editor editor = prefs.edit();
                        editor.putInt(SatisfactionActivity.PREF_SATISFACTION_ACTIVITY_WAIT, wait);
                        editor.commit();

                    }

                    int daysToWait = prefs.getInt(SatisfactionActivity.PREF_SATISFACTION_ACTIVITY_WAIT, 0);
                    daysToWait += 14;

                    if ((System.currentTimeMillis() - getPackageManager().getPackageInfo(getPackageName(), 0).firstInstallTime) > (AlarmManager.INTERVAL_DAY * daysToWait)) {
                        Editor editor = prefs.edit();
                        editor.putInt(SatisfactionActivity.PREF_SATISFACTION_ACTIVITY_SEEN, 0);
                        editor.commit();
                        startActivity(new Intent(this, SatisfactionActivity.class));
                    }
                } catch (NameNotFoundException e) {
                    Logger.e("Failed to find self in SatisfactionActivity launching code.");
                }
            }*/
        }

        if (savedInstanceState != null) {
            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            List<Fragment> fragments = manager.getFragments();
            for (int i = 0; i < fragments.size(); i++) {
                Fragment f = fragments.get(i);
                if (f.getTag() != null) {
                    if (!f.getTag().equals(mCurrentTag)) {
                        transaction.hide(f);
                    }
                }
            }
            transaction.commit();
        }

        Bundle extras = getIntent().getExtras();

        if ((extras != null) && (extras.getBoolean(EXTRA_LAUNCH_CONFIG, false))) {
            Agent agent = AgentFactory.getAgentFromGuid(this, extras.getString(EXTRA_AGENT_GUID));

            Intent configIntent = new Intent(this, agent.getConfigActivity());

            configIntent.putExtra(EXTRA_AGENT_GUID, agent.getGuid());


            // sent in when agentconfigurationactivity is started from a
            // notification
            boolean clearNotifications = extras.getBoolean(MainActivity.EXTRA_CLEAR_NOTIFICATIONS, false);
            if (clearNotifications) {
                int notifId = extras.getInt(MainActivity.EXTRA_NOTIF_ID, -1);
                String notifTag = extras.getString(MainActivity.EXTRA_NOTIF_TAG);
                if ((notifId != -1) && (notifTag != null)) {
                    NotificationFactory.dismissWithTag(this, notifTag, notifId);
                }
            }

            configIntent.putExtra(EXTRA_FROM_NOTIF, extras.getString(EXTRA_FROM_NOTIF));
            configIntent.putExtra(EXTRA_FROM_WIDGET, extras.getString(EXTRA_FROM_WIDGET));

            startActivity(configIntent);

            new AppLaunchCountTask(MainActivity.this, AppLaunchCountTask.APP_LAUNCH_TYPE.PUSH_START).execute();
        } else {
            new AppLaunchCountTask(MainActivity.this, AppLaunchCountTask.APP_LAUNCH_TYPE.START).execute();
        }

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(Constants.EXTRAS_PERMISSIONS)) {
                Utils.requestPermissions(this,
                        intent.getStringArrayExtra(Constants.EXTRAS_PERMISSIONS),
                        Constants.PERMISSIONS_REQUEST_CODE);
            } else {
                handleScheme(intent.getData());
            }
        }

        // Make sure you only run addShortcut() once, not to create duplicate shortcuts.
        if(!PrefsHelper.getPrefBool(MainActivity.this, Constants.PREF_SHORTCUT_CREATED, false)) {
            addShortcut();
        }

        if(PrefsHelper.getPrefBool(MainActivity.this, Constants.PREF_DISPLAY_PUSH_SETTING, true)
                && !PrefsHelper.getPrefBool(MainActivity.this, Constants.PREF_PUSH_NOTIFICATION, false)){

            AlertDialogUtility.showNotificationConfirmationDialog(this, new AlertDialogUtility.AlertDialogNotificationClickListner() {
                @Override
                public void onYesClick() {
                    PrefsHelper.setPrefBool(MainActivity.this, Constants.PREF_PUSH_NOTIFICATION, true);
                }

                @Override
                public void onLaterClick() {

                }

                @Override
                public void onDoNotDisplayClick() {
                    PrefsHelper.setPrefBool(MainActivity.this, Constants.PREF_DISPLAY_PUSH_SETTING, false);
                }
            });
        }
    }

    private void addShortcut() {
        //Adding shortcut for MainActivity
        //on Home screen
        Intent shortcutIntent = new Intent(getApplicationContext(), MainActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name));
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.drawable.ic_launcher));
        addIntent.putExtra("duplicate", false);
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);

        PrefsHelper.setPrefBool(MainActivity.this, Constants.PREF_SHORTCUT_CREATED, true);
    }

    protected void handleScheme(Uri data) {
        if (data == null) {
            return;
        }

        String scheme = data.getScheme();
        String host = data.getHost();

        Logger.i("scheme: " + scheme + " // host: " + host);

        if (scheme.equals("tryagent")) {
            if (host.equals("send_debug_log")) {

                View view = MainActivity.this.getLayoutInflater().inflate(R.layout.agent_dialog, null,
                        false);
                ((TextView) view.findViewById(R.id.aboutTextHeading)).setText(R.string.email_text);

                new AlertDialog.Builder(MainActivity.this).setView(view).setPositiveButton(R.string.email_affirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        DebugFileUtils.sendEmail(MainActivity.this);
                    }
                }).show();


            }
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerLeftToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerLeftToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerLeftToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        if (mCurrentPosition != 0) {
            selectItem(0);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onResume() {
//        if(!DateUtils.isToday(PrefsHelper.getPrefLong(MainActivity.this, Constants.PREF_DRM_SHOWN_DATE, 0L))) {
        //validate(MainActivity.this); //rohans comment //as per instruction
//        }

        super.onResume();

        checkPurchaseAndTrial();

        if (checkGooglePlayServicesAvailable() && !PrefsHelper.getPrefBool(this,Constants.PREF_FCM_REGISTERED,false)) {
            MyRegistrationIntentService.startIntentService(this);
        }
    }

    //rohans comment //as per instruction
    private void validate(final Activity myActivity) {
        myActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MobirooDrm.setDebugLog(true);
                MobirooDrm.validateActivity(myActivity);
                PrefsHelper.setPrefLong(MainActivity.this, Constants.PREF_DRM_SHOWN_DATE,System.currentTimeMillis());
            }
        });
    }

    /**
     * Check whether Google Play Services are available.
     *
     * If not, then display dialog allowing user to update Google Play Services
     *
     * @return true if available, or false if not
     */
    private boolean checkGooglePlayServicesAvailable()
    {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int status = googleApiAvailability.isGooglePlayServicesAvailable(getApplicationContext());

        if (status == ConnectionResult.SUCCESS)
        {
            return true;
        }

        Log.e(Constants.TAG, "Google Play Services not available: " + googleApiAvailability.getErrorString(status));

        if (googleApiAvailability.isUserResolvableError(status))
        {
            final Dialog errorDialog = googleApiAvailability.getErrorDialog(this, status, 1);
            if (errorDialog != null)
            {
                errorDialog.show();
            }
        }

        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();

        try {
            if (Usage.canLogData(MainActivity.this)) {
                EasyTracker.getInstance(this).activityStart(this);
            }
        } catch (Exception e) {
            Logger.d("Analytics error: " + e.toString());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            if (Usage.canLogData(MainActivity.this)) {
                EasyTracker.getInstance(this).activityStop(this);
            }
        } catch (Exception e) {
            Logger.d("Analytics error: " + e.toString());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // no options menu
        // getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 1001:
                Logger.d("Got back purchase info!");
                int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
                String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
                String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

                if (resultCode == RESULT_OK) {
                    try {
                        JSONObject jo = new JSONObject(purchaseData);
                        String sku = jo.getString("productId");
                        if (TextUtils.equals(sku, Constants.SKU_UNLOCK)) {
                            mIabClient.storePurchase(MainActivity.this, Constants.SKU_UNLOCK, true);
//                            findViewById(R.id.trial_footer).setVisibility(View.GONE);
                            IabClient.cancelTrialCancel(MainActivity.this);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case REQUEST_AGENT_CONFIG:
                Logger.i("Agent", "recognized requestcode as request agent config");
                super.onActivityResult(requestCode, resultCode, data);
                break;
            case REQUEST_WELCOME_ACTIVITY:
                Logger.i("Agent", "recognized requestcode as request welcome");
                if (resultCode == RESULT_OK) {
                /* OK should only be returned if we should install.  We'll check anyway */
                    boolean install = data.getBooleanExtra(OnboardingActivity.EXTRA_INSTALL, false);
                    if (install) {
                        String[] config = data.getStringArrayExtra(OnboardingActivity.EXTRA_CONFIG_DATA);
                        // Split on separator.  Will be in the form of GUID, pref name, value
                        new InstallWelcomeTask(config).execute();
                        Usage.logEvent(this, Usage.Events.WELCOME_INSTALL, true);
                        return;
                    }

                    // If we don't catch above then try restore
                    new InstallWelcomeTask(null).execute();
                    Usage.logEvent(this, Usage.Events.WELCOME_INSTALL, true);

                } else {
                    // If we get a cancelled result then try to restore defaults and proceed
                    new InstallWelcomeTask(null).execute();
                    Usage.logEvent(this, Usage.Events.WELCOME_INSTALL, true);
                    Usage.logEvent(this, Usage.Events.WELCOME_SKIPPED, true);

                }

                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

//    private void checkPurchaseAndTrial() {
//        View trial_message = (LinearLayout) findViewById(R.id.trial_footer);
//        if (IabClient.checkLocalUnlock(this) || IabClient.grantUnlock(this)) {
//            trial_message.setVisibility(View.GONE);
//            IabClient.cancelTrialCancel(this);
//        } else if (IabClient.isUserOnTrial(this)) {
//            long expiration = IabClient.getExpiration(this);
//            Calendar expiry = Calendar.getInstance();
//            expiry.setTimeInMillis(expiration);
//            setupUpgrade(getString(R.string.trial_expires, new SimpleDateFormat("MM/dd/yyyy").format(expiry.getTime())));
//        } else {
//            // Query IAB Client here and get purchase results.  Act accordingly
//            Runnable r;
//            if (IabClient.isTrialAvailable(this)) {
//                r = new Runnable() {
//                    @Override
//                    public void run() {
//                        PrefsHelper.setPrefBool(MainActivity.this, "has_shown_warning", true);
//                        showTrialMessageAndStartTrial();
//                    }
//                };
//            } else {
//                r = new Runnable() {
//                    @Override
//                    public void run() {
//                        setupUpgrade(getString(R.string.free_trial_has_expired));
//                        List<Agent> installed = AgentFactory.getInstalledAgents(MainActivity.this);
//                        if (installed.size() > 0) {
//                            startService(new Intent(MainActivity.this, ExpireTrialIntentService.class));
//                        }
//                    }
//                };
//            }
//            initIabClientForPurchaseCheck(r, trial_message);
//        }
//    }

    private void checkPurchaseAndTrial() {
        View trial_message = (LinearLayout) findViewById(R.id.trial_footer);
        if (IabClient.grantUnlock(this)) {
            trial_message.setVisibility(View.GONE);
            IabClient.cancelTrialCancel(this);
        }
//        else if (IabClient.isUserOnTrial(this)) {
//            long expiration = IabClient.getExpiration(this);
//            Calendar expiry = Calendar.getInstance();
//            expiry.setTimeInMillis(expiration);
//            setupUpgrade(getString(R.string.trial_expires, new SimpleDateFormat("MM/dd/yyyy").format(expiry.getTime())));
//        }
        else {
            // Query IAB Client here and get purchase results.  Act accordingly
            Runnable r;
//            if (IabClient.isTrialAvailable(this)) {
//                r = new Runnable() {
//                    @Override
//                    public void run() {
//                        PrefsHelper.setPrefBool(MainActivity.this, "has_shown_warning", true);
//                        showTrialMessageAndStartTrial();
//                    }
//                };
//            } else {
            r = new Runnable() {
                @Override
                public void run() {
                    setupUpgrade(getString(R.string.click_to_purchase));
                    List<Agent> installed = AgentFactory.getInstalledAgents(MainActivity.this);
                    if (installed.size() > 0) {
                        startService(new Intent(MainActivity.this, ExpireTrialIntentService.class));
                    }
                }
            };
//            }
            initIabClientForPurchaseCheck(r, trial_message);
        }
    }

    private void initIabClientForPurchaseCheck(final Runnable errorRunnable, final View v) {
        mIabClient = new IabClient(this, new IabClient.IabCallback() {
            @Override
            public void handleIabError(IabResult result) {
                if (errorRunnable != null) {
                    errorRunnable.run();
                }
            }

            @Override
            public void handleIabSuccess(IabResult result) {
                if (mIabClient.wasUnlockPurchased(MainActivity.this)) {
                    IabClient.setLocalUnlock(MainActivity.this, true);
                    IabClient.cancelTrialCancel(MainActivity.this);
                    v.setVisibility(View.GONE);
                    return;
                } else {
                    if (errorRunnable != null) {
                        errorRunnable.run();
                    }
                }
            }

            @Override
            public void onIabPurchaseFinished(IabResult result, Purchase info) {
                if (errorRunnable != null) {
                    errorRunnable.run();
                }
            }
        });
        mIabClient.startSetup();
    }

    private void showTrialMessageAndStartTrial() {
        String message = Utils.shouldGrandfather(this) ?
                getString(R.string.grandfather_message)
                : getString(R.string.upgrade_message);

        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();

        if (Utils.shouldGrandfather(this)) {
            IabClient.setLocalUnlock(this, true);
        } else {

            IabClient.startTrial(this, "");
        }
    }

    private void setupUpgrade(String message) {
        final View trial_message = findViewById(R.id.trial_footer);
        trial_message.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.trial_message)).setText(message);
        Logger.d("Setting up IAB click handler");
        findViewById(R.id.upgrade).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.d("IAB click set up");
                mIabClient = new IabClient(MainActivity.this, new IabClient.IabCallback() {
                    @Override
                    public void handleIabError(IabResult result) {
                        Logger.d("IAB: Error in setup");
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage("There was a problem contacting Google Play.  Please try again later.\nError: " + result.getMessage())
                                .show();
                    }

                    @Override
                    public void handleIabSuccess(IabResult result) {
                        Logger.d("IAB: Set up OK");
                        if (mIabClient.wasUnlockPurchased(MainActivity.this)) {
                            Logger.d("IAB: Unlock was purchased!");
                            trial_message.setVisibility(View.GONE);
                            new InstallWelcomeTask(null).execute();
                        } else {
                            Logger.d("IAB: Starting purchase");
                            mIabClient.startPurchase(MainActivity.this, 1, Constants.SKU_UNLOCK);
                        }
                    }

                    @Override
                    public void onIabPurchaseFinished(IabResult result, Purchase info) {
                        Logger.d("IAB: Purchase finished!");
                        if (!mIabClient.checkUnlock(MainActivity.this)) {
                            if (mIabClient.wasUnlockPurchased(MainActivity.this)) {
                                trial_message.setVisibility(View.GONE);
                                new InstallWelcomeTask(null).execute();
                            }
                        }
                    }
                });
                mIabClient.startSetup();
            }
        });
    }

    private DrawerListElement[] mDrawer;

    private void createDrawerList() {

        ArrayList<DrawerListElement> drawer = new ArrayList<DrawerListElement>();
        drawer.add(new DrawerListElement(DrawerListElementType.ELEMENT, R.drawable.ic_agent_inverse, getString(R.string.agents), new AgentsFragment()));

        drawer.add(new DrawerListElement(DrawerListElementType.ELEMENT, R.drawable.ic_activity, getString(R.string.agents_feed), new FeedFragment()));
        drawer.add(new DrawerListElement(DrawerListElementType.ELEMENT, R.drawable.ic_settings_white, getString(R.string.title_settings), new AppPreferencesFragment()));
        drawer.add(new DrawerListElement(DrawerListElementType.ELEMENT, R.drawable.ic_widget, getString(R.string.widgets), new AgentWidgetFragment()));

        mFeedbackLocation = drawer.size();
//        drawer.add(new DrawerListElement(DrawerListElementType.ELEMENT, R.drawable.ic_feedback, getString(R.string.title_feedback), new FeedbackFragment()));
        drawer.add(new DrawerListElement(DrawerListElementType.ELEMENT, R.drawable.ic_help, getString(R.string.help), new AgentHelpFragment()));

        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String carrierName = manager.getNetworkOperatorName();
        String support_queries_link = AppURLs.SUPPORT_QUERIES_LINK+"&ver="+BuildConfig.VERSION_NAME+carrierName;
        drawer.add(new DrawerListElement(DrawerListElementType.LINK, R.drawable.ic_help, getString(R.string.title_support_inquiries), support_queries_link));

        drawer.add(new DrawerListElement(DrawerListElementType.LINK, R.drawable.ic_help, getString(R.string.title_faq), AppURLs.FAQ_LINK));

        mDrawer = new DrawerListElement[drawer.size()];
        drawer.toArray(mDrawer);
    }

    private enum DrawerListElementType {TITLE, HEADER, ELEMENT, ACTIVE, LINK}

    private class DrawerListElement {
        private DrawerListElementType mType;
        private String mTitle;
        private Fragment mFragment;
        private String mTag;
        private int mIcon;
        private String mLink;

        public DrawerListElement(DrawerListElementType type, int icon, String title, Fragment fragment) {
            mType = type;
            mTitle = title;
            mFragment = fragment;
            mTag = fragment.getClass().toString();
            mIcon = icon;
        }

        public DrawerListElement(DrawerListElementType type, int icon, String title, String link) {
            mType = type;
            mTitle = title;
            mIcon = icon;
            mLink = link;
        }

        public DrawerListElementType getType() {
            return mType;
        }

        public String getTitle() {
            return mTitle;
        }

        public Fragment getFragment() {
            return mFragment;
        }

        public String getTag() {
            return mTag;
        }

        public String getLink() {
            return mLink;
        }
    }

    private class DrawerListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mDrawer.length;
        }

        @Override
        public Object getItem(int position) {
            return mDrawer[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v;

            if (mDrawer[position].getType() == DrawerListElementType.TITLE) {
                v = MainActivity.this.getLayoutInflater().inflate(R.layout.drawer_title_item, null);
            } else if (mDrawer[position].getType() == DrawerListElementType.HEADER) {
                v = MainActivity.this.getLayoutInflater().inflate(R.layout.drawer_header_item, null);
            } else {
                v = MainActivity.this.getLayoutInflater().inflate(R.layout.drawer_list_item, null);

                ImageView iv = (ImageView) v.findViewById(R.id.image);
                iv.setImageResource(mDrawer[position].mIcon);
            }

            TextView tv = (TextView) v.findViewById(R.id.text);
            tv.setText(mDrawer[position].getTitle());


            return v;
        }

    }

    private class DrawerItemClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int position,
                                long id) {
            selectItem(position);
        }
    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {

        final DrawerListElement selectedDrawerListElement = mDrawer[position];

        if(selectedDrawerListElement.getType() != DrawerListElementType.LINK) {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(MainActivity.this);

            Editor editor = prefs.edit();

            //setting the last position of the drawer item....
            editor.putInt(LAST_DRAWER_LOCATION, position);
            editor.commit();

            //getting the current position of the drawer...
            mCurrentPosition = position;

            //string tag stores the name of the current drawer item in it....
            String tag = selectedDrawerListElement.getTag();

            FragmentManager manager = getSupportFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();

            //if the current mCurrentTag is not empty than
            //initialize the mCurrentTag to the current tag
            // and hide the current tag so that the new fragment can be seen

            if (mCurrentTag != null) {
                Fragment current = manager.findFragmentByTag(mCurrentTag);
                if (current != null) {
                    transaction.hide(current);
                }
            }

            // the selected fragment is than inflated on the screen...
            Fragment f = manager.findFragmentByTag(tag);
            if (f == null) {
                Logger.i("Getting new fragment for " + tag);
                f = selectedDrawerListElement.getFragment();
                transaction.add(R.id.content_frame, f, tag);
            } else {
                transaction.show(f);
            }
            mCurrentTag = tag;

            // Insert the fragment by replacing any existing fragment
            try {
                transaction.commitAllowingStateLoss();
            } catch (Exception e) {
                Logger.e("Exception selecting fragment", e);
            }

            // Highlight the selected item, update the title, and close the drawer
            mDrawerLeftList.setItemChecked(position, true);
        } else {
            mDrawerLeftList.setItemChecked(mCurrentPosition,true);
            AlertDialogUtility.showDialog(this, getString(R.string.external_link_dialog_title), getString(R.string.external_link_dialog_message), getString(R.string.label_yes), getString(R.string.label_no), new AlertDialogUtility.AlertDialogClickListner() {
                @Override
                public void onOkClick() {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(selectedDrawerListElement.getLink()));
                    startActivity(browserIntent);
                }

                @Override
                public void onCancelClick() {

                }
            });
        }

        mDrawerLayout.closeDrawer(mDrawerLeftList);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content
        // view
        // boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        return super.onPrepareOptionsMenu(menu);
    }

    public void openDrawer() {
        mDrawerLayout.openDrawer(mDrawerLeftList);
    }


    public void ideaClicked(View v) {
        selectItem(mFeedbackLocation);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward permission request result to the fragments
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    protected class InstallWelcomeTask extends AsyncTask<Void, Void, Void> {
        protected ProgressDialog mDialog;
        protected String[] prefs;
        protected boolean restore;

        public InstallWelcomeTask(String[] prefs) {
            this.prefs = prefs;
            this.restore = (prefs == null);
        }

        @Override
        protected void onPreExecute() {
            mDialog = ProgressDialog.show(MainActivity.this, "", getResources().getString(R.string.agent_setup_enable), true);
            mDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.agent_animation_agent));
            mTaskCollection.addTask(this);
        }

        @Override
        protected Void doInBackground(Void... arg) {
            try {
                if (restore) {
                    AgentPrefDumper.restoreAgentPrefs(MainActivity.this);
                }

                PrefsHelper.setPrefLong(MainActivity.this, WELCOME_AGENTS_INSTALLED_AT, System.currentTimeMillis());

                List<Agent> agents = AgentFactory.getAllAgents(MainActivity.this);
                HashMap<String, Agent> agentsHash = new HashMap<String, Agent>(agents.size());

                for (Agent agent : agents) {
                    agentsHash.put(agent.getGuid(), agent);

                    if(!Utils.isMarshmallowOrUp() || !agent.getGuid().equals(BatteryAgent.HARDCODED_GUID)) {
                        agent.install(MainActivity.this, false, true);
                        NotificationUtils.SetAgentNotificationSeen(agent.getGuid(), MainActivity.this, false);
                    }
                }


                boolean needUpdateMeetingAlarms = false;

                if (!restore && (prefs != null)) {
                    for (String pref : prefs) {
                        String[] values = pref.split(AgentPreferences.STRING_SPLIT);
                        if (values.length >= 3) {
                            Agent a = agentsHash.get(values[0]);
                            String prefName = values[1];
                            String prefValue = values[2];
                            if (values.length > 3) {
                                for (int i = 3; i < values.length; i++) {
                                    prefValue += AgentPreferences.STRING_SPLIT + values[i];
                                }
                            }

                            String curPrefVal = a.getPreferencesMap().get(prefName);
                            if (MeetingAgent.HARDCODED_GUID.equals(a.getGuid())) {
                                if (prefName.startsWith(AgentPreferences.MEETING_ACCOUNTS) && ((curPrefVal == null) || (curPrefVal.isEmpty()))) {
                                    curPrefVal = "false";
                                }
                            }

                            if (((curPrefVal == null) && (prefValue != null)) || (!curPrefVal.equals(prefValue))) {
                                Logger.d(String.format("Saving: %s = %s for %s", prefName, prefValue, values[0]));
                                if (MeetingAgent.HARDCODED_GUID.equals(a.getGuid()) && prefName.startsWith(AgentPreferences.MEETING_ACCOUNTS)) {
                                    ((MeetingAgent) a).updatePreferenceNoUpdateAlarms(prefName, prefValue);
                                    needUpdateMeetingAlarms = true;
                                } else {
                                    a.updatePreference(prefName, prefValue);
                                }
                            } else {
                                Logger.d(String.format("Same; not saving: %s = %s for %s", prefName, prefValue, values[0]));
                            }
                        }
                    }
                }

                Utils.checkReceivers(MainActivity.this);
                if (needUpdateMeetingAlarms) {
                    Logger.d("MainActivity: update meeting alarms after onboarding");
                    ((MeetingAgent) AgentFactory.getAgentFromGuid(MainActivity.this, MeetingAgent.HARDCODED_GUID)).updateScheduledAlarms(MainActivity.this);
                } else {
                    Logger.d("MainActivity: no need to update meeting after onboarding");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                mDialog.dismiss();
                mTaskCollection.completeTask(this);
            }
        }
    }

    protected class SanityCheckerTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            mTaskCollection.addTask(this);
        }

        @Override
        protected Void doInBackground(Void... nothing) {
            try {
                Logger.d("Running SanityCheckerTask");
                Utils.checkReceivers(MainActivity.this);
                ActivityRecognitionHelper.startActivityRecognitionIfNeeded(MainActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
                Logger.d("Error in SanityCheckerTask: " + e.toString());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                mTaskCollection.completeTask(this);
            }
        }

    }


}