package com.mobiroo.n.sourcenextcorporation.agent.billing;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;

import com.android.vending.billing.IInAppBillingService;
import com.mobiroo.n.sourcenextcorporation.agent.service.ExpireTrialIntentService;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.util.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class IabClient {

    public static final String EXTRA_SKU = "sku";
    public static final String EXTRA_ITEM = "item";
    public static final String PREF_NAME   = "IAP_PREFS";
    
    private static final boolean UNLOCK_ALLOW = true;
    
    private static final int TRIAL_UNIT    = Calendar.DAY_OF_YEAR;
    private static final int TRIAL_VALUE   = 0;
    
    private ArrayList<String> mSkus;
    private IabHelper mHelper;

    private IabCallback mCallback;
    
    private int mItem;
    private String mSku;

    IInAppBillingService mService;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
            mIabSetupFinishedListener.onIabSetupFinished(null);
        }
    };

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_NAME, 0);
    }

    private Context mContext;

    private void logd(String message) {
        Logger.d("IAB: " + message);
    }
    
    @SuppressWarnings("unused")
    private void logi(String message) {
        Logger.i("IAB: " + message);
    }
    
    private void loge(String message, Exception e) {
        Logger.e("IAB: " + message, e);
    }
    
    @Deprecated
    public IabClient(Context context) {
        mContext = context;
        mHelper = new IabHelper(context, Constants.getpk());
        mCallback = null;
    }

    public IabClient(Context context, IabCallback callback) {
        mContext = context.getApplicationContext();
        mHelper = new IabHelper(context, Constants.getpk());
        mCallback = callback;
    }
    
    public void setCallback(IabCallback callback) {
        mCallback = callback;
    }
    
    public IabHelper getHelper() {
        return mHelper;
    }

    private void buildSkus() {
        mSkus = new ArrayList<String>();
        mSkus.add(Constants.SKU_UNLOCK);
    }

    public Bundle getSkuList() {
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList("ITEM_ID_LIST", getSkuArrayList());
        return querySkus;
    }

    public ArrayList<String> getSkuArrayList() {
        if (mSkus == null) {
            buildSkus();
        }

        return mSkus;
    }

    public static boolean grantUnlock(Context context) {
        return true;

//        PackageManager packageManager = context.getPackageManager();
//        PackageInfo info = null;
//        try { info = packageManager.getPackageInfo("com.tagstand.betaunlock", PackageManager.GET_META_DATA);}
//        catch (NameNotFoundException e) {}
//
//        if ((info != null)) {
//            Logger.d("IAB: GrantUnlock: True");
//            return true;
//        } else {
//            Logger.d("IAB: GrantUnlock: False");
//            return false;
//        }

    }
    
    public static final boolean checkLocalUnlockOrTrial(Context context) {
        return checkLocalUnlock(context) || isUserOnTrial(context) || grantUnlock(context);
    }
    
    public static boolean checkLocalUnlock(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        boolean local = settings.getBoolean(Constants.SKU_UNLOCK, false);
        Logger.d("IAB: CheckLocalUnlock: " + local);
//        ToDo remove this before generating a build.
        return local;
    }

    public static void setLocalUnlock(Context context, boolean value) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        settings.edit().putBoolean(Constants.SKU_UNLOCK, value).commit();
    }

    public void dispose() {
        if (mService != null) {
            mContext.unbindService(mServiceConn);
        }
    }

    public boolean wasUnlockPurchased(Context context) {
        ArrayList<String> ownedSkus = new ArrayList<>();
        try {
            Bundle ownedItems = mService.getPurchases(3, mContext.getPackageName(), "inapp", null);
            int response = ownedItems.getInt("RESPONSE_CODE");
            if (response == 0) {
                ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                logd("Owned SKUs is " + TextUtils.join(",", ownedSkus));
                logd("WasUnlockPurchased 1 " + (ownedSkus.contains(Constants.SKU_UNLOCK)));
                return (ownedSkus.contains(Constants.SKU_UNLOCK));
            }

            //inventory = mHelper.queryInventory(false, getSkuList());
        } catch (Exception e) {
            loge("Exception building inventory", e);
            return false;
        }
        logd("Owned SKUs is " + TextUtils.join(",", ownedSkus));
        return UNLOCK_ALLOW && (ownedSkus.contains(Constants.SKU_UNLOCK));
    }
    
    public boolean checkUnlock(Context context) {
        return checkLocalUnlock(context) || wasUnlockPurchased(context);
    }
    
    public void storePurchase(Context context, String sku, boolean purchased) {
        logd("Storing " + sku + ", " + purchased);

        Editor editor = getPreferences(context).edit();
        editor.putBoolean(sku, purchased);
        editor.commit();
    }
    
    private final static String USER_IS_ON_TRIAL = "trial_started";
    private final static String TRIAL_EXPIRATION = "trial_expiration";
    private final static String USER_TRIAL_CLAIMED = "trial_claimed";
    
    public static boolean isUserOnTrial(Context context) {
        SharedPreferences prefs = getPreferences(context);
        
        long stored_end = prefs.getLong(TRIAL_EXPIRATION, 1000);
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(stored_end);

        Logger.d("IAB: User is on trial? " + (getPreferences(context).getBoolean(USER_IS_ON_TRIAL, false)
                && Calendar.getInstance().before(end)));

        return (getPreferences(context).getBoolean(USER_IS_ON_TRIAL, false)
                && Calendar.getInstance().before(end));
    }
    
    public static void startTrial(Context context, String account) {
        Editor editor = getPreferences(context).edit();
        
        editor.putBoolean(USER_IS_ON_TRIAL, true);
        editor.putBoolean(USER_TRIAL_CLAIMED, true);
        
        Calendar end = Calendar.getInstance();
        end.add(TRIAL_UNIT, TRIAL_VALUE);
        Logger.d("Trial expires " + new SimpleDateFormat("MM-dd-yy hh:mm").format(end.getTime()));
        editor.putLong(TRIAL_EXPIRATION, end.getTimeInMillis());
        editor.commit();

        scheduleTrialCancel(context);
    }

    public static void scheduleTrialCancel(Context context) {
        long expiration = getExpiration(context);
        if (expiration > 0) {
            AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            manager.set(AlarmManager.RTC_WAKEUP, expiration, getExpirationPendingIntent(context));
        }
    }

    public static void cancelTrialCancel(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.cancel(getExpirationPendingIntent(context));
    }

    private static PendingIntent getExpirationPendingIntent(Context context) {
        return PendingIntent.getService(context, 109, ExpireTrialIntentService.getStartIntent(context, true), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static long getExpiration(Context context) {
        return getPreferences(context).getLong(TRIAL_EXPIRATION, 0);
    }

    public static void endTrial(Context context) {
        Editor editor = getPreferences(context).edit();
        editor.putBoolean(USER_IS_ON_TRIAL, false);
    }

    public static boolean isTrialAvailable(Context context) {
        Logger.d("IAB: Is trial available? " + !(getPreferences(context).getBoolean(USER_TRIAL_CLAIMED, false)));
        return !(getPreferences(context).getBoolean(USER_TRIAL_CLAIMED, false));
    }
    
    public void startSetup() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        mContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }
    
    public void startSetup(IabHelper.OnIabSetupFinishedListener listener) {
        getHelper().startSetup(listener);
    }
    
    private IabHelper.OnIabSetupFinishedListener mIabSetupFinishedListener = new IabHelper.OnIabSetupFinishedListener() {
        public void onIabSetupFinished(IabResult result) {
            mCallback.handleIabSuccess(result);
        }
    };
    
    
    public void startPurchase(Activity activity, int item, String sku) {
        mItem = item;
        mSku = sku;
        try {
            Bundle buyIntentBundle = mService.getBuyIntent(3, mContext.getPackageName(), sku, "inapp", null);
            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            activity.startIntentSenderForResult(pendingIntent.getIntentSender(),
                    1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                    Integer.valueOf(0));
        } catch (Exception e) {
            loge("Exception starting purchase: " + e, e);
        }
        //getHelper().launchPurchaseFlow(activity, sku, item, mOnIabFinishListener);
    }
    
    
    private IabHelper.OnIabPurchaseFinishedListener mOnIabFinishListener = new IabHelper.OnIabPurchaseFinishedListener() {

        @Override
        public void onIabPurchaseFinished(IabResult result, Purchase info) {
            if (result.isSuccess()) {
                Logger.d("IAB purchase finished successfully for " + mItem + " " + mSku);
            } else if (result.isFailure()) {
                Logger.d("IAB purchase failed [" + result.getResponse() + "]");
            }
            
            mCallback.onIabPurchaseFinished(result, info);
        }
    };
    
    public static interface IabCallback {
        public void handleIabError(IabResult result);
        public void handleIabSuccess(IabResult result);
        public void onIabPurchaseFinished(IabResult result, Purchase info);
    }
}
