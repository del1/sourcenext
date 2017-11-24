package com.mobiroo.n.sourcenextcorporation.agent.util;

import android.Manifest;
import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Instances;

import com.mobiroo.n.sourcenextcorporation.agent.item.MeetingAgent;
import com.mobiroo.n.sourcenextcorporation.tagstand.util.Logger;
import com.mobiroo.n.sourcenextcorporation.agent.item.AgentFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class UserCalendarUtils {

    private static SimpleDateFormat mDateFormat = new SimpleDateFormat("MM-dd-yyyy kk:mm");

    public static class EventInfo {
        private long    mStart;
        private long    mEnd;
        private String  mTitle;
        private int     mAvailability;
        private int     mStartId;
        private int     mEndId;
        private int     mAllDay;

        public EventInfo(String title, long start, long end, int availability, int startId,
                         int endId, int allDay) {
            mTitle = title;
            mStart = start;
            mEnd = end;
            mAvailability = availability;
            mStartId = startId;
            mEndId = endId;
            mAllDay = allDay;
        }



        public void setTitle(String title) {
            mTitle = title;
        }

        public String getTitle() {
            return mTitle;
        }

        public long getEnd() {
            return mEnd;
        }

        public long getStart() {
            return mStart;
        }

        public int getAvailability() {
            return mAvailability;
        }

        public int getStartId() {
            return mStartId;
        }

        public int getEndId() {
            return mEndId;
        }

        public void setStartId(int id) {
            mStartId = id;
        }

        public void setEndId(int id) {
            mEndId = id;
        }

        public void setEndTime(long time) {
            mEnd = time;
        }

        public void setStartTime(long time) {
            mStart = time;
        }

        public int getAllDay() {
            return mAllDay;
        }

        public void setAllDay(int allDay) {
            this.mAllDay = allDay;
        }

    }

    public static final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy kk:mm");



    public static MeetingAgent.SubCalendar[] getCalendarsForAccount(Context context, Account account) {
        if(Utils.isPermissionGranted(context, Manifest.permission.READ_CALENDAR)) {
            Cursor c = getCalendarsCursorForAccount(context, account);

            if (c == null) {
                return new MeetingAgent.SubCalendar[0];
            }

            ArrayList<MeetingAgent.SubCalendar> cals = new ArrayList<MeetingAgent.SubCalendar>();
            if (c.moveToFirst()) {
                do {
                    cals.add(new MeetingAgent.SubCalendar(c.getString(0), c.getString(2), c.getInt(4)));
                } while (c.moveToNext());
            }

            c.close();

            return cals.toArray(new MeetingAgent.SubCalendar[cals.size()]);
        }

        return new MeetingAgent.SubCalendar[0];
    }

    private static Cursor getCalendarsCursorForAccount(Context context, Account account) {
        Uri uri = Calendars.CONTENT_URI;

        final String[] CALENDAR_PROJECTION = new String[] {
                Calendars._ID,                           // 0
                Calendars.ACCOUNT_NAME,                  // 1
                Calendars.CALENDAR_DISPLAY_NAME,         // 2
                Calendars.OWNER_ACCOUNT,                 // 3
                Calendars.CALENDAR_COLOR
        };

        String selection = "((" + Calendars.ACCOUNT_NAME + " = ?) AND ("
                + Calendars.ACCOUNT_TYPE + " = ?))";

        String[] selectionArgs = new String[] {account.name, account.type};

        return context.getContentResolver().query(uri, CALENDAR_PROJECTION, selection, selectionArgs, null);
    }

    private static Cursor getCursorForTimes(Context context, Account account, Uri uri, long startMillis, long endMillis,
                                            boolean busyOnly, boolean ignoreAllDay, String[] projection) {

        Cursor c = null;

        MeetingAgent.SubCalendar[] calendars = getCalendarsForAccount(context, account);
        if (calendars.length == 0) {
            Logger.d("Trying to get cursor for account with 0 calendars:" + account.name + "=" + account.type);
            return null;
        }

        // Build a list of calendar IDs that we care about
        ArrayList<String> ids = new ArrayList<String>();
        MeetingAgent agent = (MeetingAgent) AgentFactory.getAgentFromGuid(context, MeetingAgent.HARDCODED_GUID);
        for (MeetingAgent.SubCalendar calendar: calendars) {
            boolean isMainCalendar = account.name.equals(calendar.name);

            if (Utils.getBoolPref(agent.getPreferencesMap(), AgentPreferences.MEETING_ACCOUNTS + account.name + "_" + calendar.id, isMainCalendar)) {
                ids.add(calendar.id);
            }
        }

        if (ids.size() == 0) {
            return null;
        }
        
        /* 
         * CALENDAR_ID The _ID of the calendar the event belongs to.
         */

        String selection = "";

        if (startMillis != TIME_DONT_USE) {
            selection += "((" + Instances.BEGIN + " > " + startMillis + ")";
        } else {
            selection += "((" + Instances.BEGIN + " > " + System.currentTimeMillis() + ")";
        }

        if (endMillis != TIME_DONT_USE) {
            selection += "AND (" + Instances.BEGIN + " < " + endMillis + ")";
        }

        if (busyOnly) {
            selection += " AND (" + Instances.AVAILABILITY + "=" + Instances.AVAILABILITY_BUSY + ")";
        }

        if (ignoreAllDay) {
            selection += " AND (" + Instances.ALL_DAY + "=0" + ")";
        }

        selection += " AND ((" + Instances.STATUS + " IS NULL) OR (" + Instances.STATUS + "<>" + Instances.STATUS_CANCELED + "))";

        selection += " AND (";
        for (int i=0; i < ids.size(); i++) {
            if (i != 0) {
                selection += " OR ";
            }
            selection += Instances.CALENDAR_ID + "=" + ids.get(i);
        }
        selection += ") ";

        selection += " )";

        Logger.d("Selection is " + selection);
        String[] selectionArgs = new String[] {};

        try {
            c = context.getContentResolver().query(uri, projection, selection, selectionArgs, Instances.BEGIN + " ASC");
        } catch (IllegalArgumentException e) {
            Logger.e("Excpetion building calendar events: " + e, e);
            // Caused by: java.lang.IllegalArgumentException: allDay is true but sec, min, hour are not 0.
            // Occurs on some devices that improperly insert calendar data.  This exception is thrown by the content provider itself
            // due to inconsistent data
        }


        return c;
    }

    private static final String[] INSTANCES_PROJECTION = new String[] {
            Instances.TITLE,        // 0
            Instances.BEGIN,        // 1
            Instances.END,          // 2
            Instances.AVAILABILITY, // 3
            Instances._ID,          // 4
            Instances.ALL_DAY,      // 5
            Instances.STATUS,       // 6
            Instances.ORGANIZER,    // 7
            Instances.OWNER_ACCOUNT, // 8
            Instances.SELF_ATTENDEE_STATUS, // 9
            Instances.EVENT_ID,       // 10
            Instances.CALENDAR_DISPLAY_NAME   // 11
    };

    private static final long TIME_DONT_USE = -1;


    private static final String[] ATTENDEES_PROJECTION = new String[] {
            Attendees.EVENT_ID,             // 0
            Attendees.ATTENDEE_NAME,        // 1
            Attendees.ATTENDEE_EMAIL,       // 2
            Attendees.ATTENDEE_STATUS       // 3
    };

    private static int getAttendeeStatusForEvent(Context context, long eventId, String email) {
        int status = Attendees.ATTENDEE_STATUS_NONE;
        Cursor c = Attendees.query(context.getContentResolver(), eventId, ATTENDEES_PROJECTION);
        if (c.moveToFirst()) {
            Logger.d("getAttendeeStatusForEvent: Checking attendee list");
            do {
                //Logger.d(String.format("getAttendeeStatusForEvent Attendee: %s %s %d", c.getString(1), c.getString(2), c.getInt(3)));
                if (email.equals(c.getString(2))){
                    status = c.getInt(3);
                    break;
                }
            } while (c.moveToNext());
        } else {
            Logger.d("getAttendeeStatusForEvent: no attendee data");
        }
        c.close();
        return status;
    }


    private static boolean isAttendingEvent(Context context, String email, String organizerEmail,
                                            String calendarOwnerEmail, int selfAttendeeStatus, long eventId, boolean acceptedOnly) {

        if (email == null) {Logger.d("isAttendingEvent: null email"); return false;}

        if (email.equals(organizerEmail)) {
            Logger.d("isAttendingEvent: organizer");
            return true;
        }

        int attendeeStatus;

        if (email.equals(calendarOwnerEmail)) {
            Logger.d("isAttendingEvent: calendar owner");
            attendeeStatus = selfAttendeeStatus;
        } else {
            attendeeStatus = getAttendeeStatusForEvent(context, eventId, email);
        }

        Logger.d("isAttendingEvent: attendeeStatus = " + attendeeStatus);
        if (acceptedOnly) {
            return ((attendeeStatus == Attendees.ATTENDEE_STATUS_ACCEPTED)
                    || (attendeeStatus == Attendees.ATTENDEE_STATUS_TENTATIVE));
        }

        return (attendeeStatus != Attendees.ATTENDEE_STATUS_DECLINED);
    }

    public interface EventTimeChecker {
        public boolean isAllowedStartTime(long eventStartMillis);
    }


    /**
     * @param context
     * @param account
     * @param busyOnly
     * @return Returns the next matching event from the instances table for this account
     */
    @SuppressLint("DefaultLocale")
    public static EventInfo getNextCompositeEventForUser(Context context, Account account,
                                                         boolean busyOnly, boolean acceptedOnly, boolean ignoreAllDay, EventTimeChecker etc) {

        Logger.d("Checking events for " + account.name);

        // Grab all events in the next 30 days
        long startMillis = System.currentTimeMillis() - 1000;
        long endMillis = startMillis + 30 * AlarmManager.INTERVAL_DAY;

        // Set up a URI with our start and end times for the query
        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMillis);
        ContentUris.appendId(builder, endMillis);

        Cursor c = getCursorForTimes(context, account, builder.build(),
                TIME_DONT_USE, TIME_DONT_USE,
                busyOnly, ignoreAllDay, INSTANCES_PROJECTION);
        if (c == null) { return null;}
        if (!c.moveToFirst()) {c.close(); return null;}

        // Try to grab first event
        EventInfo event = null;
        do {
            String start = "";
            String end = "";
            try { start = mDateFormat.format(c.getLong(1)); }
            catch (Exception e) { start = String.valueOf(c.getLong(1)); }

            try { end = mDateFormat.format(c.getLong(2)); }
            catch (Exception e) { end = String.valueOf(c.getLong(2)); }

            String debugStr = String.format("Account=%s, Calendar=%s, Event=%s, start=%s, end=%s, " +
                    "avail=%d, id=%d, all_day=%d, " +
                    "status=%d, organizer=%s, owner=%s, " +
                    "sas=%d, event_id=%d",
                    account.name, c.getString(11), c.getString(0), start, end,
                    c.getInt(3), c.getInt(4), c.getInt(5),
                    c.getInt(6), c.getString(7), c.getString(8),
                    c.getInt(9), c.getLong(10));
            Logger.d(debugStr);

            long eventStartMillis = c.getLong(1);

            if (eventStartMillis < startMillis) {
                Logger.d("Event is in the past.");
                continue;
            }

            if (!etc.isAllowedStartTime(eventStartMillis)) {
                continue;
            }

            if (!isAttendingEvent(context, account.name, c.getString(7), c.getString(8), c.getInt(9), c.getLong(10), acceptedOnly)) {
                Logger.d("Not attending event.");
                continue;
            }

            Logger.d("Setting first event start time to " + eventStartMillis);
            event = new EventInfo(c.getString(0), c.getLong(1), c.getLong(2), c.getInt(3), c.getInt(4), c.getInt(4), c.getInt(5));
            break;
        } while (c.moveToNext());
        c.close();

        if (event == null) { return null;}


        // We have 1+ events.  Query to see if we have any events that start during the
        // duration of this event
        builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, event.getStart());
        ContentUris.appendId(builder, event.getEnd());

        c = getCursorForTimes(context, account, builder.build(), event.getStart(), event.getEnd(),
                busyOnly, ignoreAllDay, INSTANCES_PROJECTION);
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    Logger.d(account.name + " Overlapping event:" + c.getString(0) + ",  at " + c.getLong(1) + " to " + c.getLong(2));

                    if (!etc.isAllowedStartTime(c.getLong(1))) {
                        Logger.d("Overlapping event not within allowed timeframe.");
                        continue;
                    }

                    if (c.getLong(2) <= event.getEnd()) {
                        Logger.d("Overlapping event ends before current end time.");
                        continue;
                    }

                    if (!isAttendingEvent(context, account.name, c.getString(7), c.getString(8), c.getInt(9), c.getLong(10), acceptedOnly)) {
                        Logger.d("Not attending overlapping event.");
                        continue;
                    }

                    Logger.d("Setting end time to that of overlapping event");
                    event.setEndTime(c.getLong(2));
                    event.setEndId(c.getInt(4));
                } while (c.moveToNext());
            }
            c.close();
        }

        Logger.d("Event \"" + event.getTitle() + "\" runs from: " + sdf.format(new Date(event.getStart())) + " to " + sdf.format(new Date(event.getEnd())));
        return event;
    }
}
