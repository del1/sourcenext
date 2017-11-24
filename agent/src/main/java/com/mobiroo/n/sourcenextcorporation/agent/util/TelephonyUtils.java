package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.Manifest;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.PhoneLookup;
import android.telephony.SmsManager;
import android.text.TextUtils;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;

import java.util.ArrayList;
import java.util.Locale;


public class TelephonyUtils {

    public static class SimpleContactWrapper {
        public String displayName;
        public String phoneNumber;
        public String numberType;
        public String contactId;
        public String photoUri;

        public String firstName() {
            return (displayName.contains(" ") ? displayName.substring(0, displayName.indexOf(" ")) : displayName);
        }


        public String toString() {
            return contactId + ":" + displayName + ":" + phoneNumber;
        }

        @Override
        public boolean equals(Object v) {
            if (v instanceof SimpleContactWrapper) {
                return TextUtils.equals(((SimpleContactWrapper) v).contactId, contactId);
            }
            return false;
        }
    }

    private static ArrayList<SimpleContactWrapper> contactWrapperFromPhoneNumber(Context context, String phoneNumber) {
        ArrayList<SimpleContactWrapper> contacts = new ArrayList<>();

        //Check for contacts permission
        if (!Utils.isPermissionGranted(context, Manifest.permission.READ_CONTACTS)) {
            // User removed the permission from settings. return.
            Utils.postNotification(context, new String[] {
                    Manifest.permission.READ_CONTACTS
            });
            return contacts;
        }

        SimpleContactWrapper spn = null;
        String[] proj = {PhoneLookup._ID, PhoneLookup.DISPLAY_NAME, PhoneLookup.NUMBER, PhoneLookup.TYPE, PhoneLookup.PHOTO_URI};
        Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));

        Cursor cursor = context.getContentResolver().query(lookupUri, proj, null, null, null);

        while (cursor.moveToNext()) {
            spn = new SimpleContactWrapper();

            spn.displayName = cursor.getString(cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME));
            spn.phoneNumber = cursor.getString(cursor.getColumnIndex(PhoneLookup.NUMBER));
            spn.numberType = cursor.getString(cursor.getColumnIndex(PhoneLookup.TYPE));
            spn.contactId = cursor.getString(cursor.getColumnIndex(PhoneLookup._ID));
            spn.photoUri = cursor.getString(cursor.getColumnIndex(PhoneLookup.PHOTO_URI));
            contacts.add(spn);
        }

        cursor.close();

        return contacts;
    }

    public static ArrayList<SimpleContactWrapper> contactFromPhone(Context context, String phoneNumber) {
        ArrayList<SimpleContactWrapper> contacts = new ArrayList<>();

        for (SimpleContactWrapper contact: contactWrapperFromPhoneNumber(context, phoneNumber)) {
            if (!contacts.contains(contact)) {
                contacts.add(contact);
            }
        }

        for (SimpleContactWrapper contact: contactWrapperFromPhoneNumber(context, phoneNumber.replace("+", ""))) {
            if (!contacts.contains(contact)) {
                contacts.add(contact);
            }
        }

        for (SimpleContactWrapper contact: contactFromInternationalPhone(context, phoneNumber)) {
            if (!contacts.contains(contact)) {
                contacts.add(contact);
            }
        }

        if (contacts.size() == 0) {
            Logger.d("No contact for: " + phoneNumber);
        }
        return contacts;
    }

    public static ArrayList<SimpleContactWrapper> contactFromInternationalPhone(Context context, String rawPhoneNumber) {
        ArrayList<SimpleContactWrapper> contacts = new ArrayList<>();
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

        Locale locale = context.getResources().getConfiguration().locale;
        String country = "US";
        if ((locale.getISOCountries() != null) && (locale.getISOCountries().length > 0)) {
            country = locale.getISOCountries()[0];
        }

        String adjPhoneNumber;
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(rawPhoneNumber, country);
            //phoneNumber = Long.toString(number.getNationalNumber());
            adjPhoneNumber = phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
            Logger.d("updating " + rawPhoneNumber + " to " + adjPhoneNumber + " to search for national number formats.");

        } catch (Exception e) {
            Logger.e("error parsing international number", e);

            // Return 'contacts' instead of 'null'
            return contacts;
        }

        contacts.addAll(contactWrapperFromPhoneNumber(context, adjPhoneNumber));
        return contacts;
    }

    public static String contactNameFromPhone(Context context, String phoneNumber) {
        ArrayList<SimpleContactWrapper> spn = contactFromPhone(context, phoneNumber);
        return (spn.size() == 0 ? null : spn.get(0).displayName);
    }

    public static ArrayList<SimpleContactWrapper> contactIdsFromPhone(Context context, String phoneNumber) {
//        ArrayList<SimpleContactWrapper> spn = contactFromPhone(context, phoneNumber);
//        return (spn.size() == 0 ? null : spn);

        // Instead of 'null' just return the list returned from 'contactFromPhone'
        return contactFromPhone(context, phoneNumber);
    }


    public static ArrayList<SimpleContactWrapper> getAllPhoneNumbers(Context context) {
        ArrayList<SimpleContactWrapper> allContacts = new ArrayList<SimpleContactWrapper>();

        Cursor cursor = context.getContentResolver().query(Phone.CONTENT_URI,
                new String[]{Phone._ID, Phone.NUMBER, Phone.DISPLAY_NAME, Phone.TYPE, Phone.CONTACT_ID, Phone.PHOTO_URI}, null, null, null);

        if (!cursor.moveToFirst()) {
            Logger.d("No contacts in cursor.");
        } else {
            do {
                SimpleContactWrapper sc = new SimpleContactWrapper();
                sc.phoneNumber = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
                sc.numberType = cursor.getString(cursor.getColumnIndex(Phone.TYPE));
                sc.displayName = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));
                sc.contactId = cursor.getString(cursor.getColumnIndex(Phone.CONTACT_ID));
                sc.photoUri = cursor.getString(cursor.getColumnIndex(Phone.PHOTO_URI));
                allContacts.add(sc);
            } while (cursor.moveToNext());
        }

        cursor.close();

        return allContacts;
    }


    public static void sendSMS(String body, String to, Context context) {
        if (to != null) {
            try {
                Logger.i("sms request sent to " + to);

                SmsManager sm = SmsManager.getDefault();
                if (body == null) {
                    body = "";
                } else {
                    Logger.i("setting message to " + body);
                }

                String SENT_ACTION = "SMS_SENT_ACTION";
                String DELIVERED_ACTION = "SMS_DELIVERED_ACTION";
                PendingIntent sentIntent = PendingIntent.getBroadcast(context,
                        0, new Intent(SENT_ACTION), 0);
                PendingIntent deliveredIntent = PendingIntent.getBroadcast(
                        context, 0, new Intent(DELIVERED_ACTION), 0);

                ArrayList<String> parts = sm.divideMessage(body);

                Logger.i("splitting message into " + parts.size() + " parts.");

                ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
                ArrayList<PendingIntent> deliveredIntents = new ArrayList<PendingIntent>();

                for (int i = 0; i < parts.size(); i++) {
                    sentIntents.add(sentIntent);
                    deliveredIntents.add(deliveredIntent);
                }

                sm.sendMultipartTextMessage(to, null, parts, sentIntents,
                        null);  // Set delivered intents to null 6/24/2014 to prevent delivery reports from being requested (some users pay 20 cents per report).  -- Josh

                Logger.i("sending message");

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    // Also insert sent message
                    Logger.i("placing message in database");
                    ContentValues values = new ContentValues();
                    values.put("address", to);
                    values.put("body", body);
                    context.getContentResolver().insert(
                            Uri.parse("content://sms/sent"), values);
                }
            } catch (Exception e) {
                Logger.i("exception inserting text message [e]: "
                        + e.toString());
                Logger.i("exception inserting text message [body]: " + body);
                Logger.i("exception inserting text message [to]: " + to);
            }
        }
    }

    private static final String PREFS_NAME = "READ_SMS_PREFS";

    private static final String PREF_SENDER = "last_sender";
    private static final String PREF_SENDER_TIME = "last_sender_time";

    private static long timeout = 3 * 1000;

    public static String getLastSmsSender(Context context) {
        return context.getSharedPreferences(PREFS_NAME, 0).getString(PREF_SENDER, "");
    }

    public static long getLastSmsSenderTime(Context context) {
        return context.getSharedPreferences(PREFS_NAME, 0).getLong(PREF_SENDER_TIME, 0);
    }

    public static void storeLastSmsSender(Context context, String sender) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putString(PREF_SENDER, sender);
        editor.putLong(PREF_SENDER_TIME, System.currentTimeMillis());
        editor.commit();
    }

    public static boolean readCurrentSender(Context context, String sender) {

        String last = getLastSmsSender(context);
        if (!last.equals(sender)) {
            return true;
        }

        if ((System.currentTimeMillis() - getLastSmsSenderTime(context)) > timeout) {
            return true;
        }

        return false;


    }
}
